package br.com.estilofitudi.category.controller

import br.com.estilofitudi.category.dto.CategoryRequest
import br.com.estilofitudi.category.dto.CategoryResponse
import br.com.estilofitudi.category.dto.CategoryStatusRequest
import br.com.estilofitudi.category.service.CategoryService
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
@RequestMapping("/categories")
@Tag(name = "Categorias", description = "Categorias de produto")
@SecurityRequirement(name = "bearerAuth")
class CategoryController(private val categoryService: CategoryService) {

    @GetMapping
    @Operation(summary = "Listar categorias", description = "🟢 Qualquer role")
    fun list(
        @RequestParam(defaultValue = "true") onlyActive: Boolean,
    ): ResponseEntity<List<CategoryResponse>> =
        ResponseEntity.ok(categoryService.findAll(onlyActive))

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Criar categoria", description = "🟡 Admin + Gestor")
    fun create(
        @Valid @RequestBody request: CategoryRequest,
    ): ResponseEntity<CategoryResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request))

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Renomear categoria", description = "🟡 Admin + Gestor")
    fun rename(
        @PathVariable id: UUID,
        @Valid @RequestBody request: CategoryRequest,
    ): ResponseEntity<CategoryResponse> =
        ResponseEntity.ok(categoryService.rename(id, request))

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Ativar/desativar categoria", description = "🟡 Admin + Gestor")
    fun updateStatus(
        @PathVariable id: UUID,
        @Valid @RequestBody request: CategoryStatusRequest,
    ): ResponseEntity<CategoryResponse> =
        ResponseEntity.ok(categoryService.updateStatus(id, request.active))
}
