package br.com.estilofitudi.promotion

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

class PromotionIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val authHelper: TestAuthHelper,
    private val objectMapper: ObjectMapper,
) : IntegrationTest() {

    private fun managerToken() = authHelper.bearerFor(Role.MANAGER)

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
                "receivedAt" to LocalDate.now().toString(),
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

    private fun createSale(token: String, channelId: String, variantId: String, qty: Int) {
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

    /** Consulta /promotions/stale com o parâmetro days. */
    private fun stale(token: String, days: Int): JsonNode {
        val json = mockMvc.perform(
            get("/promotions/stale").header("Authorization", token).param("days", days.toString()),
        ).andExpect(status().isOk).andReturn().response.contentAsString
        return objectMapper.readTree(json)
    }

    private fun itemOf(resp: JsonNode, variantId: String): JsonNode? =
        resp["items"].firstOrNull { it["variantId"].asText() == variantId }

    @Test
    fun `variacao com estoque que nunca vendeu aparece como parada (days=0)`() {
        val token = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(token, s)
        stockUp(token, variantId, s, qty = 5, unitCost = 40.0) // entrada hoje, nunca vendida

        val resp = stale(token, 0)
        val mine = itemOf(resp, variantId)
        assertNotNull(mine, "variação encalhada deveria aparecer com days=0")
        assertTrue(mine!!["neverSold"].asBoolean(), "deveria estar marcada como nunca vendida")
        assertNull(mine["lastSaleAt"].takeIf { !it.isNull }, "lastSaleAt deveria ser nulo")
        assertEquals(0, java.math.BigDecimal("200.00").compareTo(mine["stockValue"].decimalValue())) // 5 x 40
    }

    @Test
    fun `variacao vendida hoje nao aparece com days=1`() {
        val token = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(token, s)
        stockUp(token, variantId, s, qty = 10, unitCost = 40.0)
        val channelId = firstChannelId(token)
        createSale(token, channelId, variantId, 1) // vendida hoje -> 0 dias parada

        val resp = stale(token, 1)
        assertNull(itemOf(resp, variantId), "vendida hoje não deve aparecer com limiar de 1 dia")
    }

    @Test
    fun `variacao sem estoque nao aparece mesmo sem vendas`() {
        val token = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(token, s) // estoque 0, sem lote

        val resp = stale(token, 0)
        assertNull(itemOf(resp, variantId), "variação sem estoque não deve entrar no alerta")
    }

    @Test
    fun `limiar alto exclui variacao recem-cadastrada`() {
        val token = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(token, s)
        stockUp(token, variantId, s, qty = 5, unitCost = 40.0)

        // entrada foi hoje -> 0 dias; com limiar 60 não deve aparecer
        val resp = stale(token, 60)
        assertNull(itemOf(resp, variantId), "entrada recente não deve aparecer com limiar de 60 dias")
    }

    @Test
    fun `resumo traz threshold e capital parado`() {
        val token = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(token, s)
        stockUp(token, variantId, s, qty = 3, unitCost = 40.0) // 120 de capital

        val resp = stale(token, 0)
        assertEquals(0, resp["thresholdDays"].asInt())
        assertTrue(resp["staleCount"].asInt() >= 1)
        assertTrue(resp["totalStockValue"].decimalValue() >= java.math.BigDecimal("120.00"))
    }

    @Test
    fun `vendedor nao acessa alertas de promocao (403)`() {
        val stoken = authHelper.bearerFor(Role.SELLER)
        mockMvc.perform(get("/promotions/stale").header("Authorization", stoken).param("days", "0"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `alerta sem token retorna 401`() {
        mockMvc.perform(get("/promotions/stale").param("days", "0"))
            .andExpect(status().isUnauthorized)
    }
}
