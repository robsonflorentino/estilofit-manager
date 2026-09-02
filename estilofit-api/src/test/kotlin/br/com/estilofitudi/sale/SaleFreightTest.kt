package br.com.estilofitudi.sale

import br.com.estilofitudi.support.IntegrationTest
import br.com.estilofitudi.support.TestAuthHelper
import br.com.estilofitudi.user.domain.Role
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.math.BigDecimal
import java.time.LocalDate

class SaleFreightTest @Autowired constructor(
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
                "supplierId" to supplierId, "receivedAt" to LocalDate.now().toString(),
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

    private fun createSale(token: String, body: Map<String, Any?>): JsonNode {
        val json = mockMvc.perform(
            post("/sales").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        return objectMapper.readTree(json)
    }

    @Test
    fun `venda com frete pago soma no total sem alterar faturamento`() {
        val token = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(token, s)
        stockUp(token, variantId, s, qty = 50, unitCost = 50.0) // salePrice 100
        val channelId = firstChannelId(token)

        val sale = createSale(token, mapOf(
            "channelId" to channelId, "paymentMethod" to "CASH",
            "freightType" to "PAID", "freightAmount" to 25.00,
            "items" to listOf(mapOf("variantId" to variantId, "quantity" to 2)), // produtos = 200
        ))

        assertEquals("PAID", sale["freightType"].asText())
        assertEquals(0, BigDecimal("25.00").compareTo(sale["freightAmount"].decimalValue()))
        // faturamento (produtos) permanece 200; total pago = 200 + 25 = 225
        assertEquals(0, BigDecimal("200.00").compareTo(sale["finalAmount"].decimalValue()))
        assertEquals(0, BigDecimal("225.00").compareTo(sale["totalPaid"].decimalValue()))
    }

    @Test
    fun `frete gratis e sem frete ficam com valor zero`() {
        val token = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(token, s)
        stockUp(token, variantId, s, qty = 50, unitCost = 50.0)
        val channelId = firstChannelId(token)

        val free = createSale(token, mapOf(
            "channelId" to channelId, "paymentMethod" to "CASH", "freightType" to "FREE",
            "items" to listOf(mapOf("variantId" to variantId, "quantity" to 1)),
        ))
        assertEquals("FREE", free["freightType"].asText())
        assertEquals(0, BigDecimal.ZERO.compareTo(free["freightAmount"].decimalValue()))
        assertEquals(0, free["finalAmount"].decimalValue().compareTo(free["totalPaid"].decimalValue()))

        val none = createSale(token, mapOf(
            "channelId" to channelId, "paymentMethod" to "CASH",
            "items" to listOf(mapOf("variantId" to variantId, "quantity" to 1)),
        ))
        assertEquals("NONE", none["freightType"].asText())
        assertEquals(0, BigDecimal.ZERO.compareTo(none["freightAmount"].decimalValue()))
    }

    @Test
    fun `frete pago sem valor retorna 422`() {
        val token = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(token, s)
        stockUp(token, variantId, s, qty = 50, unitCost = 50.0)
        val channelId = firstChannelId(token)

        mockMvc.perform(
            post("/sales").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(
                objectMapper.writeValueAsString(mapOf(
                    "channelId" to channelId, "paymentMethod" to "CASH", "freightType" to "PAID",
                    "items" to listOf(mapOf("variantId" to variantId, "quantity" to 1)),
                )),
            ),
        ).andExpect(status().isUnprocessableEntity)
    }
}
