package br.com.estilofitudi.sale.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate

class InstallmentSchedulerTest {

    private val scheduler = InstallmentScheduler()

    private fun bd(v: String) = BigDecimal(v)
    private val saleDate = LocalDate.of(2026, 1, 15)

    @Test
    fun `gera n parcelas com vencimentos D+30, D+60, D+90`() {
        val plans = scheduler.schedule(bd("300.00"), 3, bd("0.00"), saleDate)

        assertEquals(3, plans.size)
        assertEquals(LocalDate.of(2026, 2, 14), plans[0].dueDate) // +30
        assertEquals(LocalDate.of(2026, 3, 16), plans[1].dueDate) // +60
        assertEquals(LocalDate.of(2026, 4, 15), plans[2].dueDate) // +90
        assertEquals(1, plans[0].installmentNum)
        assertEquals(3, plans[2].installmentNum)
    }

    @Test
    fun `soma dos brutos e exatamente o valor final (residuo na ultima parcela)`() {
        // 100 / 3 = 33.33, 33.33, 33.34
        val plans = scheduler.schedule(bd("100.00"), 3, bd("0.00"), saleDate)

        assertEquals(0, bd("33.33").compareTo(plans[0].grossAmount))
        assertEquals(0, bd("33.33").compareTo(plans[1].grossAmount))
        assertEquals(0, bd("33.34").compareTo(plans[2].grossAmount))

        val soma = plans.fold(BigDecimal.ZERO) { acc, p -> acc + p.grossAmount }
        assertEquals(0, bd("100.00").compareTo(soma))
    }

    @Test
    fun `valor liquido desconta a taxa da maquininha`() {
        // 300 em 3x, taxa 10% -> bruto 100/parcela, liquido 90
        val plans = scheduler.schedule(bd("300.00"), 3, bd("10.00"), saleDate)

        plans.forEach { assertEquals(0, bd("100.00").compareTo(it.grossAmount)) }
        plans.forEach { assertEquals(0, bd("90.00").compareTo(it.netAmount)) }
    }

    @Test
    fun `taxa zero mantem liquido igual ao bruto`() {
        val plans = scheduler.schedule(bd("200.00"), 2, bd("0.00"), saleDate)
        plans.forEach { assertEquals(0, it.grossAmount.compareTo(it.netAmount)) }
    }

    @Test
    fun `lanca erro com menos de 2 parcelas`() {
        assertThrows<IllegalArgumentException> {
            scheduler.schedule(bd("100.00"), 1, bd("0.00"), saleDate)
        }
    }
}
