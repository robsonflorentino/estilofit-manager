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
