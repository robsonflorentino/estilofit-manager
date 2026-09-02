package br.com.estilofitudi.product.dto

import br.com.estilofitudi.product.domain.ProductVariant
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.util.*

// ── Responses ────────────────────────────────────────────────────────────────

data class VariantResponse(
    val id: UUID,
    val sku: String,
    val size: String,
    val color: String,
    val profitMargin: BigDecimal?,
    val salePrice: BigDecimal?,
    val averageCost: BigDecimal?,
    val stockQuantity: Int,
    val active: Boolean,
)

data class VariantSummary(
    val id: UUID,
    val sku: String,
    val productName: String,
    val size: String,
    val color: String,
)

fun ProductVariant.toResponse() = VariantResponse(
    id = id,
    sku = sku,
    size = size,
    color = color,
    profitMargin = profitMargin,
    salePrice = salePrice,
    averageCost = averageCost,
    stockQuantity = stockQuantity,
    active = active,
)

fun ProductVariant.toSummary() = VariantSummary(
    id = id,
    sku = sku,
    productName = product.name,
    size = size,
    color = color,
)

// ── Requests ─────────────────────────────────────────────────────────────────

data class CreateVariantRequest(
    @field:NotBlank(message = "Tamanho é obrigatório")
    @field:Size(max = 10, message = "Tamanho deve ter no máximo 10 caracteres")
    val size: String,

    @field:NotBlank(message = "Cor é obrigatória")
    @field:Size(max = 50, message = "Cor deve ter no máximo 50 caracteres")
    val color: String,

    // null herda a margem global configurada no sistema
    val profitMargin: BigDecimal? = null,
)

data class UpdateVariantRequest(
    val profitMargin: BigDecimal? = null,

    @field:Positive(message = "Preço de venda deve ser positivo")
    val salePrice: BigDecimal? = null,
)
