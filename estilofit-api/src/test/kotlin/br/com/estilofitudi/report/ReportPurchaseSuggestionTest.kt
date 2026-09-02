package br.com.estilofitudi.report

import br.com.estilofitudi.support.IntegrationTest
import br.com.estilofitudi.support.TestAuthHelper
import br.com.estilofitudi.user.domain.Role
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDate

class ReportPurchaseSuggestionTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val authHelper: TestAuthHelper,
    private val objectMapper: ObjectMapper,
) : IntegrationTest() {

    private fun managerToken() = authHelper.bearerFor(Role.MANAGER)
    private val today = LocalDate.now()

    private fun postId(url: String, token: String, body: String): String {
        val json = mockMvc.perform(
            post(url).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(body),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        return objectMapper.readTree(json)["id"].asText()
    }

    private fun setupVariant(token: String, suffix: String): String {
        val catId = postId("/categories", token, """{"name":"Cat $suffix"}""")
        val prodId = postId("/products", token, """{"name":"Prod $suffix","categoryId":"$catId"}""")
        val varJson = mockMvc.perform(
            post("/products/$prodId/variants").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("""{"size":"M","color":"Azul"}"""),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        return objectMapper.readTree(varJson)["id"].asText()
    }

    private fun stockUp(token: String, variantId: String, suffix: String, qty: Int, unitCost: Double) {
        val supplierId = postId("/suppliers", token, """{"name":"Forn $suffix"}""")
        val body = objectMapper.writeValueAsString(
            mapOf(
                "supplierId" to supplierId,
                "receivedAt" to today.toString(),
                "freightCost" to 0.00,
                "items" to listOf(mapOf("variantId" to variantId, "quantity" to qty, "unitCost" to unitCost)),
            ),
        )
        mockMvc.perform(
            post("/supply-lots").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(body),
        ).andExpect(status().isCreated)
    }

    private fun firstChannelId(token: String): String {
        val json = mockMvc.perform(get("/sale-channels").header("Authorization", token))
            .andExpect(status().isOk).andReturn().response.contentAsString
        return objectMapper.readTree(json)[0]["id"].asText()
    }

    private fun sell(token: String, channelId: String, variantId: String, qty: Int) {
        postId(
            "/sales", token,
            objectMapper.writeValueAsString(
                mapOf(
                    "channelId" to channelId,
                    "paymentMethod" to "CASH",
                    "items" to listOf(mapOf("variantId" to variantId, "quantity" to qty)),
                ),
            ),
        )
    }

    private fun suggestion(token: String, days: Int): JsonNode {
        val json = mockMvc.perform(
            get("/reports/purchase-suggestion").header("Authorization", token).param("days", days.toString()),
        ).andExpect(status().isOk).andReturn().response.contentAsString
        return objectMapper.readTree(json)
    }

    /** Achata todos os itens de todos os grupos de fornecedor. */
    private fun allItems(resp: JsonNode): List<JsonNode> =
        resp["groups"].flatMap { g -> g["items"].toList() }

    private fun itemOf(resp: JsonNode, variantId: String): JsonNode? =
        allItems(resp).firstOrNull { it["variantId"].asText() == variantId }

    private fun groupOf(resp: JsonNode, variantId: String): JsonNode? =
        resp["groups"].firstOrNull { g -> g["items"].any { it["variantId"].asText() == variantId } }

    @Test
    fun `sugere compra com base na velocidade e cobertura alvo`() {
        val token = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(token, s)
        // estoque inicial 60, vende 30 -> resta 30 em estoque
        stockUp(token, variantId, s, qty = 60, unitCost = 40.0)
        val channelId = firstChannelId(token)
        sell(token, channelId, variantId, 30)

        // days=15 -> velocidade = 30/15 = 2/dia
        val resp = suggestion(token, 15)
        assertEquals(15, resp["referenceDays"].asInt())
        val mine = itemOf(resp, variantId)
        assertNotNull(mine, "variação vendida deve aparecer na sugestão")
        // velocidade = 30/15 = 2/dia; cobertura = estoque 30 / 2 = 15 dias
        assertEquals(0, java.math.BigDecimal("2.00").compareTo(mine!!["dailyVelocity"].decimalValue()))
        assertEquals(15, mine["coverageDays"].asInt())
        // demanda 30 dias = 2*30 = 60; sugestão = 60 - 30 = 30
        assertEquals(30, mine["suggestedQty"].asInt())
        // resumo: ao menos este item conta e o custo total cobre o dele
        assertTrue(resp["totalItems"].asInt() >= 1)
        assertTrue(resp["totalEstimatedCost"].decimalValue() >= mine["estimatedCost"].decimalValue())
        // todos os itens listados têm sugestão > 0 (só o que importa)
        assertTrue(allItems(resp).all { it["suggestedQty"].asInt() > 0 })

        // agrupado pelo último fornecedor: o item deve estar num grupo com fornecedor nomeado e subtotal
        val group = groupOf(resp, variantId)
        assertNotNull(group, "o item deve estar em um grupo de fornecedor")
        assertTrue(group!!["supplierName"].asText().isNotBlank())
        assertTrue(group["estimatedCost"].decimalValue() >= mine["estimatedCost"].decimalValue())
    }

    @Test
    fun `variacao que nao vendeu no periodo nao aparece`() {
        val token = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(token, s)
        stockUp(token, variantId, s, qty = 10, unitCost = 40.0) // tem estoque, mas nunca vendeu

        val resp = suggestion(token, 90)
        assertNull(itemOf(resp, variantId), "sem vendas não deve sugerir compra")
    }

    @Test
    fun `estoque suficiente para o horizonte gera sugestao zero`() {
        val token = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(token, s)
        // estoque alto (500), vende pouco (5) em 90 dias -> velocidade baixa, estoque cobre de sobra
        stockUp(token, variantId, s, qty = 500, unitCost = 40.0)
        val channelId = firstChannelId(token)
        sell(token, channelId, variantId, 5)

        // estoque cobre de sobra -> sugestão zero -> não aparece na lista (só o que importa)
        assertNull(itemOf(suggestion(token, 90), variantId))
    }

    @Test
    fun `parametros de referencia e cobertura vem na resposta`() {
        val resp = suggestion(managerToken(), 90)
        assertEquals(90, resp["referenceDays"].asInt())
        assertTrue(resp["coverageTargetDays"].asInt() >= 1) // default 30
    }

    @Test
    fun `vendedor nao acessa a sugestao de compra (403)`() {
        val stoken = authHelper.bearerFor(Role.SELLER)
        mockMvc.perform(
            get("/reports/purchase-suggestion").header("Authorization", stoken).param("days", "90"),
        ).andExpect(status().isForbidden)
    }
}
