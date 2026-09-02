package br.com.estilofitudi.product.controller

import br.com.estilofitudi.product.dto.CreateVariantRequest
import br.com.estilofitudi.product.dto.ProductStatusRequest
import br.com.estilofitudi.product.dto.UpdateVariantRequest
import br.com.estilofitudi.product.dto.VariantResponse
import br.com.estilofitudi.product.service.ProductVariantService
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
@RequestMapping("/products/{productId}/variants")
@Tag(name = "Variações", description = "Variações de produto (tamanho e cor)")
@SecurityRequirement(name = "bearerAuth")
class ProductVariantController(private val variantService: ProductVariantService) {

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Adicionar variação", description = "🟡 Admin + Gestor — SKU gerado automaticamente")
    fun create(
        @PathVariable productId: UUID,
        @Valid @RequestBody request: CreateVariantRequest,
    ): ResponseEntity<VariantResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(variantService.create(productId, request))

    @PutMapping("/{variantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Atualizar variação", description = "🟡 Admin + Gestor — só margem e preço (SKU imutável)")
    fun update(
        @PathVariable productId: UUID,
        @PathVariable variantId: UUID,
        @Valid @RequestBody request: UpdateVariantRequest,
    ): ResponseEntity<VariantResponse> =
        ResponseEntity.ok(variantService.update(productId, variantId, request))

    @PatchMapping("/{variantId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Ativar/desativar variação", description = "🟡 Admin + Gestor")
    fun updateStatus(
        @PathVariable productId: UUID,
        @PathVariable variantId: UUID,
        @Valid @RequestBody request: ProductStatusRequest,
    ): ResponseEntity<VariantResponse> =
        ResponseEntity.ok(variantService.updateStatus(productId, variantId, request.active))
}
