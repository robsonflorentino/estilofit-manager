package br.com.estilofitudi.supplier

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

class SupplierIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val authHelper: TestAuthHelper,
    private val objectMapper: ObjectMapper,
) : IntegrationTest() {

    private fun managerToken() = authHelper.bearerFor(Role.MANAGER)
    private fun sellerToken() = authHelper.bearerFor(Role.SELLER)

    @Test
    fun `sem token retorna 401`() {
        mockMvc.perform(get("/suppliers")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `vendedor nao acessa fornecedores (403)`() {
        mockMvc.perform(get("/suppliers").header("Authorization", sellerToken()))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `gestor lista fornecedores (paginado)`() {
        mockMvc.perform(get("/suppliers").header("Authorization", managerToken()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.page").value(0))
    }

    @Test
    fun `criar fornecedor retorna 201`() {
        val body = objectMapper.writeValueAsString(
            mapOf(
                "name" to "Moda Brasil LTDA",
                "contactPhone" to "(11) 99999-0000",
                "contactEmail" to "contato@modabrasil.com.br",
                "cnpj" to "12.345.678/0001-${(10..99).random()}",
            ),
        )
        mockMvc.perform(
            post("/suppliers").header("Authorization", managerToken())
                .contentType(MediaType.APPLICATION_JSON).content(body),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Moda Brasil LTDA"))
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.id").isNotEmpty)
    }

    @Test
    fun `criar com CNPJ duplicado retorna 409`() {
        val cnpj = "98.765.432/0001-${(10..99).random()}"
        val body = objectMapper.writeValueAsString(mapOf("name" to "Fornecedor A", "cnpj" to cnpj))
        val token = managerToken()

        mockMvc.perform(
            post("/suppliers").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(body),
        ).andExpect(status().isCreated)

        val body2 = objectMapper.writeValueAsString(mapOf("name" to "Fornecedor B", "cnpj" to cnpj))
        mockMvc.perform(
            post("/suppliers").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(body2),
        ).andExpect(status().isConflict)
    }

    @Test
    fun `criar com nome invalido retorna 400`() {
        val body = objectMapper.writeValueAsString(mapOf("name" to ""))
        mockMvc.perform(
            post("/suppliers").header("Authorization", managerToken())
                .contentType(MediaType.APPLICATION_JSON).content(body),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.fieldErrors").isArray)
    }

    @Test
    fun `atualizar e desativar fornecedor`() {
        val token = managerToken()
        val createBody = objectMapper.writeValueAsString(mapOf("name" to "Fornecedor Editar"))
        val created = mockMvc.perform(
            post("/suppliers").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(createBody),
        ).andReturn().response.contentAsString
        val id = objectMapper.readTree(created)["id"].asText()

        // atualizar
        val updateBody = objectMapper.writeValueAsString(
            mapOf("name" to "Fornecedor Renomeado", "contactPhone" to "(11) 98888-7777"),
        )
        mockMvc.perform(
            put("/suppliers/$id").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(updateBody),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Fornecedor Renomeado"))
            .andExpect(jsonPath("$.contactPhone").value("(11) 98888-7777"))

        // desativar
        mockMvc.perform(
            patch("/suppliers/$id/status").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.active").value(false))
    }
}
