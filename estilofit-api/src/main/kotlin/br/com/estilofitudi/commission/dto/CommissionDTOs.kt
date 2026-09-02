package br.com.estilofitudi.commission.dto

import java.math.BigDecimal
import java.util.*

/** Comissão a pagar a um vendedor no período. */
data class SellerCommissionResponse(
    val sellerId: UUID,
    val sellerName: String,
    val revenue: BigDecimal,          // faturamento do vendedor no período
    val commissionAmount: BigDecimal, // total de comissão a pagar
    val saleCount: Long,
)

/** Relatório de comissões: lista por vendedor + total geral. */
data class CommissionReportResponse(
    val totalCommission: BigDecimal,
    val sellers: List<SellerCommissionResponse>,
)
