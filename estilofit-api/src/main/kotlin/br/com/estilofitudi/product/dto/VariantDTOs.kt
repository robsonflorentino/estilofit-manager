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
    // true = preço definido manualmente (não segue a margem na entrada de mercadoria)
    val priceOverride: Boolean,
    // preço sugerido pela margem sobre o custo médio (null quando ainda não há custo)
    val suggestedPrice: BigDecimal?,
    val active: Boolean,
)

data class VariantSummary(
    val id: UUID,
    val sku: String,
    val productName: String,
    val size: String,
    val color: String,
)

/**
 * @param effectiveMargin margem a considerar para o preço sugerido (margem da variação,
 *        ou a global quando a variação não tem margem própria). Se null, não calcula sugestão.
 */
fun ProductVariant.toResponse(effectiveMargin: BigDecimal? = null): VariantResponse {
    val cost = averageCost
    val suggested = if (cost != null && effectiveMargin != null) {
        cost.multiply(java.math.BigDecimal.ONE.add(effectiveMargin.divide(java.math.BigDecimal(100))))
            .setScale(2, java.math.RoundingMode.HALF_UP)
    } else {
        null
    }
    return VariantResponse(
        id = id,
        sku = sku,
        size = size,
        color = color,
        profitMargin = profitMargin,
        salePrice = salePrice,
        averageCost = averageCost,
        stockQuantity = stockQuantity,
        priceOverride = priceOverride,
        suggestedPrice = suggested,
        active = active,
    )
}

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

    // Preço de venda manual. Quando informado, ativa o modo "preço manual"
    // (priceOverride=true): a entrada de mercadoria deixa de sobrescrever o preço.
    @field:Positive(message = "Preço de venda deve ser positivo")
    val salePrice: BigDecimal? = null,

    // Quando true, volta ao modo automático (preço segue a margem): recalcula o preço
    // sugerido a partir do custo médio atual e desliga o priceOverride.
    val resetToSuggested: Boolean = false,
)
