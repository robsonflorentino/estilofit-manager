package br.com.estilofitudi.sale.service

import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/**
 * Gera o cronograma de parcelas de uma venda parcelada no cartão de crédito (RN-022 a RN-025).
 *
 * - Vencimentos: D+30, D+60, ... a partir da data da venda
 * - Valor bruto de cada parcela = final_amount / n (última absorve o resíduo de arredondamento)
 * - Valor líquido = bruto × (1 - taxa_maquininha / 100)
 */
@Component
class InstallmentScheduler {

    data class InstallmentPlan(
        val installmentNum: Int,
        val dueDate: LocalDate,
        val grossAmount: BigDecimal,
        val netAmount: BigDecimal,
    )

    fun schedule(
        finalAmount: BigDecimal,
        installments: Int,
        cardFeePct: BigDecimal,
        saleDate: LocalDate,
    ): List<InstallmentPlan> {
        require(installments >= 2) { "Parcelamento requer no mínimo 2 parcelas." }

        val perInstallment = finalAmount.divide(BigDecimal(installments), 2, RoundingMode.HALF_UP)
        val feeMultiplier = BigDecimal.ONE.subtract(cardFeePct.divide(BigDecimal(100)))

        val plans = ArrayList<InstallmentPlan>(installments)
        var allocated = BigDecimal.ZERO

        for (n in 1..installments) {
            val gross = if (n == installments) {
                // última parcela absorve o resíduo para somar exatamente o final_amount
                finalAmount.subtract(allocated).setScale(2, RoundingMode.HALF_UP)
            } else {
                perInstallment
            }
            allocated = allocated.add(gross)

            val net = gross.multiply(feeMultiplier).setScale(2, RoundingMode.HALF_UP)

            plans.add(
                InstallmentPlan(
                    installmentNum = n,
                    dueDate = saleDate.plusDays(30L * n),
                    grossAmount = gross,
                    netAmount = net,
                )
            )
        }
        return plans
    }
}
