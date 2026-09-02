package br.com.estilofitudi.supplier.dto

import br.com.estilofitudi.supplier.domain.Supplier
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.*

// ── Responses ────────────────────────────────────────────────────────────────

data class SupplierResponse(
    val id: UUID,
    val name: String,
    val contactPhone: String?,
    val contactEmail: String?,
    val whatsapp: String?,
    val cnpj: String?,
    val address: String?,
    val notes: String?,
    val active: Boolean,
    val createdAt: LocalDateTime,
)

data class SupplierSummary(
    val id: UUID,
    val name: String,
)

fun Supplier.toResponse() = SupplierResponse(
    id = id,
    name = name,
    contactPhone = contactPhone,
    contactEmail = contactEmail,
    whatsapp = whatsapp,
    cnpj = cnpj,
    address = address,
    notes = notes,
    active = active,
    createdAt = createdAt,
)

fun Supplier.toSummary() = SupplierSummary(id = id, name = name)

// ── Requests ─────────────────────────────────────────────────────────────────

data class SupplierRequest(
    @field:NotBlank(message = "Nome é obrigatório")
    @field:Size(min = 2, max = 200, message = "Nome deve ter entre 2 e 200 caracteres")
    val name: String,

    val contactPhone: String? = null,

    @field:Email(message = "E-mail inválido")
    val contactEmail: String? = null,

    val whatsapp: String? = null,

    @field:Size(max = 18, message = "CNPJ deve ter no máximo 18 caracteres")
    val cnpj: String? = null,

    val address: String? = null,

    val notes: String? = null,
)

data class SupplierStatusRequest(
    @field:NotNull(message = "Status é obrigatório")
    val active: Boolean,
)
