package br.com.estilofitudi.category

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

class CategoryIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val authHelper: TestAuthHelper,
    private val objectMapper: ObjectMapper,
) : IntegrationTest() {

    private fun managerToken() = authHelper.bearerFor(Role.MANAGER)
    private fun sellerToken() = authHelper.bearerFor(Role.SELLER)

    @Test
    fun `GET categories retorna as categorias seed sem autenticacao deve falhar`() {
        mockMvc.perform(get("/categories"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET categories autenticado retorna lista com as categorias seed`() {
        mockMvc.perform(get("/categories").header("Authorization", sellerToken()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[?(@.name == 'Blusas')]").exists())
    }

    @Test
    fun `POST categories como gestor cria categoria`() {
        val body = objectMapper.writeValueAsString(mapOf("name" to "Categoria Teste A"))

        mockMvc.perform(
            post("/categories")
                .header("Authorization", managerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Categoria Teste A"))
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.id").isNotEmpty)
    }

    @Test
    fun `POST categories duplicada retorna 409`() {
        val body = objectMapper.writeValueAsString(mapOf("name" to "Categoria Duplicada"))
        val token = managerToken()

        mockMvc.perform(
            post("/categories").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(body)
        ).andExpect(status().isCreated)

        // segunda vez, case-insensitive
        val body2 = objectMapper.writeValueAsString(mapOf("name" to "categoria duplicada"))
        mockMvc.perform(
            post("/categories").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(body2)
        ).andExpect(status().isConflict)
    }

    @Test
    fun `POST categories com nome invalido retorna 400 com fieldErrors`() {
        val body = objectMapper.writeValueAsString(mapOf("name" to ""))

        mockMvc.perform(
            post("/categories").header("Authorization", managerToken())
                .contentType(MediaType.APPLICATION_JSON).content(body)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.fieldErrors").isArray)
            .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
    }

    @Test
    fun `POST categories como vendedor retorna 403`() {
        val body = objectMapper.writeValueAsString(mapOf("name" to "Proibida"))

        mockMvc.perform(
            post("/categories").header("Authorization", sellerToken())
                .contentType(MediaType.APPLICATION_JSON).content(body)
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `PUT categories renomeia categoria`() {
        val token = managerToken()
        val createBody = objectMapper.writeValueAsString(mapOf("name" to "Renomear Origem"))
        val created = mockMvc.perform(
            post("/categories").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(createBody)
        ).andReturn().response.contentAsString
        val id = objectMapper.readTree(created)["id"].asText()

        val renameBody = objectMapper.writeValueAsString(mapOf("name" to "Renomear Destino"))
        mockMvc.perform(
            put("/categories/$id").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(renameBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Renomear Destino"))
    }

    @Test
    fun `PATCH status desativa categoria e some da listagem padrao`() {
        val token = managerToken()
        val createBody = objectMapper.writeValueAsString(mapOf("name" to "Para Desativar"))
        val created = mockMvc.perform(
            post("/categories").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(createBody)
        ).andReturn().response.contentAsString
        val id = objectMapper.readTree(created)["id"].asText()

        val statusBody = objectMapper.writeValueAsString(mapOf("active" to false))
        mockMvc.perform(
            patch("/categories/$id/status").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(statusBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.active").value(false))

        // listagem padrao (onlyActive=true) nao traz a desativada
        mockMvc.perform(get("/categories").header("Authorization", token))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.name == 'Para Desativar')]").doesNotExist())

        // onlyActive=false traz a desativada
        mockMvc.perform(get("/categories?onlyActive=false").header("Authorization", token))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.name == 'Para Desativar')]").exists())
    }
}
