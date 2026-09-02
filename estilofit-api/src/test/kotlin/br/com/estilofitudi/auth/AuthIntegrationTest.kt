package br.com.estilofitudi.auth

import br.com.estilofitudi.shared.config.DataInitializer
import br.com.estilofitudi.support.IntegrationTest
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

class AuthIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) : IntegrationTest() {

    private fun loginBody(email: String, password: String) =
        objectMapper.writeValueAsString(mapOf("email" to email, "password" to password))

    @Test
    fun `login com admin padrao retorna 200 e accessToken`() {
        val body = loginBody(DataInitializer.DEFAULT_ADMIN_EMAIL, DataInitializer.DEFAULT_ADMIN_PASSWORD)

        mockMvc.perform(
            post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").isNotEmpty)
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.user.role").value("ADMIN"))
            .andExpect(jsonPath("$.user.email").value(DataInitializer.DEFAULT_ADMIN_EMAIL))
    }

    @Test
    fun `login com senha incorreta retorna 422`() {
        val body = loginBody(DataInitializer.DEFAULT_ADMIN_EMAIL, "senha-errada")

        mockMvc.perform(
            post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body)
        ).andExpect(status().isUnprocessableEntity)
    }

    @Test
    fun `login com email inexistente retorna 422`() {
        val body = loginBody("naoexiste@estilofit.com.br", "qualquer123")

        mockMvc.perform(
            post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body)
        ).andExpect(status().isUnprocessableEntity)
    }

    @Test
    fun `login com email invalido retorna 400`() {
        val body = loginBody("email-sem-arroba", "qualquer123")

        mockMvc.perform(
            post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body)
        ).andExpect(status().isBadRequest)
    }
}
