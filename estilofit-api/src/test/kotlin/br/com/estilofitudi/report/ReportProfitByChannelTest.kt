package br.com.estilofitudi.report

import br.com.estilofitudi.support.IntegrationTest
import br.com.estilofitudi.support.TestAuthHelper
import br.com.estilofitudi.user.domain.Role
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.math.BigDecimal
import java.time.LocalDate

class ReportProfitByChannelTest @Autowired constructor(
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

    private fun createChannel(token: String, name: String): String =
        postId("/sale-channels", token, """{"name":"$name"}""")

    private fun createSale(token: String, channelId: String, variantId: String, qty: Int): String =
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

    private fun profitByChannel(token: String): JsonNode {
        val json = mockMvc.perform(
            get("/reports/profit-by-channel").header("Authorization", token)
                .param("startDate", monthStart.toString())
                .param("endDate", today.toString()),
        ).andExpect(status().isOk).andReturn().response.contentAsString
        return objectMapper.readTree(json)
    }

    @Test
    fun `lucro por canal calcula receita menos custo e margem`() {
        val token = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(token, s)
        // custo 40 -> salePrice 80 (margem 100% markup); margem sobre venda = 50%
        stockUp(token, variantId, s, qty = 100, unitCost = 40.0)
        val channelName = "LucroCanal-$s"
        val channelId = createChannel(token, channelName)

        createSale(token, channelId, variantId, 5) // receita 400, custo 200, lucro 200

        val rows = profitByChannel(token)
        val mine = rows.firstOrNull { it["channel"].asText() == channelName }
        assertNotNull(mine, "canal exclusivo deveria aparecer")
        assertEquals(0, BigDecimal("400.00").compareTo(mine!!["revenue"].decimalValue()))
        assertEquals(0, BigDecimal("200.00").compareTo(mine["cost"].decimalValue()))
        assertEquals(0, BigDecimal("200.00").compareTo(mine["profit"].decimalValue()))
        assertEquals(0, BigDecimal("50.00").compareTo(mine["marginPct"].decimalValue())) // 200/400
        assertEquals(1, mine["saleCount"].asLong())
    }

    @Test
    fun `resultado vem ordenado do canal mais lucrativo para o menos`() {
        val token = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(token, s)
        stockUp(token, variantId, s, qty = 200, unitCost = 40.0)
        val chA = createChannel(token, "CanalA-$s")
        val chB = createChannel(token, "CanalB-$s")

        createSale(token, chA, variantId, 2)  // lucro 80
        createSale(token, chB, variantId, 6)  // lucro 240

        val rows = profitByChannel(token)
        // lucros devem estar em ordem decrescente ao longo de toda a lista
        val profits = (0 until rows.size()).map { rows[it]["profit"].decimalValue() }
        for (i in 1 until profits.size) {
            org.junit.jupiter.api.Assertions.assertTrue(
                profits[i - 1] >= profits[i],
                "lista deveria estar ordenada por lucro desc",
            )
        }
        // CanalB (240) deve aparecer antes de CanalA (80)
        val idxA = (0 until rows.size()).first { rows[it]["channel"].asText() == "CanalA-$s" }
        val idxB = (0 until rows.size()).first { rows[it]["channel"].asText() == "CanalB-$s" }
        org.junit.jupiter.api.Assertions.assertTrue(idxB < idxA, "CanalB mais lucrativo deve vir antes")
    }

    @Test
    fun `venda cancelada nao entra no lucro do canal`() {
        val token = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(token, s)
        stockUp(token, variantId, s, qty = 50, unitCost = 40.0)
        val channelName = "CancelCanal-$s"
        val channelId = createChannel(token, channelName)

        createSale(token, channelId, variantId, 2)          // fica: lucro 80
        val cancelId = createSale(token, channelId, variantId, 3) // será cancelada
        mockMvc.perform(
            patch("/sales/$cancelId/cancel").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("""{"reason":"teste lucro canal cancelamento"}"""),
        ).andExpect(status().isOk)

        val mine = profitByChannel(token).first { it["channel"].asText() == channelName }
        assertEquals(0, BigDecimal("160.00").compareTo(mine["revenue"].decimalValue())) // só a venda de 2x80
        assertEquals(0, BigDecimal("80.00").compareTo(mine["profit"].decimalValue()))
        assertEquals(1, mine["saleCount"].asLong())
    }

    @Test
    fun `vendedor nao acessa lucro por canal (403)`() {
        val stoken = authHelper.bearerFor(Role.SELLER)
        mockMvc.perform(
            get("/reports/profit-by-channel").header("Authorization", stoken)
                .param("startDate", monthStart.toString())
                .param("endDate", today.toString()),
        ).andExpect(status().isForbidden)
    }
}
