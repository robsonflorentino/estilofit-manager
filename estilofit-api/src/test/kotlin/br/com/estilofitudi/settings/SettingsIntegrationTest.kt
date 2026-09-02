package br.com.estilofitudi.settings

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

class SettingsIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val authHelper: TestAuthHelper,
    private val objectMapper: ObjectMapper,
) : IntegrationTest() {

    private fun adminToken() = authHelper.bearerFor(Role.ADMIN)

    private fun list(token: String): JsonNode {
        val json = mockMvc.perform(get("/settings").header("Authorization", token))
            .andExpect(status().isOk).andReturn().response.contentAsString
        return objectMapper.readTree(json)
    }

    private fun valueOf(list: JsonNode, key: String): String =
        list.first { it["key"].asText() == key }["value"].asText()

    private fun put(token: String, key: String, value: String) =
        mockMvc.perform(
            put("/settings/$key").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("""{"value":"$value"}"""),
        )

    @Test
    fun `admin lista as quatro configuracoes conhecidas`() {
        val keys = list(adminToken()).map { it["key"].asText() }.toSet()
        assertTrue(
            keys.containsAll(
                setOf("DEFAULT_PROFIT_MARGIN", "LOW_STOCK_THRESHOLD", "PRO_LABORE_PCT", "PROMOTION_ALERT_DAYS"),
            ),
            "deveria listar as 4 chaves conhecidas, veio: $keys",
        )
        // cada item traz metadados de edição
        val margin = list(adminToken()).first { it["key"].asText() == "DEFAULT_PROFIT_MARGIN" }
        assertNotNull(margin["label"])
        assertEquals("DECIMAL", margin["type"].asText())
    }

    @Test
    fun `admin atualiza uma configuracao e o novo valor persiste`() {
        val token = adminToken()
        // usa PROMOTION_ALERT_DAYS (os testes de promoção passam days explícito, não dependem do default)
        put(token, "PROMOTION_ALERT_DAYS", "45")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.value").value("45"))

        assertEquals("45", valueOf(list(token), "PROMOTION_ALERT_DAYS"))

        // registra quem atualizou
        val updated = list(token).first { it["key"].asText() == "PROMOTION_ALERT_DAYS" }
        assertNotNull(updated["updatedByName"].takeIf { !it.isNull })
    }

    @Test
    fun `valor inteiro com casas decimais e rejeitado (422)`() {
        put(adminToken(), "LOW_STOCK_THRESHOLD", "2.5")
            .andExpect(status().isUnprocessableEntity)
    }

    @Test
    fun `percentual de pro-labore acima de 100 e rejeitado (422)`() {
        put(adminToken(), "PRO_LABORE_PCT", "150")
            .andExpect(status().isUnprocessableEntity)
    }

    @Test
    fun `valor nao numerico e rejeitado (422)`() {
        put(adminToken(), "DEFAULT_PROFIT_MARGIN", "abc")
            .andExpect(status().isUnprocessableEntity)
    }

    @Test
    fun `chave desconhecida retorna 404`() {
        put(adminToken(), "CHAVE_INEXISTENTE", "10")
            .andExpect(status().isNotFound)
    }

    @Test
    fun `gestor nao acessa configuracoes (403)`() {
        val mtoken = authHelper.bearerFor(Role.MANAGER)
        mockMvc.perform(get("/settings").header("Authorization", mtoken))
            .andExpect(status().isForbidden)
        put(mtoken, "PROMOTION_ALERT_DAYS", "30")
            .andExpect(status().isForbidden)
    }

    @Test
    fun `sem token retorna 401`() {
        mockMvc.perform(get("/settings")).andExpect(status().isUnauthorized)
    }
}
