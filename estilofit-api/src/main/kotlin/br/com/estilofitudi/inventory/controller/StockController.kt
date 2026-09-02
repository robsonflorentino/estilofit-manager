package br.com.estilofitudi.inventory.controller

import br.com.estilofitudi.inventory.domain.StockMovementType
import br.com.estilofitudi.inventory.dto.StockAdjustmentRequest
import br.com.estilofitudi.inventory.dto.StockMovementResponse
import br.com.estilofitudi.inventory.dto.StockSummaryItem
import br.com.estilofitudi.inventory.service.StockService
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
@RequestMapping("/stock")
@Tag(name = "Estoque", description = "Consulta de estoque e movimentações")
@SecurityRequirement(name = "bearerAuth")
class StockController(private val stockService: StockService) {

    @GetMapping("/summary")
    @Operation(summary = "Resumo do estoque atual", description = "🟢 Qualquer role")
    fun summary(
        @RequestParam(required = false) productId: UUID?,
        @RequestParam(required = false) categoryId: UUID?,
        // Nome distinto de "size" para não colidir com o size do Pageable (paginação)
        @RequestParam(name = "variantSize", defaultValue = "") variantSize: String,
        @RequestParam(defaultValue = "") color: String,
        @RequestParam(defaultValue = "false") lowStock: Boolean,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ResponseEntity<PageResponse<StockSummaryItem>> =
        ResponseEntity.ok(stockService.summary(productId, categoryId, variantSize, color, lowStock, pageable))

    @GetMapping("/movements")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Histórico de movimentações", description = "🟡 Admin + Gestor")
    fun movements(
        @RequestParam(required = false) variantId: UUID?,
        @RequestParam(required = false) type: StockMovementType?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) start: LocalDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) end: LocalDateTime?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ResponseEntity<PageResponse<StockMovementResponse>> =
        ResponseEntity.ok(stockService.movements(variantId, type, start, end, pageable))

    @PostMapping("/adjustments")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Ajuste manual de estoque", description = "🟡 Admin + Gestor — justificativa obrigatória")
    fun adjust(
        @Valid @RequestBody request: StockAdjustmentRequest,
        authentication: Authentication,
    ): ResponseEntity<StockMovementResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(stockService.adjust(request, authentication.name))
}
