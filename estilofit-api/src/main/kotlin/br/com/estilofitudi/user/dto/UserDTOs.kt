package br.com.estilofitudi.user.dto

import br.com.estilofitudi.user.domain.Role
import br.com.estilofitudi.user.domain.User
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.*

// ── Responses ────────────────────────────────────────────────────────────────

data class UserResponse(
    val id: UUID,
    val name: String,
    val email: String,
    val role: Role,
    val active: Boolean,
    val createdAt: LocalDateTime,
)

data class UserSummary(
    val id: UUID,
    val name: String,
)

fun User.toResponse() = UserResponse(
    id = id,
    name = name,
    email = email,
    role = role,
    active = active,
    createdAt = createdAt,
)

fun User.toSummary() = UserSummary(
    id = id,
    name = name,
)

// ── Requests ─────────────────────────────────────────────────────────────────

data class CreateUserRequest(
    @field:NotBlank(message = "Nome é obrigatório")
    @field:Size(min = 2, max = 200, message = "Nome deve ter entre 2 e 200 caracteres")
    val name: String,

    @field:NotBlank(message = "E-mail é obrigatório")
    @field:Email(message = "E-mail inválido")
    val email: String,

    @field:NotBlank(message = "Senha é obrigatória")
    @field:Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
    val password: String,

    @field:NotNull(message = "Perfil é obrigatório")
    val role: Role,
)

data class UpdateUserRequest(
    @field:NotBlank(message = "Nome é obrigatório")
    @field:Size(min = 2, max = 200, message = "Nome deve ter entre 2 e 200 caracteres")
    val name: String,

    @field:NotBlank(message = "E-mail é obrigatório")
    @field:Email(message = "E-mail inválido")
    val email: String,

    @field:NotNull(message = "Perfil é obrigatório")
    val role: Role,
)

data class StatusRequest(
    @field:NotNull(message = "Status é obrigatório")
    val active: Boolean,
)

data class ResetPasswordRequest(
    @field:NotBlank(message = "Nova senha é obrigatória")
    @field:Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
    val newPassword: String,
)

data class ChangeMyPasswordRequest(
    @field:NotBlank(message = "Senha atual é obrigatória")
    val currentPassword: String,

    @field:NotBlank(message = "Nova senha é obrigatória")
    @field:Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
    val newPassword: String,
)
