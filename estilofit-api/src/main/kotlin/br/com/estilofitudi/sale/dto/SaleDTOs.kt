package br.com.estilofitudi.sale.dto

import br.com.estilofitudi.sale.domain.FreightType
import br.com.estilofitudi.sale.domain.PaymentMethod
import br.com.estilofitudi.sale.domain.Sale
import br.com.estilofitudi.sale.domain.SaleStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

// ── Requests ─────────────────────────────────────────────────────────────────

data class CreateSaleRequest(
    @field:NotNull(message = "Canal de venda é obrigatório")
    val channelId: UUID,

    @field:NotNull(message = "Forma de pagamento é obrigatória")
    val paymentMethod: PaymentMethod,

    @field:Min(value = 1, message = "Parcelas deve ser no mínimo 1")
    val installments: Int = 1,

    val cardFeePct: BigDecimal? = null,

    val cardFeePassed: Boolean = true,

    @field:DecimalMin(value = "0.0", message = "Desconto não pode ser negativo")
    val discountAmount: BigDecimal = BigDecimal.ZERO,

    val freightType: FreightType = FreightType.NONE,

    @field:DecimalMin(value = "0.0", message = "Frete não pode ser negativo")
    val freightAmount: BigDecimal = BigDecimal.ZERO,

    val notes: String? = null,

    @field:NotEmpty(message = "A venda deve ter ao menos um item")
    @field:Valid
    val items: List<CreateSaleItemRequest>,
)

data class CreateSaleItemRequest(
    @field:NotNull(message = "Variação é obrigatória")
    val variantId: UUID,

    @field:Min(value = 1, message = "Quantidade deve ser no mínimo 1")
    val quantity: Int,
)

data class CancelSaleRequest(
    @field:jakarta.validation.constraints.Size(min = 5, message = "Motivo é obrigatório (mínimo 5 caracteres)")
    val reason: String,
)

// ── Responses ────────────────────────────────────────────────────────────────

data class ChannelRef(val id: UUID, val name: String)
data class SellerRef(val id: UUID, val name: String)

data class SaleItemResponse(
    val id: UUID,
    val variant: SaleVariantRef,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val totalPrice: BigDecimal,
)

data class SaleVariantRef(
    val id: UUID,
    val sku: String,
    val productName: String,
    val size: String,
    val color: String,
)

data class SaleSummaryResponse(
    val id: UUID,
    val channel: ChannelRef,
    val seller: SellerRef,
    val confirmedAt: LocalDateTime,
    val totalAmount: BigDecimal,
    val discountAmount: BigDecimal,
    val finalAmount: BigDecimal,
    val paymentMethod: PaymentMethod,
    val installments: Int,
    val status: SaleStatus,
    val itemCount: Int,
)

data class SaleDetailResponse(
    val id: UUID,
    val channel: ChannelRef,
    val seller: SellerRef,
    val confirmedAt: LocalDateTime,
    val totalAmount: BigDecimal,
    val discountAmount: BigDecimal,
    val finalAmount: BigDecimal,
    val paymentMethod: PaymentMethod,
    val installments: Int,
    val cardFeePct: BigDecimal?,
    val cardFeePassed: Boolean,
    val commissionPct: BigDecimal,
    val commissionAmount: BigDecimal,
    val freightType: FreightType,
    val freightAmount: BigDecimal,
    val totalPaid: BigDecimal,          // finalAmount (produtos) + frete — o que o cliente paga
    val status: SaleStatus,
    val notes: String?,
    val items: List<SaleItemResponse>,
    val installmentSchedule: List<InstallmentResponse>,
    val cancelledAt: LocalDateTime?,
)

fun Sale.toSummaryResponse() = SaleSummaryResponse(
    id = id,
    channel = ChannelRef(channel.id, channel.name),
    seller = SellerRef(seller.id, seller.name),
    confirmedAt = confirmedAt,
    totalAmount = totalAmount,
    discountAmount = discountAmount,
    finalAmount = finalAmount,
    paymentMethod = paymentMethod,
    installments = installments,
    status = status,
    itemCount = items.size,
)

fun Sale.toDetailResponse() = SaleDetailResponse(
    id = id,
    channel = ChannelRef(channel.id, channel.name),
    seller = SellerRef(seller.id, seller.name),
    confirmedAt = confirmedAt,
    totalAmount = totalAmount,
    discountAmount = discountAmount,
    finalAmount = finalAmount,
    paymentMethod = paymentMethod,
    installments = installments,
    cardFeePct = cardFeePct,
    cardFeePassed = cardFeePassed,
    commissionPct = commissionPct,
    commissionAmount = commissionAmount,
    freightType = freightType,
    freightAmount = freightAmount,
    totalPaid = finalAmount.add(freightAmount),
    status = status,
    notes = notes,
    items = items.map {
        SaleItemResponse(
            id = it.id,
            variant = SaleVariantRef(
                it.variant.id, it.variant.sku, it.variant.product.name, it.variant.size, it.variant.color,
            ),
            quantity = it.quantity,
            unitPrice = it.unitPrice,
            totalPrice = it.totalPrice,
        )
    },
    installmentSchedule = installmentSchedule
        .sortedBy { it.installmentNum }
        .map { it.toResponse() },
    cancelledAt = cancelledAt,
)
