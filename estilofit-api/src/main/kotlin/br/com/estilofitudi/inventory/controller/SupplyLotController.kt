package br.com.estilofitudi.inventory.controller

import br.com.estilofitudi.inventory.dto.CreateSupplyLotRequest
import br.com.estilofitudi.inventory.dto.SupplyLotResponse
import br.com.estilofitudi.inventory.dto.SupplyLotSummaryResponse
import br.com.estilofitudi.inventory.service.SupplyLotService
import br.com.estilofitudi.shared.dto.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/supply-lots")
@Tag(name = "Lotes de Entrada", description = "Entrada de mercadoria e atualização de estoque")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
class SupplyLotController(private val supplyLotService: SupplyLotService) {

    @GetMapping
    @Operation(summary = "Listar lotes de entrada", description = "🟡 Admin + Gestor")
    fun list(
        @RequestParam(required = false) supplierId: UUID?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?,
        @PageableDefault(size = 20, sort = ["receivedAt"], direction = Sort.Direction.DESC)
        pageable: Pageable,
    ): ResponseEntity<PageResponse<SupplyLotSummaryResponse>> =
        ResponseEntity.ok(supplyLotService.findAll(supplierId, startDate, endDate, pageable))

    @GetMapping("/{id}")
    @Operation(summary = "Buscar lote por ID", description = "🟡 Admin + Gestor")
    fun getById(@PathVariable id: UUID): ResponseEntity<SupplyLotResponse> =
        ResponseEntity.ok(supplyLotService.findById(id))

    @PostMapping
    @Operation(
        summary = "Registrar lote de entrada",
        description = "🟡 Admin + Gestor — rateia frete, recalcula custo médio, preço e estoque",
    )
    fun create(
        @Valid @RequestBody request: CreateSupplyLotRequest,
        authentication: Authentication,
    ): ResponseEntity<SupplyLotResponse> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(supplyLotService.create(request, authentication.name))
}
