package br.com.estilofitudi.sale.dto

import br.com.estilofitudi.sale.domain.InstallmentStatus
import br.com.estilofitudi.sale.domain.SaleInstallment
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

// ── Responses ────────────────────────────────────────────────────────────────

data class InstallmentResponse(
    val id: UUID,
    val installmentNum: Int,
    val dueDate: LocalDate,
    val grossAmount: BigDecimal,
    val netAmount: BigDecimal,
    val status: InstallmentStatus,
    val receivedAt: LocalDateTime?,
)

/** Parcela com dados resumidos da venda — para a listagem de contas a receber. */
data class InstallmentWithSaleResponse(
    val id: UUID,
    val installmentNum: Int,
    val dueDate: LocalDate,
    val grossAmount: BigDecimal,
    val netAmount: BigDecimal,
    val status: InstallmentStatus,
    val receivedAt: LocalDateTime?,
    val sale: InstallmentSaleRef,
)

data class InstallmentSaleRef(
    val id: UUID,
    val confirmedAt: LocalDateTime,
    val finalAmount: BigDecimal,
)

fun SaleInstallment.toResponse() = InstallmentResponse(
    id = id,
    installmentNum = installmentNum,
    dueDate = dueDate,
    grossAmount = grossAmount,
    netAmount = netAmount,
    status = status,
    receivedAt = receivedAt,
)

fun SaleInstallment.toWithSaleResponse() = InstallmentWithSaleResponse(
    id = id,
    installmentNum = installmentNum,
    dueDate = dueDate,
    grossAmount = grossAmount,
    netAmount = netAmount,
    status = status,
    receivedAt = receivedAt,
    sale = InstallmentSaleRef(sale.id, sale.confirmedAt, sale.finalAmount),
)

// ── Fluxo projetado ──────────────────────────────────────────────────────────

data class ProjectedMonthResponse(
    val month: String,          // ex: "2026-10"
    val totalGross: BigDecimal,
    val totalNet: BigDecimal,
    val installments: List<InstallmentWithSaleResponse>,
)

// ── Requests ─────────────────────────────────────────────────────────────────

data class ReceiveInstallmentRequest(
    val receivedAt: LocalDate? = null, // default: hoje
)
