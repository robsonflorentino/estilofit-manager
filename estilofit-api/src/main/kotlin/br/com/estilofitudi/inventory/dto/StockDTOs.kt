package br.com.estilofitudi.inventory.dto

import br.com.estilofitudi.inventory.domain.StockMovement
import br.com.estilofitudi.inventory.domain.StockMovementType
import br.com.estilofitudi.product.domain.ProductVariant
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

// ── Resumo de estoque ────────────────────────────────────────────────────────

data class StockSummaryItem(
    val variantId: UUID,
    val sku: String,
    val productName: String,
    val category: String,
    val size: String,
    val color: String,
    val stockQuantity: Int,
    val salePrice: BigDecimal?,
    val averageCost: BigDecimal?,
    val isLowStock: Boolean,
    val isZeroStock: Boolean,
)

fun ProductVariant.toStockSummaryItem(lowStockThreshold: Int) = StockSummaryItem(
    variantId = id,
    sku = sku,
    productName = product.name,
    category = product.category.name,
    size = size,
    color = color,
    stockQuantity = stockQuantity,
    salePrice = salePrice,
    averageCost = averageCost,
    isLowStock = stockQuantity in 1..(lowStockThreshold - 1),
    isZeroStock = stockQuantity == 0,
)

// ── Movimentações ────────────────────────────────────────────────────────────

data class StockMovementResponse(
    val id: UUID,
    val variant: MovementVariantRef,
    val type: StockMovementType,
    val quantity: Int,
    val referenceType: String?,
    val referenceId: UUID?,
    val notes: String?,
    val user: MovementUserRef,
    val createdAt: LocalDateTime,
)

data class MovementVariantRef(
    val id: UUID,
    val sku: String,
    val productName: String,
)

data class MovementUserRef(
    val id: UUID,
    val name: String,
)

fun StockMovement.toResponse() = StockMovementResponse(
    id = id,
    variant = MovementVariantRef(variant.id, variant.sku, variant.product.name),
    type = type,
    quantity = quantity,
    referenceType = referenceType,
    referenceId = referenceId,
    notes = notes,
    user = MovementUserRef(user.id, user.name),
    createdAt = createdAt,
)

// ── Ajuste manual ────────────────────────────────────────────────────────────

data class StockAdjustmentRequest(
    @field:NotNull(message = "Variação é obrigatória")
    val variantId: UUID,

    @field:NotNull(message = "Quantidade é obrigatória")
    val quantity: Int, // positivo = entrada, negativo = saída

    @field:Size(min = 5, message = "Justificativa é obrigatória (mínimo 5 caracteres)")
    val notes: String,
)

// ── Correção de custo médio ──────────────────────────────────────────────────

data class CorrectCostRequest(
    @field:NotNull(message = "Variação é obrigatória")
    val variantId: UUID,

    // Novo custo médio. Aceita >= 0 (0 permitido, ex.: brinde/consignação).
    @field:NotNull(message = "Custo é obrigatório")
    @field:PositiveOrZero(message = "Custo não pode ser negativo")
    val averageCost: BigDecimal,

    @field:Size(min = 5, message = "Justificativa é obrigatória (mínimo 5 caracteres)")
    val notes: String,
)
