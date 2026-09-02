package br.com.estilofitudi.inventory.dto

import br.com.estilofitudi.inventory.domain.SupplyLot
import br.com.estilofitudi.supplier.dto.SupplierSummary
import br.com.estilofitudi.supplier.dto.toSummary
import br.com.estilofitudi.user.dto.UserSummary
import br.com.estilofitudi.user.dto.toSummary
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

// ── Requests ─────────────────────────────────────────────────────────────────

data class CreateSupplyLotRequest(
    @field:NotNull(message = "Fornecedor é obrigatório")
    val supplierId: UUID,

    @field:NotNull(message = "Data de recebimento é obrigatória")
    val receivedAt: LocalDate,

    @field:NotNull @field:DecimalMin(value = "0.0", message = "Frete não pode ser negativo")
    val freightCost: BigDecimal = BigDecimal.ZERO,

    val notes: String? = null,

    @field:NotEmpty(message = "O lote deve ter ao menos um item")
    @field:Valid
    val items: List<CreateSupplyLotItemRequest>,
)

data class CreateSupplyLotItemRequest(
    @field:NotNull(message = "Variação é obrigatória")
    val variantId: UUID,

    @field:Min(value = 1, message = "Quantidade deve ser no mínimo 1")
    val quantity: Int,

    @field:DecimalMin(value = "0.01", message = "Custo unitário deve ser positivo")
    val unitCost: BigDecimal,
)

// ── Responses ────────────────────────────────────────────────────────────────

data class SupplyLotItemResponse(
    val id: UUID,
    val variant: VariantRef,
    val quantity: Int,
    val unitCost: BigDecimal,
    val freightShare: BigDecimal,
    val realUnitCost: BigDecimal,
)

data class VariantRef(
    val id: UUID,
    val sku: String,
    val size: String,
    val color: String,
)

data class SupplyLotResponse(
    val id: UUID,
    val supplier: SupplierSummary,
    val receivedAt: LocalDate,
    val freightCost: BigDecimal,
    val totalCost: BigDecimal,
    val notes: String?,
    val items: List<SupplyLotItemResponse>,
    val createdBy: UserSummary,
    val createdAt: LocalDateTime,
)

data class SupplyLotSummaryResponse(
    val id: UUID,
    val supplier: SupplierSummary,
    val receivedAt: LocalDate,
    val freightCost: BigDecimal,
    val totalCost: BigDecimal,
    val itemCount: Int,
    val createdBy: UserSummary,
    val createdAt: LocalDateTime,
)

fun SupplyLot.toResponse() = SupplyLotResponse(
    id = id,
    supplier = supplier.toSummary(),
    receivedAt = receivedAt,
    freightCost = freightCost,
    totalCost = totalCost,
    notes = notes,
    items = items.map {
        SupplyLotItemResponse(
            id = it.id,
            variant = VariantRef(it.variant.id, it.variant.sku, it.variant.size, it.variant.color),
            quantity = it.quantity,
            unitCost = it.unitCost,
            freightShare = it.freightShare,
            realUnitCost = it.realUnitCost,
        )
    },
    createdBy = createdBy.toSummary(),
    createdAt = createdAt,
)

fun SupplyLot.toSummaryResponse() = SupplyLotSummaryResponse(
    id = id,
    supplier = supplier.toSummary(),
    receivedAt = receivedAt,
    freightCost = freightCost,
    totalCost = totalCost,
    itemCount = items.size,
    createdBy = createdBy.toSummary(),
    createdAt = createdAt,
)
