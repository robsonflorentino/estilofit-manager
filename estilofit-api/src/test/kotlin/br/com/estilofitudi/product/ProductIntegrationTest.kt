package br.com.estilofitudi.product

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

class ProductIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val authHelper: TestAuthHelper,
    private val objectMapper: ObjectMapper,
) : IntegrationTest() {

    private fun managerToken() = authHelper.bearerFor(Role.MANAGER)
    private fun sellerToken() = authHelper.bearerFor(Role.SELLER)

    /** Cria uma categoria e retorna o id. */
    private fun createCategory(token: String, name: String): String {
        val body = objectMapper.writeValueAsString(mapOf("name" to name))
        val json = mockMvc.perform(
            post("/categories").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(body),
        ).andReturn().response.contentAsString
        return objectMapper.readTree(json)["id"].asText()
    }

    /** Cria um produto e retorna o id. */
    private fun createProduct(token: String, name: String, categoryId: String): String {
        val body = objectMapper.writeValueAsString(
            mapOf("name" to name, "description" to "Teste", "categoryId" to categoryId),
        )
        val json = mockMvc.perform(
            post("/products").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(body),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        return objectMapper.readTree(json)["id"].asText()
    }

    @Test
    fun `criar produto e variacao gera SKU automatico`() {
        val token = managerToken()
        val catId = createCategory(token, "Blusas Teste ${System.nanoTime()}")
        val prodId = createProduct(token, "Blusa Listrada", catId)

        val variantBody = objectMapper.writeValueAsString(mapOf("size" to "M", "color" to "Azul"))
        mockMvc.perform(
            post("/products/$prodId/variants").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(variantBody),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.sku").isNotEmpty)
            .andExpect(jsonPath("$.size").value("M"))
            .andExpect(jsonPath("$.color").value("Azul"))
            .andExpect(jsonPath("$.stockQuantity").value(0))
    }

    @Test
    fun `variacao duplicada (tamanho+cor) retorna 409`() {
        val token = managerToken()
        val catId = createCategory(token, "Calcas Teste ${System.nanoTime()}")
        val prodId = createProduct(token, "Calça Legging", catId)

        val body = objectMapper.writeValueAsString(mapOf("size" to "P", "color" to "Preto"))
        mockMvc.perform(
            post("/products/$prodId/variants").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(body),
        ).andExpect(status().isCreated)

        // mesma combinação, case-insensitive
        val body2 = objectMapper.writeValueAsString(mapOf("size" to "P", "color" to "preto"))
        mockMvc.perform(
            post("/products/$prodId/variants").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(body2),
        ).andExpect(status().isConflict)
    }

    @Test
    fun `criar produto em categoria inativa retorna 422`() {
        val token = managerToken()
        val catId = createCategory(token, "Inativa Teste ${System.nanoTime()}")
        // desativa a categoria
        mockMvc.perform(
            patch("/categories/$catId/status").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"),
        ).andExpect(status().isOk)

        val body = objectMapper.writeValueAsString(
            mapOf("name" to "Produto X", "categoryId" to catId),
        )
        mockMvc.perform(
            post("/products").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(body),
        ).andExpect(status().isUnprocessableEntity)
    }

    @Test
    fun `criar variacao em produto inativo retorna 422`() {
        val token = managerToken()
        val catId = createCategory(token, "CatV Teste ${System.nanoTime()}")
        val prodId = createProduct(token, "Produto Inativo", catId)
        mockMvc.perform(
            patch("/products/$prodId/status").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"),
        ).andExpect(status().isOk)

        val body = objectMapper.writeValueAsString(mapOf("size" to "M", "color" to "Verde"))
        mockMvc.perform(
            post("/products/$prodId/variants").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(body),
        ).andExpect(status().isUnprocessableEntity)
    }

    @Test
    fun `listagem reflete variantCount e totalStock`() {
        val token = managerToken()
        val catId = createCategory(token, "CatCount Teste ${System.nanoTime()}")
        val prodId = createProduct(token, "Produto Com Variacoes ${System.nanoTime()}", catId)

        // adiciona duas variações
        listOf("""{"size":"M","color":"Azul"}""", """{"size":"G","color":"Rosa"}""").forEach { body ->
            mockMvc.perform(
                post("/products/$prodId/variants").header("Authorization", token)
                    .contentType(MediaType.APPLICATION_JSON).content(body),
            ).andExpect(status().isCreated)
        }

        // busca o produto na listagem e confere o variantCount
        val json = mockMvc.perform(
            get("/products").header("Authorization", token)
                .param("name", "Produto Com Variacoes"),
        ).andExpect(status().isOk).andReturn().response.contentAsString

        val node = objectMapper.readTree(json)
        val product = node["content"].first { it["id"].asText() == prodId }
        org.junit.jupiter.api.Assertions.assertEquals(2, product["variantCount"].asInt())
    }

    @Test
    fun `listar produtos exige autenticacao`() {
        mockMvc.perform(get("/products")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `vendedor pode listar mas nao criar produto`() {
        val token = sellerToken()
        mockMvc.perform(get("/products").header("Authorization", token))
            .andExpect(status().isOk)

        val body = objectMapper.writeValueAsString(
            mapOf("name" to "Proibido", "categoryId" to UUID_ANY),
        )
        mockMvc.perform(
            post("/products").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(body),
        ).andExpect(status().isForbidden)
    }

    companion object {
        const val UUID_ANY = "00000000-0000-0000-0000-000000000000"
    }
}
