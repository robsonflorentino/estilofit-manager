package br.com.estilofitudi.supplier.controller

import br.com.estilofitudi.shared.dto.PageResponse
import br.com.estilofitudi.supplier.dto.SupplierRequest
import br.com.estilofitudi.supplier.dto.SupplierResponse
import br.com.estilofitudi.supplier.dto.SupplierStatusRequest
import br.com.estilofitudi.supplier.service.SupplierService
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
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/suppliers")
@Tag(name = "Fornecedores", description = "Cadastro e gestão de fornecedores")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
class SupplierController(private val supplierService: SupplierService) {

    @GetMapping
    @Operation(summary = "Listar fornecedores", description = "🟡 Admin + Gestor")
    fun list(
        @RequestParam(defaultValue = "") name: String,
        @RequestParam(required = false) active: Boolean?,
        @PageableDefault(size = 20, sort = ["name"], direction = Sort.Direction.ASC)
        pageable: Pageable,
    ): ResponseEntity<PageResponse<SupplierResponse>> =
        ResponseEntity.ok(supplierService.findAll(name, active, pageable))

    @GetMapping("/{id}")
    @Operation(summary = "Buscar fornecedor por ID", description = "🟡 Admin + Gestor")
    fun getById(@PathVariable id: UUID): ResponseEntity<SupplierResponse> =
        ResponseEntity.ok(supplierService.findById(id))

    @PostMapping
    @Operation(summary = "Criar fornecedor", description = "🟡 Admin + Gestor")
    fun create(
        @Valid @RequestBody request: SupplierRequest,
    ): ResponseEntity<SupplierResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(supplierService.create(request))

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar fornecedor", description = "🟡 Admin + Gestor")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: SupplierRequest,
    ): ResponseEntity<SupplierResponse> =
        ResponseEntity.ok(supplierService.update(id, request))

    @PatchMapping("/{id}/status")
    @Operation(summary = "Ativar/desativar fornecedor", description = "🟡 Admin + Gestor")
    fun updateStatus(
        @PathVariable id: UUID,
        @Valid @RequestBody request: SupplierStatusRequest,
    ): ResponseEntity<SupplierResponse> =
        ResponseEntity.ok(supplierService.updateStatus(id, request.active))
}
