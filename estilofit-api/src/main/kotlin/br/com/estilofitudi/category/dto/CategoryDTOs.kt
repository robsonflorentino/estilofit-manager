package br.com.estilofitudi.category.dto

import br.com.estilofitudi.category.domain.Category
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.*

// ── Responses ────────────────────────────────────────────────────────────────

data class CategoryResponse(
    val id: UUID,
    val name: String,
    val active: Boolean,
)

fun Category.toResponse() = CategoryResponse(
    id = id,
    name = name,
    active = active,
)

// ── Requests ─────────────────────────────────────────────────────────────────

data class CategoryRequest(
    @field:NotBlank(message = "Nome é obrigatório")
    @field:Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
    val name: String,
)

data class CategoryStatusRequest(
    @field:NotNull(message = "Status é obrigatório")
    val active: Boolean,
)
