package br.com.estilofitudi.product.controller

import br.com.estilofitudi.product.dto.*
import br.com.estilofitudi.product.service.ProductService
import br.com.estilofitudi.shared.dto.PageResponse
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
@RequestMapping("/products")
@Tag(name = "Produtos", description = "Cadastro e gestão de produtos")
@SecurityRequirement(name = "bearerAuth")
class ProductController(private val productService: ProductService) {

    @GetMapping
    @Operation(summary = "Listar produtos", description = "🟢 Qualquer role")
    fun list(
        @RequestParam(defaultValue = "") name: String,
        @RequestParam(required = false) categoryId: UUID?,
        @RequestParam(required = false) active: Boolean?,
        @PageableDefault(size = 20, sort = ["name"], direction = Sort.Direction.ASC)
        pageable: Pageable,
    ): ResponseEntity<PageResponse<ProductSummaryResponse>> =
        ResponseEntity.ok(productService.findAll(name, categoryId, active, pageable))

    @GetMapping("/{id}")
    @Operation(summary = "Buscar produto com variações", description = "🟢 Qualquer role")
    fun getById(@PathVariable id: UUID): ResponseEntity<ProductDetailResponse> =
        ResponseEntity.ok(productService.findById(id))

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Criar produto", description = "🟡 Admin + Gestor")
    fun create(
        @Valid @RequestBody request: CreateProductRequest,
    ): ResponseEntity<ProductDetailResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request))

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Atualizar produto", description = "🟡 Admin + Gestor")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: CreateProductRequest,
    ): ResponseEntity<ProductDetailResponse> =
        ResponseEntity.ok(productService.update(id, request))

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Ativar/desativar produto", description = "🟡 Admin + Gestor")
    fun updateStatus(
        @PathVariable id: UUID,
        @Valid @RequestBody request: ProductStatusRequest,
    ): ResponseEntity<ProductDetailResponse> =
        ResponseEntity.ok(productService.updateStatus(id, request.active))
}
