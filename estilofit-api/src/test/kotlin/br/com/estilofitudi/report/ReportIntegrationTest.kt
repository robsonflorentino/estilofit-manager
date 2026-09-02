package br.com.estilofitudi.report

import br.com.estilofitudi.support.IntegrationTest
import br.com.estilofitudi.support.TestAuthHelper
import br.com.estilofitudi.user.domain.Role
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

class ReportIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val authHelper: TestAuthHelper,
    private val objectMapper: ObjectMapper,
) : IntegrationTest() {

    private fun managerToken() = authHelper.bearerFor(Role.MANAGER)

    private val today = LocalDate.now()
    private val monthStart = today.withDayOfMonth(1)

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

    private fun createChannel(token: String, name: String): String =
        postId("/sale-channels", token, """{"name":"$name"}""")

    private fun createSale(
        token: String,
        channelId: String,
        variantId: String,
        qty: Int,
        paymentMethod: String = "CASH",
    ): String =
        postId(
            "/sales", token,
            objectMapper.writeValueAsString(
                mapOf(
                    "channelId" to channelId,
                    "paymentMethod" to paymentMethod,
                    "items" to listOf(mapOf("variantId" to variantId, "quantity" to qty)),
                ),
            ),
        )

    private fun get(url: String, token: String): JsonNode {
        val json = mockMvc.perform(
            get(url).header("Authorization", token)
                .param("startDate", monthStart.toString())
                .param("endDate", today.toString()),
        ).andExpect(status().isOk).andReturn().response.contentAsString
        return objectMapper.readTree(json)
    }

    @Test
    fun `resumo do periodo traz faturamento, ticket medio e lucro coerentes`() {
        val token = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(token, s)
        stockUp(token, variantId, s, qty = 100, unitCost = 50.0) // salePrice 100, averageCost 50
        val channelId = firstChannelId(token)

        val before = get("/reports/summary", token)
        val revBefore = before["revenue"].decimalValue()
        val countBefore = before["saleCount"].asLong()
        val profitBefore = before["estimatedProfit"].decimalValue()

        // 2 vendas: 3un e 2un -> 5un x 100 = 500 de faturamento; custo 5x50=250; lucro 250
        createSale(token, channelId, variantId, 3)
        createSale(token, channelId, variantId, 2)

        val after = get("/reports/summary", token)
        assertEquals(0, BigDecimal("500.00").compareTo(after["revenue"].decimalValue().subtract(revBefore)))
        assertEquals(2, after["saleCount"].asLong() - countBefore)
        assertEquals(0, BigDecimal("250.00").compareTo(after["estimatedProfit"].decimalValue().subtract(profitBefore)))
        // ticket médio = faturamento total / nº vendas (>0)
        assertTrue(after["averageTicket"].decimalValue() > BigDecimal.ZERO)
    }

    @Test
    fun `venda cancelada nao entra no resumo`() {
        val token = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(token, s)
        stockUp(token, variantId, s, qty = 50, unitCost = 50.0)
        val channelId = firstChannelId(token)

        val before = get("/reports/summary", token)["revenue"].decimalValue()

        val saleId = createSale(token, channelId, variantId, 2) // 200
        mockMvc.perform(
            patch("/sales/$saleId/cancel").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("""{"reason":"teste relatorio cancelamento"}"""),
        ).andExpect(status().isOk)

        val after = get("/reports/summary", token)["revenue"].decimalValue()
        assertEquals(0, before.compareTo(after), "faturamento não deve mudar após cancelar a única venda nova")
    }

    @Test
    fun `top produtos ranqueia a variacao vendida`() {
        val token = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(token, s)
        stockUp(token, variantId, s, qty = 100, unitCost = 50.0)
        val channelId = firstChannelId(token)

        createSale(token, channelId, variantId, 7) // 7 unidades

        val rows = get("/reports/top-products", token)
        val mine = rows.firstOrNull { it["variantId"].asText() == variantId }
        assertNotNull(mine, "a variação vendida deveria aparecer no ranking")
        assertEquals(7, mine!!["quantity"].asLong())
        assertEquals(0, BigDecimal("700.00").compareTo(mine["revenue"].decimalValue())) // 7 x 100
    }

    @Test
    fun `por canal agrupa o faturamento do canal exclusivo`() {
        val token = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(token, s)
        stockUp(token, variantId, s, qty = 100, unitCost = 50.0)
        val channelId = createChannel(token, "Canal-$s")

        createSale(token, channelId, variantId, 4) // 400 nesse canal exclusivo

        val rows = get("/reports/by-channel", token)
        val mine = rows.firstOrNull { it["label"].asText() == "Canal-$s" }
        assertNotNull(mine, "o canal exclusivo deveria aparecer")
        assertEquals(0, BigDecimal("400.00").compareTo(mine!!["revenue"].decimalValue()))
        assertEquals(1, mine["saleCount"].asLong())
        // percentage é participação no total (0-100)
        assertTrue(mine["percentage"].decimalValue() > BigDecimal.ZERO)
    }

    @Test
    fun `por forma de pagamento agrega e inclui TRANSFER`() {
        val token = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(token, s)
        stockUp(token, variantId, s, qty = 100, unitCost = 50.0)
        val channelId = firstChannelId(token)

        // TRANSFER agora é válido (fix desta feature)
        createSale(token, channelId, variantId, 1, paymentMethod = "TRANSFER")

        val rows = get("/reports/by-payment", token)
        val labels = rows.map { it["label"].asText() }
        assertTrue(labels.contains("TRANSFER"), "TRANSFER deveria aparecer no relatório por pagamento")
    }

    @Test
    fun `revenue-by-day inclui o dia de hoje`() {
        val token = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(token, s)
        stockUp(token, variantId, s, qty = 100, unitCost = 50.0)
        val channelId = firstChannelId(token)

        createSale(token, channelId, variantId, 1)

        val rows = get("/reports/revenue-by-day", token)
        val todayRow = rows.firstOrNull { it["day"].asText() == today.toString() }
        assertNotNull(todayRow, "deveria haver uma linha para o dia de hoje")
        assertTrue(todayRow!!["revenue"].decimalValue() > BigDecimal.ZERO)
    }

    @Test
    fun `vendedor nao acessa relatorios (403)`() {
        val stoken = authHelper.bearerFor(Role.SELLER)
        mockMvc.perform(
            get("/reports/summary").header("Authorization", stoken)
                .param("startDate", monthStart.toString())
                .param("endDate", today.toString()),
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `relatorio sem token retorna 401`() {
        mockMvc.perform(
            get("/reports/summary")
                .param("startDate", monthStart.toString())
                .param("endDate", today.toString()),
        ).andExpect(status().isUnauthorized)
    }
}
