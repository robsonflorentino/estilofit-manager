package br.com.estilofitudi.user.controller

import br.com.estilofitudi.shared.dto.PageResponse
import br.com.estilofitudi.user.domain.Role
import br.com.estilofitudi.user.dto.*
import br.com.estilofitudi.user.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/users")
@Tag(name = "Usuários", description = "Gerenciamento de usuários do sistema")
@SecurityRequirement(name = "bearerAuth")
class UserController(private val userService: UserService) {

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar usuários", description = "🔴 Apenas Admin")
    fun listUsers(
        @RequestParam(defaultValue = "") name: String,
        @RequestParam(required = false) role: Role?,
        @RequestParam(required = false) active: Boolean?,
        @PageableDefault(size = 20, sort = ["name"], direction = Sort.Direction.ASC)
        pageable: Pageable,
    ): ResponseEntity<PageResponse<UserResponse>> =
        ResponseEntity.ok(userService.findAll(name, role, active, pageable))

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Criar usuário", description = "🔴 Apenas Admin")
    fun createUser(
        @Valid @RequestBody request: CreateUserRequest,
    ): ResponseEntity<UserResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request))

    @GetMapping("/me")
    @Operation(summary = "Meus dados", description = "🟢 Qualquer role — dados do usuário logado")
    fun getMe(authentication: Authentication): ResponseEntity<UserResponse> =
        ResponseEntity.ok(userService.getMe(authentication.name))

    @PatchMapping("/me/password")
    @Operation(summary = "Alterar minha senha", description = "🟢 Qualquer role")
    fun changeMyPassword(
        @Valid @RequestBody request: ChangeMyPasswordRequest,
        authentication: Authentication,
    ): ResponseEntity<Void> {
        userService.changeMyPassword(authentication.name, request.currentPassword, request.newPassword)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Buscar usuário por ID", description = "🔴 Apenas Admin")
    fun getUserById(@PathVariable id: UUID): ResponseEntity<UserResponse> =
        ResponseEntity.ok(userService.findById(id))

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualizar usuário", description = "🔴 Apenas Admin")
    fun updateUser(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateUserRequest,
    ): ResponseEntity<UserResponse> =
        ResponseEntity.ok(userService.update(id, request))

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ativar/desativar usuário", description = "🔴 Apenas Admin")
    fun updateStatus(
        @PathVariable id: UUID,
        @Valid @RequestBody request: StatusRequest,
    ): ResponseEntity<UserResponse> =
        ResponseEntity.ok(userService.updateStatus(id, request.active))

    @PatchMapping("/{id}/password")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Redefinir senha de usuário", description = "🔴 Apenas Admin")
    fun resetPassword(
        @PathVariable id: UUID,
        @Valid @RequestBody request: ResetPasswordRequest,
    ): ResponseEntity<Void> {
        userService.resetPassword(id, request.newPassword)
        return ResponseEntity.noContent().build()
    }
}
