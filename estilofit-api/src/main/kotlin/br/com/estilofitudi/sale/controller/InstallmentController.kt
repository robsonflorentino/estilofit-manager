package br.com.estilofitudi.sale.controller

import br.com.estilofitudi.sale.domain.InstallmentStatus
import br.com.estilofitudi.sale.dto.InstallmentResponse
import br.com.estilofitudi.sale.dto.InstallmentWithSaleResponse
import br.com.estilofitudi.sale.dto.ProjectedMonthResponse
import br.com.estilofitudi.sale.dto.ReceiveInstallmentRequest
import br.com.estilofitudi.sale.service.InstallmentService
import br.com.estilofitudi.shared.dto.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/installments")
@Tag(name = "Contas a Receber", description = "Parcelas de vendas parceladas e fluxo projetado")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
class InstallmentController(private val installmentService: InstallmentService) {

    @GetMapping
    @Operation(summary = "Listar parcelas", description = "🟡 Admin + Gestor")
    fun findAll(
        @RequestParam(required = false) status: InstallmentStatus?,
        @RequestParam(required = false) saleId: UUID?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDue: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDue: LocalDate?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ResponseEntity<PageResponse<InstallmentWithSaleResponse>> =
        ResponseEntity.ok(installmentService.findAll(status, saleId, startDue, endDue, pageable))

    @GetMapping("/projected")
    @Operation(summary = "Fluxo de caixa projetado", description = "🟡 Admin + Gestor — parcelas pendentes agrupadas por mês")
    fun projected(
        @RequestParam(defaultValue = "6") months: Int,
    ): ResponseEntity<List<ProjectedMonthResponse>> =
        ResponseEntity.ok(installmentService.projected(months))

    @PatchMapping("/{id}/receive")
    @Operation(summary = "Dar baixa em parcela", description = "🟡 Admin + Gestor")
    fun receive(
        @PathVariable id: UUID,
        @Valid @RequestBody(required = false) request: ReceiveInstallmentRequest?,
        authentication: Authentication,
    ): ResponseEntity<InstallmentResponse> =
        ResponseEntity.ok(
            installmentService.receive(id, request ?: ReceiveInstallmentRequest(), authentication.name)
        )
}
