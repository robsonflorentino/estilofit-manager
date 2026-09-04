package br.com.estilofitudi.inventory

import br.com.estilofitudi.support.IntegrationTest
import br.com.estilofitudi.support.TestAuthHelper
import br.com.estilofitudi.user.domain.Role
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDate

class InventoryIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val authHelper: TestAuthHelper,
    private val objectMapper: ObjectMapper,
) : IntegrationTest() {

    private fun managerToken() = authHelper.bearerFor(Role.MANAGER)
    private fun sellerToken() = authHelper.bearerFor(Role.SELLER)

    private fun postId(url: String, token: String, body: String): String {
        val json = mockMvc.perform(
            post(url).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(body),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        return objectMapper.readTree(json)["id"].asText()
    }

    /** Cria categoria + produto + 1 variação e retorna (variantId). */
    private fun setupVariant(token: String, suffix: String): String {
        val catId = postId("/categories", token, """{"name":"Cat $suffix"}""")
        val prodId = postId("/products", token, """{"name":"Prod $suffix","categoryId":"$catId"}""")
        val varJson = mockMvc.perform(
            post("/products/$prodId/variants").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("""{"size":"M","color":"Azul"}"""),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        return objectMapper.readTree(varJson)["id"].asText()
    }

    private fun createSupplier(token: String, suffix: String): String =
        postId("/suppliers", token, """{"name":"Forn $suffix"}""")

    @Test
    fun `registrar lote atualiza estoque, custo e preco da variacao`() {
        val token = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(token, s)
        val supplierId = createSupplier(token, s)

        // lote: 10 un x custo 30, frete 200 (um item só absorve todo o frete)
        val body = objectMapper.writeValueAsString(
            mapOf(
                "supplierId" to supplierId,
                "receivedAt" to LocalDate.now().toString(),
                "freightCost" to 200.00,
                "items" to listOf(mapOf("variantId" to variantId, "quantity" to 10, "unitCost" to 30.00)),
            ),
        )

        mockMvc.perform(
            post("/supply-lots").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(body),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.items[0].realUnitCost").value(50.00)) // 30 + 200/10
            .andExpect(jsonPath("$.totalCost").value(500.00))            // 300 + 200

        // consulta o produto: variação deve ter estoque 10, custo 50, preço 100 (margem 100%)
        val stockJson = mockMvc.perform(
            get("/stock/summary").header("Authorization", token).param("size", "M"),
        ).andExpect(status().isOk).andReturn().response.contentAsString
        val item = objectMapper.readTree(stockJson)["content"].first { it["variantId"].asText() == variantId }
        org.junit.jupiter.api.Assertions.assertEquals(10, item["stockQuantity"].asInt())
        org.junit.jupiter.api.Assertions.assertEquals(50.0, item["averageCost"].asDouble())
        org.junit.jupiter.api.Assertions.assertEquals(100.0, item["salePrice"].asDouble())
    }

    @Test
    fun `segundo lote aplica custo medio ponderado`() {
        val token = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(token, s)
        val supplierId = createSupplier(token, s)

        fun lote(qty: Int, cost: Double) = objectMapper.writeValueAsString(
            mapOf(
                "supplierId" to supplierId,
                "receivedAt" to LocalDate.now().toString(),
                "freightCost" to 0.00,
                "items" to listOf(mapOf("variantId" to variantId, "quantity" to qty, "unitCost" to cost)),
            ),
        )

        // 1o lote: 10 un a custo 40 (frete 0)
        mockMvc.perform(post("/supply-lots").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(lote(10, 40.0))).andExpect(status().isCreated)
        // 2o lote: 10 un a custo 60 -> media ponderada = (10*40 + 10*60)/20 = 50
        mockMvc.perform(post("/supply-lots").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(lote(10, 60.0))).andExpect(status().isCreated)

        val stockJson = mockMvc.perform(
            get("/stock/summary").header("Authorization", token).param("size", "M"),
        ).andReturn().response.contentAsString
        val item = objectMapper.readTree(stockJson)["content"].first { it["variantId"].asText() == variantId }
        org.junit.jupiter.api.Assertions.assertEquals(20, item["stockQuantity"].asInt())
        org.junit.jupiter.api.Assertions.assertEquals(50.0, item["averageCost"].asDouble())
    }

    @Test
    fun `ajuste manual negativo alem do estoque retorna 422`() {
        val token = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(token, s)

        // variação recém-criada tem estoque 0; ajuste -5 deve falhar
        val body = objectMapper.writeValueAsString(
            mapOf("variantId" to variantId, "quantity" to -5, "notes" to "teste de estoque negativo"),
        )
        mockMvc.perform(
            post("/stock/adjustments").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(body),
        ).andExpect(status().isUnprocessableEntity)
    }

    @Test
    fun `ajuste manual sem justificativa retorna 400`() {
        val token = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(token, s)

        val body = objectMapper.writeValueAsString(
            mapOf("variantId" to variantId, "quantity" to 5, "notes" to ""),
        )
        mockMvc.perform(
            post("/stock/adjustments").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(body),
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `vendedor pode consultar estoque mas nao registrar lote`() {
        val stoken = sellerToken()
        mockMvc.perform(get("/stock/summary").header("Authorization", stoken))
            .andExpect(status().isOk)

        val body = objectMapper.writeValueAsString(
            mapOf(
                "supplierId" to "00000000-0000-0000-0000-000000000000",
                "receivedAt" to LocalDate.now().toString(),
                "items" to listOf(mapOf("variantId" to "00000000-0000-0000-0000-000000000000", "quantity" to 1, "unitCost" to 10.0)),
            ),
        )
        mockMvc.perform(
            post("/supply-lots").header("Authorization", stoken)
                .contentType(MediaType.APPLICATION_JSON).content(body),
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `stock summary sem filtros lista as variacoes`() {
        val token = managerToken()
        val s = System.nanoTime().toString()
        val variantId = setupVariant(token, s)

        // resumo SEM nenhum filtro (cenário da tela de estoque) deve conter a variação criada
        val json = mockMvc.perform(
            get("/stock/summary").header("Authorization", token)
                .param("page", "0").param("size", "50"),
        ).andExpect(status().isOk).andReturn().response.contentAsString

        val node = objectMapper.readTree(json)
        org.junit.jupiter.api.Assertions.assertTrue(
            node["totalElements"].asInt() >= 1,
            "totalElements deveria ser >= 1",
        )
        val found = node["content"].any { it["variantId"].asText() == variantId }
        org.junit.jupiter.api.Assertions.assertTrue(found, "a variação criada deveria aparecer no resumo")
    }

    /** Cria categoria + produto + variação e retorna (prodId, variantId). */
    private fun setupProductAndVariant(token: String, suffix: String): Pair<String, String> {
        val catId = postId("/categories", token, """{"name":"Cat $suffix"}""")
        val prodId = postId("/products", token, """{"name":"Prod $suffix","categoryId":"$catId"}""")
        val varJson = mockMvc.perform(
            post("/products/$prodId/variants").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("""{"size":"M","color":"Azul"}"""),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        return prodId to objectMapper.readTree(varJson)["id"].asText()
    }

    @Test
    fun `preco manual NAO e sobrescrito pela entrada de mercadoria (custo atualiza, preco fica)`() {
        val token = managerToken()
        val s = System.nanoTime().toString()
        val (prodId, variantId) = setupProductAndVariant(token, s)
        val supplierId = createSupplier(token, s)

        // 1) Define preço de venda MANUAL de R$ 150 (acima do que a margem sugeriria)
        mockMvc.perform(
            put("/products/$prodId/variants/$variantId").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("""{"salePrice":150.00}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.priceOverride").value(true))
            .andExpect(jsonPath("$.salePrice").value(150.00))

        // 2) Registra entrada de mercadoria: 10 un x custo 30 (margem sugeriria ~60)
        val body = objectMapper.writeValueAsString(
            mapOf(
                "supplierId" to supplierId,
                "receivedAt" to LocalDate.now().toString(),
                "freightCost" to 0.00,
                "items" to listOf(mapOf("variantId" to variantId, "quantity" to 10, "unitCost" to 30.00)),
            ),
        )
        mockMvc.perform(
            post("/supply-lots").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(body),
        ).andExpect(status().isCreated)

        // 3) Confere: custo médio atualizou para 30, mas o preço MANUAL de 150 permaneceu
        val stockJson = mockMvc.perform(
            get("/stock/summary").header("Authorization", token).param("size", "M"),
        ).andExpect(status().isOk).andReturn().response.contentAsString
        val item = objectMapper.readTree(stockJson)["content"].first { it["variantId"].asText() == variantId }
        org.junit.jupiter.api.Assertions.assertEquals(30.0, item["averageCost"].asDouble())
        org.junit.jupiter.api.Assertions.assertEquals(150.0, item["salePrice"].asDouble())
    }

    @Test
    fun `voltar ao sugerido recalcula preco pela margem apos entrada`() {
        val token = managerToken()
        val s = System.nanoTime().toString()
        val (prodId, variantId) = setupProductAndVariant(token, s)
        val supplierId = createSupplier(token, s)

        // entrada: custo 30 -> preço automático 60 (margem global 100%)
        mockMvc.perform(
            post("/supply-lots").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "supplierId" to supplierId,
                            "receivedAt" to LocalDate.now().toString(),
                            "freightCost" to 0.00,
                            "items" to listOf(mapOf("variantId" to variantId, "quantity" to 10, "unitCost" to 30.00)),
                        ),
                    ),
                ),
        ).andExpect(status().isCreated)

        // define preço manual 150
        mockMvc.perform(
            put("/products/$prodId/variants/$variantId").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("""{"salePrice":150.00}"""),
        ).andExpect(status().isOk).andExpect(jsonPath("$.priceOverride").value(true))

        // volta ao sugerido -> preço recalculado pela margem sobre custo 30 = 60
        mockMvc.perform(
            put("/products/$prodId/variants/$variantId").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("""{"resetToSuggested":true}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.priceOverride").value(false))
            .andExpect(jsonPath("$.salePrice").value(60.00))
    }

    @Test
    fun `correcao de custo altera o averageCost da variacao`() {
        val token = managerToken()
        val s = System.nanoTime().toString()
        val (prodId, variantId) = setupProductAndVariant(token, s)
        val supplierId = createSupplier(token, s)

        // entrada define custo 30
        mockMvc.perform(
            post("/supply-lots").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "supplierId" to supplierId,
                            "receivedAt" to LocalDate.now().toString(),
                            "freightCost" to 0.00,
                            "items" to listOf(mapOf("variantId" to variantId, "quantity" to 5, "unitCost" to 30.00)),
                        ),
                    ),
                ),
        ).andExpect(status().isCreated)

        // corrige custo para 42,50
        mockMvc.perform(
            post("/stock/cost-corrections").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"variantId":"$variantId","averageCost":42.50,"notes":"ajuste de custo errado"}"""),
        ).andExpect(status().isCreated)

        // confere no detalhe do produto
        val json = mockMvc.perform(get("/products/$prodId").header("Authorization", token))
            .andExpect(status().isOk).andReturn().response.contentAsString
        val v = objectMapper.readTree(json)["variants"].first { it["id"].asText() == variantId }
        org.junit.jupiter.api.Assertions.assertEquals(42.5, v["averageCost"].asDouble())
    }

    @Test
    fun `correcao de custo sem justificativa retorna 400`() {
        val token = managerToken()
        val s = System.nanoTime().toString()
        val (_, variantId) = setupProductAndVariant(token, s)
        mockMvc.perform(
            post("/stock/cost-corrections").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"variantId":"$variantId","averageCost":42.50,"notes":""}"""),
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `vendedor nao pode corrigir custo`() {
        val stoken = sellerToken()
        mockMvc.perform(
            post("/stock/cost-corrections").header("Authorization", stoken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"variantId":"00000000-0000-0000-0000-000000000000","averageCost":10.00,"notes":"tentativa"}"""),
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `registrar lote sem token retorna 401`() {
        mockMvc.perform(get("/supply-lots")).andExpect(status().isUnauthorized)
    }
}
