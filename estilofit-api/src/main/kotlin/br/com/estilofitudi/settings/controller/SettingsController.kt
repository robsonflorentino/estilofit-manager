package br.com.estilofitudi.settings.controller

import br.com.estilofitudi.settings.dto.SettingResponse
import br.com.estilofitudi.settings.dto.UpdateSettingRequest
import br.com.estilofitudi.settings.service.SettingsService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/settings")
@Tag(name = "Configurações", description = "Parâmetros do sistema")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
class SettingsController(private val settingsService: SettingsService) {

    @GetMapping
    @Operation(summary = "Listar configurações", description = "🔴 Admin")
    fun list(): ResponseEntity<List<SettingResponse>> =
        ResponseEntity.ok(settingsService.list())

    @PutMapping("/{key}")
    @Operation(summary = "Atualizar configuração", description = "🔴 Admin")
    fun update(
        @PathVariable key: String,
        @Valid @RequestBody request: UpdateSettingRequest,
        authentication: Authentication,
    ): ResponseEntity<SettingResponse> =
        ResponseEntity.ok(settingsService.update(key, request.value, authentication.name))
}
