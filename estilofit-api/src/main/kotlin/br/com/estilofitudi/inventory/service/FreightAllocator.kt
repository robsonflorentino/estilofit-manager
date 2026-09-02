package br.com.estilofitudi.inventory.service

import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Rateia o frete de um lote entre seus itens, proporcionalmente ao valor de custo
 * de cada item (RN-009).
 *
 *   participacao   = (unit_cost × quantity) / custo_total_sem_frete
 *   freight_share  = freight_total × participacao
 *   real_unit_cost = unit_cost + (freight_share / quantity)
 *
 * A última fatia de frete recebe o ajuste de arredondamento para que a soma dos
 * freight_share seja exatamente igual ao frete total (evita centavos perdidos).
 */
@Component
class FreightAllocator {

    data class Input(val unitCost: BigDecimal, val quantity: Int)

    data class Allocation(
        val freightShare: BigDecimal,   // frete rateado total do item
        val realUnitCost: BigDecimal,   // custo unitário + frete rateado por unidade
    )

    fun allocate(items: List<Input>, freightTotal: BigDecimal): List<Allocation> {
        require(items.isNotEmpty()) { "O lote deve ter ao menos um item." }

        val totalCost = items.fold(BigDecimal.ZERO) { acc, it ->
            acc + it.unitCost.multiply(BigDecimal(it.quantity))
        }
        require(totalCost > BigDecimal.ZERO) { "O custo total do lote deve ser maior que zero." }

        val allocations = ArrayList<Allocation>(items.size)
        var allocatedFreight = BigDecimal.ZERO

        items.forEachIndexed { index, item ->
            val itemCost = item.unitCost.multiply(BigDecimal(item.quantity))

            val share: BigDecimal = if (index == items.lastIndex) {
                // último item absorve o resíduo de arredondamento
                freightTotal.subtract(allocatedFreight).setScale(2, RoundingMode.HALF_UP)
            } else {
                freightTotal
                    .multiply(itemCost)
                    .divide(totalCost, 2, RoundingMode.HALF_UP)
            }
            allocatedFreight = allocatedFreight.add(share)

            val freightPerUnit = share.divide(BigDecimal(item.quantity), 2, RoundingMode.HALF_UP)
            val realUnit = item.unitCost.add(freightPerUnit).setScale(2, RoundingMode.HALF_UP)

            allocations.add(Allocation(freightShare = share, realUnitCost = realUnit))
        }

        return allocations
    }
}
