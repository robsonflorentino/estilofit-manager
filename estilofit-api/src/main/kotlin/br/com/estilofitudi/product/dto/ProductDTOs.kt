package br.com.estilofitudi.product.dto

import br.com.estilofitudi.category.dto.CategoryResponse
import br.com.estilofitudi.category.dto.toResponse
import br.com.estilofitudi.product.domain.Product
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.*

// ── Responses ────────────────────────────────────────────────────────────────

data class CategorySummary(
    val id: UUID,
    val name: String,
)

data class ProductSummaryResponse(
    val id: UUID,
    val name: String,
    val category: CategorySummary,
    val active: Boolean,
    val variantCount: Int,
    val totalStock: Int,
    val createdAt: LocalDateTime,
)

data class ProductDetailResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val category: CategoryResponse,
    val active: Boolean,
    val variants: List<VariantResponse>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

fun Product.toSummaryResponse() = ProductSummaryResponse(
    id = id,
    name = name,
    category = CategorySummary(category.id, category.name),
    active = active,
    variantCount = variants.size,
    totalStock = variants.sumOf { it.stockQuantity },
    createdAt = createdAt,
)

fun Product.toDetailResponse() = ProductDetailResponse(
    id = id,
    name = name,
    description = description,
    category = category.toResponse(),
    active = active,
    variants = variants.map { it.toResponse() },
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// ── Requests ─────────────────────────────────────────────────────────────────

data class CreateProductRequest(
    @field:NotBlank(message = "Nome é obrigatório")
    @field:Size(min = 2, max = 200, message = "Nome deve ter entre 2 e 200 caracteres")
    val name: String,

    val description: String? = null,

    @field:NotNull(message = "Categoria é obrigatória")
    val categoryId: UUID,
)

data class ProductStatusRequest(
    @field:NotNull(message = "Status é obrigatório")
    val active: Boolean,
)
