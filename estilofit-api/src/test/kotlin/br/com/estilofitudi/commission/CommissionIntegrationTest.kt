package br.com.estilofitudi.commission

import br.com.estilofitudi.support.IntegrationTest
import br.com.estilofitudi.support.TestAuthHelper
import br.com.estilofitudi.user.domain.Role
import br.com.estilofitudi.user.domain.User
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.math.BigDecimal
import java.time.LocalDate

class CommissionIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val authHelper: TestAuthHelper,
    private val objectMapper: ObjectMapper,
) : IntegrationTest() {

    private fun managerToken() = authHelper.bearerFor(Role.MANAGER)
    private val monthStart = LocalDate.now().withDayOfMonth(1)
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

    private fun createSaleJson(token: String, channelId: String, variantId: String, qty: Int): JsonNode {
        val json = mockMvc.perform(
            post("/sales").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(
                objectMapper.writeValueAsString(
                    mapOf(
                        "channelId" to channelId,
                        "paymentMethod" to "CASH",
                        "items" to listOf(mapOf("variantId" to variantId, "quantity" to qty)),
                    ),
                ),
            ),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        return objectMapper.readTree(json)
    }

    private fun commissions(token: String): JsonNode {
        val json = mockMvc.perform(
            get("/commissions").header("Authorization", token)
                .param("startDate", monthStart.toString())
                .param("endDate", today.toString()),
        ).andExpect(status().isOk).andReturn().response.contentAsString
        return objectMapper.readTree(json)
    }

    @Test
    fun `venda de vendedor grava comissao de 5 por cento no snapshot`() {
        val mtoken = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(mtoken, s)
        stockUp(mtoken, variantId, s, qty = 100, unitCost = 50.0) // salePrice 100
        val channelId = firstChannelId(mtoken)

        val seller = authHelper.createUser(Role.SELLER)
        val stoken = "Bearer ${authHelper.tokenFor(seller)}"

        val sale = createSaleJson(stoken, channelId, variantId, 4) // faturamento 400
        assertEquals(0, BigDecimal("5.00").compareTo(sale["commissionPct"].decimalValue()))
        assertEquals(0, BigDecimal("20.00").compareTo(sale["commissionAmount"].decimalValue())) // 5% de 400
    }

    @Test
    fun `venda de gestor nao gera comissao`() {
        val mtoken = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(mtoken, s)
        stockUp(mtoken, variantId, s, qty = 100, unitCost = 50.0)
        val channelId = firstChannelId(mtoken)

        val sale = createSaleJson(mtoken, channelId, variantId, 4)
        assertEquals(0, BigDecimal.ZERO.compareTo(sale["commissionPct"].decimalValue()))
        assertEquals(0, BigDecimal.ZERO.compareTo(sale["commissionAmount"].decimalValue()))
    }

    @Test
    fun `relatorio de comissoes soma por vendedor e traz total`() {
        val mtoken = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(mtoken, s)
        stockUp(mtoken, variantId, s, qty = 200, unitCost = 50.0)
        val channelId = firstChannelId(mtoken)

        val seller = authHelper.createUser(Role.SELLER)
        val stoken = "Bearer ${authHelper.tokenFor(seller)}"

        createSaleJson(stoken, channelId, variantId, 3) // 300 -> comissão 15
        createSaleJson(stoken, channelId, variantId, 2) // 200 -> comissão 10

        val resp = commissions(mtoken)
        val mine = resp["sellers"].firstOrNull { it["sellerId"].asText() == seller.id.toString() }
        assertNotNull(mine, "vendedor deveria aparecer nas comissões")
        assertEquals(0, BigDecimal("500.00").compareTo(mine!!["revenue"].decimalValue()))
        assertEquals(0, BigDecimal("25.00").compareTo(mine["commissionAmount"].decimalValue())) // 5% de 500
        assertEquals(2, mine["saleCount"].asLong())
        // total geral inclui pelo menos a comissão deste vendedor
        org.junit.jupiter.api.Assertions.assertTrue(
            resp["totalCommission"].decimalValue() >= BigDecimal("25.00"),
        )
    }

    @Test
    fun `venda cancelada nao paga comissao`() {
        val mtoken = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(mtoken, s)
        stockUp(mtoken, variantId, s, qty = 100, unitCost = 50.0)
        val channelId = firstChannelId(mtoken)

        val seller = authHelper.createUser(Role.SELLER)
        val stoken = "Bearer ${authHelper.tokenFor(seller)}"

        val sale = createSaleJson(stoken, channelId, variantId, 2) // 200 -> comissão 10
        val saleId = sale["id"].asText()
        mockMvc.perform(
            patch("/sales/$saleId/cancel").header("Authorization", mtoken)
                .contentType(MediaType.APPLICATION_JSON).content("""{"reason":"teste comissao cancelamento"}"""),
        ).andExpect(status().isOk)

        // após cancelar a única venda, o vendedor não deve constar nas comissões
        val resp = commissions(mtoken)
        val mine = resp["sellers"].firstOrNull { it["sellerId"].asText() == seller.id.toString() }
        assertNull(mine, "venda cancelada não deve gerar comissão")
    }

    @Test
    fun `vendedor nao acessa comissoes (403)`() {
        val stoken = authHelper.bearerFor(Role.SELLER)
        mockMvc.perform(
            get("/commissions").header("Authorization", stoken)
                .param("startDate", monthStart.toString())
                .param("endDate", today.toString()),
        ).andExpect(status().isForbidden)
    }
}
