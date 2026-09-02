package br.com.estilofitudi.product.service

import br.com.estilofitudi.product.domain.ProductVariant
import br.com.estilofitudi.product.dto.CreateVariantRequest
import br.com.estilofitudi.product.dto.UpdateVariantRequest
import br.com.estilofitudi.product.dto.VariantResponse
import br.com.estilofitudi.product.dto.toResponse
import br.com.estilofitudi.product.repository.ProductRepository
import br.com.estilofitudi.product.repository.ProductVariantRepository
import br.com.estilofitudi.shared.exception.BusinessException
import br.com.estilofitudi.shared.exception.DataConflictException
import br.com.estilofitudi.shared.exception.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

@Service
@Transactional(readOnly = true)
class ProductVariantService(
    private val productRepository: ProductRepository,
    private val variantRepository: ProductVariantRepository,
    private val skuGenerator: SkuGenerator,
) {

    @Transactional
    fun create(productId: UUID, request: CreateVariantRequest): VariantResponse {
        val product = productRepository.findById(productId)
            .orElseThrow { EntityNotFoundException("Produto", productId) }

        // Regra: não permitir variação em produto inativo (decisão 4 do tech design)
        if (!product.active) {
            throw BusinessException("Não é possível adicionar variação a um produto inativo.")
        }

        val size = request.size.trim()
        val color = request.color.trim()

        // Regra: variação única por (produto, tamanho, cor)
        if (variantRepository.existsByProductIdAndSizeIgnoreCaseAndColorIgnoreCase(productId, size, color)) {
            throw DataConflictException("Já existe uma variação $size / $color para este produto.")
        }

        val sku = skuGenerator.generate(product.category.name, size, color)

        val variant = ProductVariant(
            product = product,
            sku = sku,
            size = size,
            color = color,
            profitMargin = request.profitMargin,
            salePrice = null,   // sem custo ainda; preço vem na entrada de mercadoria
            averageCost = null,
            stockQuantity = 0,
        )
        return variantRepository.save(variant).toResponse()
    }

    @Transactional
    fun update(productId: UUID, variantId: UUID, request: UpdateVariantRequest): VariantResponse {
        val variant = findVariantOfProduct(productId, variantId)

        // SKU imutável (decisão 5): size/color NÃO são alterados aqui.
        request.profitMargin?.let { variant.profitMargin = it }
        request.salePrice?.let { variant.salePrice = it }

        // Se veio novo preço manual e há custo, recalcula a margem efetiva para transparência
        val cost = variant.averageCost
        if (request.salePrice != null && cost != null && cost > BigDecimal.ZERO) {
            variant.profitMargin = request.salePrice.subtract(cost)
                .divide(cost, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal(100))
                .setScale(2, RoundingMode.HALF_UP)
        }

        return variantRepository.save(variant).toResponse()
    }

    @Transactional
    fun updateStatus(productId: UUID, variantId: UUID, active: Boolean): VariantResponse {
        val variant = findVariantOfProduct(productId, variantId)
        variant.active = active
        return variantRepository.save(variant).toResponse()
    }

    private fun findVariantOfProduct(productId: UUID, variantId: UUID): ProductVariant {
        val variant = variantRepository.findById(variantId)
            .orElseThrow { EntityNotFoundException("Variação", variantId) }
        if (variant.product.id != productId) {
            throw EntityNotFoundException("Variação", variantId)
        }
        return variant
    }
}
