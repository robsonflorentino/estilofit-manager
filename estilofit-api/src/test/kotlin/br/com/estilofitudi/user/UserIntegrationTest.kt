package br.com.estilofitudi.user

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
import java.util.*

class UserIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val authHelper: TestAuthHelper,
    private val objectMapper: ObjectMapper,
) : IntegrationTest() {

    private fun adminToken() = authHelper.bearerFor(Role.ADMIN)
    private fun sellerToken() = authHelper.bearerFor(Role.SELLER)

    private fun createUserBody(email: String, role: String = "SELLER") =
        objectMapper.writeValueAsString(
            mapOf(
                "name" to "Novo Usuario",
                "email" to email,
                "password" to "senha12345",
                "role" to role,
            )
        )

    @Test
    fun `GET users sem token retorna 401`() {
        mockMvc.perform(get("/users"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET users como admin retorna pagina`() {
        mockMvc.perform(get("/users").header("Authorization", adminToken()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.totalElements").isNumber)
    }

    @Test
    fun `GET users como vendedor retorna 403`() {
        mockMvc.perform(get("/users").header("Authorization", sellerToken()))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `POST users como admin cria usuario`() {
        val email = "criado-${UUID.randomUUID()}@estilofit.com.br"

        mockMvc.perform(
            post("/users").header("Authorization", adminToken())
                .contentType(MediaType.APPLICATION_JSON).content(createUserBody(email))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.role").value("SELLER"))
            .andExpect(jsonPath("$.active").value(true))
    }

    @Test
    fun `POST users com email duplicado retorna 409`() {
        val email = "dup-${UUID.randomUUID()}@estilofit.com.br"
        val token = adminToken()

        mockMvc.perform(
            post("/users").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(createUserBody(email))
        ).andExpect(status().isCreated)

        mockMvc.perform(
            post("/users").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(createUserBody(email))
        ).andExpect(status().isConflict)
    }

    @Test
    fun `POST users com dados invalidos retorna 400 com fieldErrors`() {
        val body = objectMapper.writeValueAsString(
            mapOf("name" to "", "email" to "invalido", "password" to "123", "role" to "SELLER")
        )

        mockMvc.perform(
            post("/users").header("Authorization", adminToken())
                .contentType(MediaType.APPLICATION_JSON).content(body)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.fieldErrors").isArray)
    }

    @Test
    fun `POST users como vendedor retorna 403`() {
        val email = "proibido-${UUID.randomUUID()}@estilofit.com.br"

        mockMvc.perform(
            post("/users").header("Authorization", sellerToken())
                .contentType(MediaType.APPLICATION_JSON).content(createUserBody(email))
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `GET users me retorna dados do usuario logado`() {
        val seller = authHelper.createUser(Role.SELLER)
        val token = "Bearer ${authHelper.tokenFor(seller)}"

        mockMvc.perform(get("/users/me").header("Authorization", token))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value(seller.email))
            .andExpect(jsonPath("$.role").value("SELLER"))
    }
}
