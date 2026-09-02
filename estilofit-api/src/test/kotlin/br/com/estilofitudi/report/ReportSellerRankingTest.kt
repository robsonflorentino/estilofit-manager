package br.com.estilofitudi.report

import br.com.estilofitudi.support.IntegrationTest
import br.com.estilofitudi.support.TestAuthHelper
import br.com.estilofitudi.user.domain.Role
import br.com.estilofitudi.user.domain.User
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.math.BigDecimal
import java.time.LocalDate

class ReportSellerRankingTest @Autowired constructor(
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

    private fun sellerBearer(user: User) = "Bearer ${authHelper.tokenFor(user)}"

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

    private fun ranking(token: String): JsonNode {
        val json = mockMvc.perform(
            get("/reports/seller-ranking").header("Authorization", token)
                .param("startDate", monthStart.toString())
                .param("endDate", today.toString()),
        ).andExpect(status().isOk).andReturn().response.contentAsString
        return objectMapper.readTree(json)
    }

    @Test
    fun `ranking ordena vendedores por faturamento e calcula ticket medio`() {
        val mtoken = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(mtoken, s)
        stockUp(mtoken, variantId, s, qty = 1000, unitCost = 50.0) // salePrice 100
        val channelId = firstChannelId(mtoken)

        // 3 vendedores com faturamentos distintos: A=500 (5un), B=300 (3un), C=100 (1un)
        val a = authHelper.createUser(Role.SELLER)
        val b = authHelper.createUser(Role.SELLER)
        val c = authHelper.createUser(Role.SELLER)
        sell(sellerBearer(a), channelId, variantId, 5)
        sell(sellerBearer(b), channelId, variantId, 3)
        sell(sellerBearer(c), channelId, variantId, 1)

        val rows = ranking(mtoken)

        fun rowOf(u: User) = (0 until rows.size()).map { rows[it] }.first { it["sellerId"].asText() == u.id.toString() }
        val ra = rowOf(a); val rb = rowOf(b); val rc = rowOf(c)

        // faturamentos corretos
        assertEquals(0, BigDecimal("500.00").compareTo(ra["revenue"].decimalValue()))
        assertEquals(0, BigDecimal("300.00").compareTo(rb["revenue"].decimalValue()))
        assertEquals(0, BigDecimal("100.00").compareTo(rc["revenue"].decimalValue()))

        // ticket médio: A vendeu 1 venda de 5un=500 -> ticket 500
        assertEquals(0, BigDecimal("500.00").compareTo(ra["averageTicket"].decimalValue()))
        assertEquals(1, ra["saleCount"].asLong())

        // ordem relativa por posição: A antes de B antes de C
        assertTrue(ra["position"].asInt() < rb["position"].asInt())
        assertTrue(rb["position"].asInt() < rc["position"].asInt())
    }

    @Test
    fun `venda cancelada nao conta no faturamento do vendedor`() {
        val mtoken = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(mtoken, s)
        stockUp(mtoken, variantId, s, qty = 100, unitCost = 50.0)
        val channelId = firstChannelId(mtoken)

        val seller = authHelper.createUser(Role.SELLER)
        val stoken = sellerBearer(seller)

        // venda que fica (200) + venda que será cancelada (300)
        sell(stoken, channelId, variantId, 2)
        val cancelId = postId(
            "/sales", stoken,
            objectMapper.writeValueAsString(
                mapOf(
                    "channelId" to channelId,
                    "paymentMethod" to "CASH",
                    "items" to listOf(mapOf("variantId" to variantId, "quantity" to 3)),
                ),
            ),
        )
        mockMvc.perform(
            patch("/sales/$cancelId/cancel").header("Authorization", mtoken)
                .contentType(MediaType.APPLICATION_JSON).content("""{"reason":"teste ranking cancelamento"}"""),
        ).andExpect(status().isOk)

        val rows = ranking(mtoken)
        val mine = (0 until rows.size()).map { rows[it] }.first { it["sellerId"].asText() == seller.id.toString() }
        assertNotNull(mine)
        assertEquals(0, BigDecimal("200.00").compareTo(mine["revenue"].decimalValue()))
        assertEquals(1, mine["saleCount"].asLong())
    }

    @Test
    fun `vendedor nao acessa o ranking (403)`() {
        val stoken = authHelper.bearerFor(Role.SELLER)
        mockMvc.perform(
            get("/reports/seller-ranking").header("Authorization", stoken)
                .param("startDate", monthStart.toString())
                .param("endDate", today.toString()),
        ).andExpect(status().isForbidden)
    }
}
