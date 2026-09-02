package br.com.estilofitudi.sale.controller

import br.com.estilofitudi.sale.domain.PaymentMethod
import br.com.estilofitudi.sale.domain.SaleStatus
import br.com.estilofitudi.sale.dto.CancelSaleRequest
import br.com.estilofitudi.sale.dto.CreateSaleRequest
import br.com.estilofitudi.sale.dto.SaleDetailResponse
import br.com.estilofitudi.sale.dto.SaleSummaryResponse
import br.com.estilofitudi.sale.service.SaleService
import br.com.estilofitudi.shared.dto.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.util.*

@RestController
@RequestMapping("/sales")
@Tag(name = "Vendas", description = "Registro e consulta de vendas")
@SecurityRequirement(name = "bearerAuth")
class SaleController(private val saleService: SaleService) {

    @GetMapping
    @Operation(
        summary = "Listar vendas",
        description = "🟢 Qualquer role — vendedor vê apenas as próprias vendas",
    )
    fun findAll(
        @RequestParam(required = false) channelId: UUID?,
        @RequestParam(required = false) paymentMethod: PaymentMethod?,
        @RequestParam(required = false) status: SaleStatus?,
        @RequestParam(required = false) sellerId: UUID?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: LocalDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: LocalDateTime?,
        authentication: Authentication,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ResponseEntity<PageResponse<SaleSummaryResponse>> =
        ResponseEntity.ok(
            saleService.findAll(
                channelId, paymentMethod, status, sellerId, startDate, endDate, authentication.name, pageable,
            )
        )

    @GetMapping("/{id}")
    @Operation(
        summary = "Detalhar venda",
        description = "🟢 Qualquer role — vendedor só acessa as próprias vendas",
    )
    fun findById(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): ResponseEntity<SaleDetailResponse> =
        ResponseEntity.ok(saleService.findById(id, authentication.name))

    @PostMapping
    @Operation(summary = "Registrar venda", description = "🟢 Qualquer role")
    fun create(
        @Valid @RequestBody request: CreateSaleRequest,
        authentication: Authentication,
    ): ResponseEntity<SaleDetailResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(saleService.create(request, authentication.name))

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Cancelar venda", description = "🟡 Admin + Gestor — estorna estoque e cancela parcelas pendentes")
    fun cancel(
        @PathVariable id: UUID,
        @Valid @RequestBody request: CancelSaleRequest,
        authentication: Authentication,
    ): ResponseEntity<SaleDetailResponse> =
        ResponseEntity.ok(saleService.cancel(id, request, authentication.name))
}
