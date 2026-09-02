package br.com.estilofitudi.inventory.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class FreightAllocatorTest {

    private val allocator = FreightAllocator()

    private fun bd(v: String) = BigDecimal(v)

    @Test
    fun `rateia frete conforme exemplo da RN-009`() {
        // Item A: 10 x 30 = 300 (50%), Item B: 5 x 60 = 300 (50%), frete 200
        val result = allocator.allocate(
            items = listOf(
                FreightAllocator.Input(bd("30.00"), 10),
                FreightAllocator.Input(bd("60.00"), 5),
            ),
            freightTotal = bd("200.00"),
        )

        // Item A: frete 100 -> 10/un -> custo real 40
        assertEquals(0, bd("100.00").compareTo(result[0].freightShare))
        assertEquals(0, bd("40.00").compareTo(result[0].realUnitCost))
        // Item B: frete 100 -> 20/un -> custo real 80
        assertEquals(0, bd("100.00").compareTo(result[1].freightShare))
        assertEquals(0, bd("80.00").compareTo(result[1].realUnitCost))
    }

    @Test
    fun `frete zero mantem custo unitario`() {
        val result = allocator.allocate(
            items = listOf(FreightAllocator.Input(bd("50.00"), 4)),
            freightTotal = BigDecimal.ZERO,
        )
        assertEquals(0, BigDecimal.ZERO.compareTo(result[0].freightShare))
        assertEquals(0, bd("50.00").compareTo(result[0].realUnitCost))
    }

    @Test
    fun `soma dos freteShares e exatamente o frete total (residuo no ultimo item)`() {
        // 3 itens com custos que geram dizima na divisao
        val result = allocator.allocate(
            items = listOf(
                FreightAllocator.Input(bd("10.00"), 1),
                FreightAllocator.Input(bd("10.00"), 1),
                FreightAllocator.Input(bd("10.00"), 1),
            ),
            freightTotal = bd("100.00"),
        )
        val soma = result.fold(BigDecimal.ZERO) { acc, a -> acc + a.freightShare }
        assertEquals(0, bd("100.00").compareTo(soma))
    }

    @Test
    fun `lanca erro quando itens vazios`() {
        assertThrows<IllegalArgumentException> {
            allocator.allocate(emptyList(), bd("100.00"))
        }
    }

    @Test
    fun `lanca erro quando custo total e zero`() {
        assertThrows<IllegalArgumentException> {
            allocator.allocate(listOf(FreightAllocator.Input(BigDecimal.ZERO, 5)), bd("100.00"))
        }
    }
}
