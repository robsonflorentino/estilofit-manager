package br.com.estilofitudi.sale

import br.com.estilofitudi.support.IntegrationTest
import br.com.estilofitudi.support.TestAuthHelper
import br.com.estilofitudi.user.domain.Role
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

class SaleIntegrationTest @Autowired constructor(
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

    /** Cria categoria + produto + 1 variação (estoque 0, sem preço) e retorna variantId. */
    private fun setupVariant(token: String, suffix: String): String {
        val catId = postId("/categories", token, """{"name":"Cat $suffix"}""")
        val prodId = postId("/products", token, """{"name":"Prod $suffix","categoryId":"$catId"}""")
        val varJson = mockMvc.perform(
            post("/products/$prodId/variants").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("""{"size":"M","color":"Azul"}"""),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        return objectMapper.readTree(varJson)["id"].asText()
    }

    /** Registra um lote que dá estoque e preço à variação. Custo -> salePrice = custo * 2 (margem 100%). */
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
        val json = mockMvc.perform(
            get("/sale-channels").header("Authorization", token),
        ).andExpect(status().isOk).andReturn().response.contentAsString
        val channels = objectMapper.readTree(json)
        assertTrue(channels.size() > 0, "deveria haver ao menos um canal de venda (seed V7)")
        return channels[0]["id"].asText()
    }

    private fun stockQty(token: String, variantId: String): Int {
        val json = mockMvc.perform(
            get("/stock/summary").header("Authorization", token).param("page", "0").param("size", "200"),
        ).andExpect(status().isOk).andReturn().response.contentAsString
        val item = objectMapper.readTree(json)["content"].first { it["variantId"].asText() == variantId }
        return item["stockQuantity"].asInt()
    }

    @Test
    fun `venda a vista debita o estoque e nao gera parcelas`() {
        val token = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(token, s)
        stockUp(token, variantId, s, qty = 10, unitCost = 50.0) // salePrice = 100
        val channelId = firstChannelId(token)

        val body = objectMapper.writeValueAsString(
            mapOf(
                "channelId" to channelId,
                "paymentMethod" to "CASH",
                "installments" to 1,
                "items" to listOf(mapOf("variantId" to variantId, "quantity" to 3)),
            ),
        )

        mockMvc.perform(
            post("/sales").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(body),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.totalAmount").value(300.00)) // 3 x 100
            .andExpect(jsonPath("$.finalAmount").value(300.00))
            .andExpect(jsonPath("$.installmentSchedule.length()").value(0))
            .andExpect(jsonPath("$.items[0].unitPrice").value(100.00)) // snapshot do backend

        assertEquals(7, stockQty(token, variantId)) // 10 - 3
    }

    @Test
    fun `venda parcelada no cartao gera parcelas com vencimento e liquido`() {
        val token = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(token, s)
        stockUp(token, variantId, s, qty = 10, unitCost = 50.0) // salePrice 100
        val channelId = firstChannelId(token)

        val body = objectMapper.writeValueAsString(
            mapOf(
                "channelId" to channelId,
                "paymentMethod" to "CREDIT_CARD",
                "installments" to 3,
                "cardFeePct" to 10.0,
                "items" to listOf(mapOf("variantId" to variantId, "quantity" to 3)), // total 300
            ),
        )

        mockMvc.perform(
            post("/sales").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(body),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.installmentSchedule.length()").value(3))
            .andExpect(jsonPath("$.installmentSchedule[0].grossAmount").value(100.00))
            .andExpect(jsonPath("$.installmentSchedule[0].netAmount").value(90.00)) // 100 - 10%
    }

    @Test
    fun `venda de variacao sem preco retorna 422`() {
        val token = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(token, s) // sem lote -> sem preço e sem estoque
        val channelId = firstChannelId(token)

        val body = objectMapper.writeValueAsString(
            mapOf(
                "channelId" to channelId,
                "paymentMethod" to "CASH",
                "items" to listOf(mapOf("variantId" to variantId, "quantity" to 1)),
            ),
        )

        mockMvc.perform(
            post("/sales").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(body),
        ).andExpect(status().isUnprocessableEntity)
    }

    @Test
    fun `venda com estoque insuficiente retorna 422 e nao debita nada`() {
        val token = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(token, s)
        stockUp(token, variantId, s, qty = 2, unitCost = 50.0)
        val channelId = firstChannelId(token)

        val body = objectMapper.writeValueAsString(
            mapOf(
                "channelId" to channelId,
                "paymentMethod" to "CASH",
                "items" to listOf(mapOf("variantId" to variantId, "quantity" to 5)), // > 2 disponíveis
            ),
        )

        mockMvc.perform(
            post("/sales").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(body),
        ).andExpect(status().isUnprocessableEntity)

        assertEquals(2, stockQty(token, variantId)) // estoque intacto
    }

    @Test
    fun `cancelamento estorna o estoque e cancela parcelas pendentes`() {
        val token = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(token, s)
        stockUp(token, variantId, s, qty = 10, unitCost = 50.0)
        val channelId = firstChannelId(token)

        val saleId = postId(
            "/sales", token,
            objectMapper.writeValueAsString(
                mapOf(
                    "channelId" to channelId,
                    "paymentMethod" to "CREDIT_CARD",
                    "installments" to 3,
                    "cardFeePct" to 10.0,
                    "items" to listOf(mapOf("variantId" to variantId, "quantity" to 4)),
                ),
            ),
        )
        assertEquals(6, stockQty(token, variantId)) // 10 - 4

        mockMvc.perform(
            patch("/sales/$saleId/cancel").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("""{"reason":"cliente desistiu da compra"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CANCELLED"))
            .andExpect(jsonPath("$.installmentSchedule[0].status").value("CANCELLED"))

        assertEquals(10, stockQty(token, variantId)) // estoque estornado
    }

    @Test
    fun `vendedor so enxerga as proprias vendas`() {
        val mtoken = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(mtoken, s)
        stockUp(mtoken, variantId, s, qty = 10, unitCost = 50.0)
        val channelId = firstChannelId(mtoken)

        // dois vendedores distintos
        val sellerA = authHelper.createUser(Role.SELLER)
        val sellerB = authHelper.createUser(Role.SELLER)
        val tokenA = "Bearer ${authHelper.tokenFor(sellerA)}"
        val tokenB = "Bearer ${authHelper.tokenFor(sellerB)}"

        val saleBody = objectMapper.writeValueAsString(
            mapOf(
                "channelId" to channelId,
                "paymentMethod" to "CASH",
                "items" to listOf(mapOf("variantId" to variantId, "quantity" to 1)),
            ),
        )
        // vendedor A cria uma venda
        val saleId = postId("/sales", tokenA, saleBody)

        // vendedor A vê a própria
        val listA = mockMvc.perform(get("/sales").header("Authorization", tokenA))
            .andExpect(status().isOk).andReturn().response.contentAsString
        assertTrue(objectMapper.readTree(listA)["content"].any { it["id"].asText() == saleId })

        // vendedor B não vê a venda de A na listagem
        val listB = mockMvc.perform(get("/sales").header("Authorization", tokenB))
            .andExpect(status().isOk).andReturn().response.contentAsString
        assertTrue(objectMapper.readTree(listB)["content"].none { it["id"].asText() == saleId })

        // vendedor B não acessa o detalhe (404)
        mockMvc.perform(get("/sales/$saleId").header("Authorization", tokenB))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `baixa de parcela move status para RECEIVED`() {
        val token = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(token, s)
        stockUp(token, variantId, s, qty = 10, unitCost = 50.0)
        val channelId = firstChannelId(token)

        val saleJson = mockMvc.perform(
            post("/sales").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(
                objectMapper.writeValueAsString(
                    mapOf(
                        "channelId" to channelId,
                        "paymentMethod" to "CREDIT_CARD",
                        "installments" to 2,
                        "cardFeePct" to 5.0,
                        "items" to listOf(mapOf("variantId" to variantId, "quantity" to 2)),
                    ),
                ),
            ),
        ).andExpect(status().isCreated).andReturn().response.contentAsString

        val installmentId = objectMapper.readTree(saleJson)["installmentSchedule"][0]["id"].asText()

        mockMvc.perform(
            patch("/installments/$installmentId/receive").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("RECEIVED"))

        // segunda baixa na mesma parcela deve falhar (não está mais PENDING)
        mockMvc.perform(
            patch("/installments/$installmentId/receive").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{}"),
        ).andExpect(status().isUnprocessableEntity)
    }

    @Test
    fun `vendedor nao pode cancelar venda`() {
        val mtoken = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(mtoken, s)
        stockUp(mtoken, variantId, s, qty = 10, unitCost = 50.0)
        val channelId = firstChannelId(mtoken)
        val stoken = authHelper.bearerFor(Role.SELLER)

        val saleId = postId(
            "/sales", stoken,
            objectMapper.writeValueAsString(
                mapOf(
                    "channelId" to channelId,
                    "paymentMethod" to "CASH",
                    "items" to listOf(mapOf("variantId" to variantId, "quantity" to 1)),
                ),
            ),
        )

        mockMvc.perform(
            patch("/sales/$saleId/cancel").header("Authorization", stoken)
                .contentType(MediaType.APPLICATION_JSON).content("""{"reason":"tentativa indevida"}"""),
        ).andExpect(status().isForbidden)
    }
}
