package br.com.estilofitudi.report.dto

import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

/** Resumo do período. */
data class ReportSummaryResponse(
    val revenue: BigDecimal,       // faturamento (finalAmount de vendas confirmadas)
    val saleCount: Long,           // número de vendas
    val averageTicket: BigDecimal, // faturamento / número de vendas
    val estimatedProfit: BigDecimal, // faturamento - custo das mercadorias vendidas
)

/** Ponto da série de faturamento por dia. */
data class DailyRevenueResponse(
    val day: LocalDate,
    val revenue: BigDecimal,
    val saleCount: Long,
)

/** Linha do ranking de produtos mais vendidos. */
data class TopProductResponse(
    val variantId: UUID,
    val sku: String,
    val productName: String,
    val size: String,
    val color: String,
    val quantity: Long,
    val revenue: BigDecimal,
)

/** Fatia de faturamento agrupada (por canal ou forma de pagamento). */
data class RevenueSliceResponse(
    val label: String,
    val revenue: BigDecimal,
    val saleCount: Long,
    val percentage: BigDecimal, // participação no faturamento total do período (0-100)
)

/** Um mês do relatório de meta de vendas para pró-labore. */
data class SalesTargetMonthResponse(
    val month: String,          // "yyyy-MM"
    val revenue: BigDecimal,    // faturamento realizado
    val target: BigDecimal,     // faturamento necessário para o pró-labore desejado
    val profitMarginPct: BigDecimal, // margem de lucro usada no cálculo (%)
    val achieved: Boolean,      // realizado >= meta
)

/** Relatório de meta de vendas: parâmetros usados + série mensal. */
data class SalesTargetResponse(
    val targetProLabore: BigDecimal, // salário desejado (R$/mês)
    val proLaborePct: BigDecimal,    // % do lucro destinado ao pró-labore
    val months: List<SalesTargetMonthResponse>,
)

/** Lucratividade de um canal de venda no período. */
data class ChannelProfitResponse(
    val channel: String,
    val revenue: BigDecimal,   // faturamento
    val cost: BigDecimal,      // custo das mercadorias vendidas
    val profit: BigDecimal,    // lucro = faturamento - custo
    val marginPct: BigDecimal, // margem = lucro / faturamento × 100
    val saleCount: Long,
)
