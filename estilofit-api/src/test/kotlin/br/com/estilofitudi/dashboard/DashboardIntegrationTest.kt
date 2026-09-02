package br.com.estilofitudi.dashboard

import br.com.estilofitudi.support.IntegrationTest
import br.com.estilofitudi.support.TestAuthHelper
import br.com.estilofitudi.user.domain.Role
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDate

class DashboardIntegrationTest @Autowired constructor(
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

    /** Registra lote -> estoque + averageCost = unitCost, salePrice = unitCost * 2 (margem 100%). */
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

    private fun createSale(token: String, channelId: String, variantId: String, qty: Int): String {
        return postId(
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

    private fun kpis(token: String): JsonNode {
        val json = mockMvc.perform(get("/dashboard/kpis").header("Authorization", token))
            .andExpect(status().isOk).andReturn().response.contentAsString
        return objectMapper.readTree(json)
    }

    @Test
    fun `gestor recebe todos os KPIs incluindo estoque e pro-labore`() {
        val token = managerToken()
        val k = kpis(token)
        // Campos de gestão presentes e não nulos
        assertTrue(!k["stockItems"].isNull, "stockItems deveria vir preenchido para gestor")
        assertTrue(!k["estimatedProLabore"].isNull, "estimatedProLabore deveria vir preenchido para gestor")
        assertTrue(k.has("monthRevenue") && k.has("saleCount"))
    }

    @Test
    fun `vendedor nao recebe estoque nem pro-labore`() {
        val stoken = authHelper.bearerFor(Role.SELLER)
        val k = kpis(stoken)
        assertTrue(k["stockItems"].isNull, "vendedor não deve ver stockItems")
        assertTrue(k["estimatedProLabore"].isNull, "vendedor não deve ver pró-labore")
        assertTrue(k.has("monthRevenue") && k.has("saleCount"))
    }

    @Test
    fun `KPIs do vendedor refletem apenas as proprias vendas`() {
        val mtoken = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(mtoken, s)
        stockUp(mtoken, variantId, s, qty = 100, unitCost = 50.0) // salePrice 100
        val channelId = firstChannelId(mtoken)

        // vendedor novo (sem histórico) -> começa zerado neste mês
        val seller = authHelper.createUser(Role.SELLER)
        val stoken = "Bearer ${authHelper.tokenFor(seller)}"

        val before = kpis(stoken)
        assertEquals(0, before["saleCount"].asLong())

        // vendedor faz 2 vendas: 1un + 2un = 3un x 100 = 300
        createSale(stoken, channelId, variantId, 1)
        createSale(stoken, channelId, variantId, 2)

        val after = kpis(stoken)
        assertEquals(2, after["saleCount"].asLong())
        assertEquals(0, java.math.BigDecimal("300.00").compareTo(after["monthRevenue"].decimalValue()))
    }

    @Test
    fun `venda cancelada nao entra no faturamento do vendedor`() {
        val mtoken = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(mtoken, s)
        stockUp(mtoken, variantId, s, qty = 50, unitCost = 50.0)
        val channelId = firstChannelId(mtoken)

        val seller = authHelper.createUser(Role.SELLER)
        val stoken = "Bearer ${authHelper.tokenFor(seller)}"

        val saleId = createSale(stoken, channelId, variantId, 2) // 200
        createSale(stoken, channelId, variantId, 1)              // 100

        // gestor cancela a primeira venda
        mockMvc.perform(
            patch("/sales/$saleId/cancel").header("Authorization", mtoken)
                .contentType(MediaType.APPLICATION_JSON).content("""{"reason":"teste de cancelamento no dashboard"}"""),
        ).andExpect(status().isOk)

        val k = kpis(stoken)
        // resta apenas a venda de 100, e a contagem cai para 1
        assertEquals(1, k["saleCount"].asLong())
        assertEquals(0, java.math.BigDecimal("100.00").compareTo(k["monthRevenue"].decimalValue()))
    }

    @Test
    fun `estoque no KPI do gestor cobre as variacoes ativas`() {
        val token = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(token, s)

        val before = kpis(token)["stockItems"].asLong()
        stockUp(token, variantId, s, qty = 7, unitCost = 50.0) // +7 no estoque global

        val after = kpis(token)["stockItems"].asLong()
        // outros testes podem alterar o estoque global em paralelo; garante ao menos o incremento próprio
        assertTrue(after >= before + 7, "estoque global deveria crescer ao menos 7 (de $before para $after)")
    }

    @Test
    fun `kpis sem token retorna 401`() {
        mockMvc.perform(get("/dashboard/kpis")).andExpect(status().isUnauthorized)
    }
}
