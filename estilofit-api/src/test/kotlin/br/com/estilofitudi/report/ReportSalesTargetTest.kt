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
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.math.BigDecimal
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class ReportSalesTargetTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val authHelper: TestAuthHelper,
    private val objectMapper: ObjectMapper,
) : IntegrationTest() {

    private fun managerToken() = authHelper.bearerFor(Role.MANAGER)

    private fun salesTarget(token: String, months: Int): JsonNode {
        val json = mockMvc.perform(
            get("/reports/sales-target").header("Authorization", token).param("months", months.toString()),
        ).andExpect(status().isOk).andReturn().response.contentAsString
        return objectMapper.readTree(json)
    }

    @Test
    fun `retorna a serie mensal com parametros de pro-labore`() {
        val resp = salesTarget(managerToken(), 6)
        assertEquals(6, resp["months"].size())
        // parâmetros default (banco semeado): salário 3000, pró-labore 30%
        assertTrue(resp["targetProLabore"].decimalValue() > BigDecimal.ZERO)
        assertTrue(resp["proLaborePct"].decimalValue() > BigDecimal.ZERO)
        // cada mês tem os campos esperados
        val first = resp["months"][0]
        assertNotNull(first["month"])
        assertNotNull(first["revenue"])
        assertNotNull(first["target"])
        assertNotNull(first["achieved"])
    }

    @Test
    fun `mes sem vendas usa a margem padrao e produz meta deterministica`() {
        // Um mês antigo (5 meses atrás) não terá vendas (confirmedAt é sempre "agora").
        // Margem padrão 100% -> fração de lucro = 100/200 = 0.5; pró-labore 30%; salário 3000
        // meta = 3000 / (0.5 * 0.30) = 20000
        val resp = salesTarget(managerToken(), 6)
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM")
        val oldMonth = YearMonth.now().minusMonths(5).format(fmt)

        val row = resp["months"].first { it["month"].asText() == oldMonth }
        assertEquals(0, BigDecimal.ZERO.compareTo(row["revenue"].decimalValue()), "mês antigo não deve ter faturamento")
        assertEquals(0, BigDecimal("20000.00").compareTo(row["target"].decimalValue()), "meta esperada de 20000")
        assertEquals(false, row["achieved"].asBoolean(), "sem vendas não atinge a meta")
    }

    @Test
    fun `mes corrente reflete faturamento realizado e marca meta atingida quando aplicavel`() {
        val resp = salesTarget(managerToken(), 3)
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM")
        val thisMonth = YearMonth.now().format(fmt)

        val row = resp["months"].first { it["month"].asText() == thisMonth }
        // O mês corrente pode ou não ter vendas de outros testes; garantimos coerência do flag
        val revenue = row["revenue"].decimalValue()
        val target = row["target"].decimalValue()
        val achieved = row["achieved"].asBoolean()
        assertEquals(achieved, revenue >= target && target > BigDecimal.ZERO)
    }

    @Test
    fun `vendedor nao acessa meta de vendas (403)`() {
        val stoken = authHelper.bearerFor(Role.SELLER)
        mockMvc.perform(
            get("/reports/sales-target").header("Authorization", stoken).param("months", "6"),
        ).andExpect(status().isForbidden)
    }
}
