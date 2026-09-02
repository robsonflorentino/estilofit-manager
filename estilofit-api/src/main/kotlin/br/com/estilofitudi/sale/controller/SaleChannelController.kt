package br.com.estilofitudi.sale.controller

import br.com.estilofitudi.sale.dto.CreateSaleChannelRequest
import br.com.estilofitudi.sale.dto.SaleChannelResponse
import br.com.estilofitudi.sale.dto.UpdateSaleChannelStatusRequest
import br.com.estilofitudi.sale.service.SaleChannelService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/sale-channels")
@Tag(name = "Canais de Venda", description = "Cadastro de canais de venda")
@SecurityRequirement(name = "bearerAuth")
class SaleChannelController(private val channelService: SaleChannelService) {

    @GetMapping
    @Operation(summary = "Listar canais de venda", description = "🟢 Qualquer role")
    fun findAll(
        @RequestParam(defaultValue = "false") includeInactive: Boolean,
    ): ResponseEntity<List<SaleChannelResponse>> =
        ResponseEntity.ok(channelService.findAll(includeInactive))

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Criar canal de venda", description = "🟡 Admin + Gestor")
    fun create(
        @Valid @RequestBody request: CreateSaleChannelRequest,
    ): ResponseEntity<SaleChannelResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(channelService.create(request))

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Ativar/inativar canal de venda", description = "🟡 Admin + Gestor")
    fun updateStatus(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateSaleChannelStatusRequest,
    ): ResponseEntity<SaleChannelResponse> =
        ResponseEntity.ok(channelService.updateStatus(id, request))
}
