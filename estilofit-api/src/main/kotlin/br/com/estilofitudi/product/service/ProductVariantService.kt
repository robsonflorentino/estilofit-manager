package br.com.estilofitudi.product.service

import br.com.estilofitudi.product.domain.ProductVariant
import br.com.estilofitudi.product.dto.CreateVariantRequest
import br.com.estilofitudi.product.dto.UpdateVariantRequest
import br.com.estilofitudi.product.dto.VariantResponse
import br.com.estilofitudi.product.dto.toResponse
import br.com.estilofitudi.inventory.service.SettingsReader
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
    private val settingsReader: SettingsReader,
) {

    /** Margem efetiva da variação: a própria, ou a global quando não definida. */
    private fun effectiveMargin(variant: ProductVariant): BigDecimal =
        variant.profitMargin ?: settingsReader.defaultProfitMargin()

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
        val saved = variantRepository.save(variant)
        return saved.toResponse(effectiveMargin(saved))
    }

    @Transactional
    fun update(productId: UUID, variantId: UUID, request: UpdateVariantRequest): VariantResponse {
        val variant = findVariantOfProduct(productId, variantId)

        // SKU imutável (decisão 5): size/color NÃO são alterados aqui.
        request.profitMargin?.let { variant.profitMargin = it }

        if (request.resetToSuggested) {
            // Volta ao modo automático: preço segue a margem novamente.
            variant.priceOverride = false
            val cost = variant.averageCost
            if (cost != null) {
                variant.salePrice = cost
                    .multiply(BigDecimal.ONE.add(effectiveMargin(variant).divide(BigDecimal(100))))
                    .setScale(2, RoundingMode.HALF_UP)
            }
        } else if (request.salePrice != null) {
            // Preço manual: passa a valer independentemente da margem, inclusive na
            // entrada de mercadoria (que não sobrescreve mais o preço).
            // NÃO alteramos profitMargin aqui: ela guarda a margem "desejada" da variação
            // (usada como sugestão e no "voltar ao sugerido"). A margem efetiva do preço
            // praticado é derivável no cliente a partir de salePrice e averageCost.
            variant.salePrice = request.salePrice
            variant.priceOverride = true
        }

        val saved = variantRepository.save(variant)
        return saved.toResponse(effectiveMargin(saved))
    }

    @Transactional
    fun updateStatus(productId: UUID, variantId: UUID, active: Boolean): VariantResponse {
        val variant = findVariantOfProduct(productId, variantId)
        variant.active = active
        val saved = variantRepository.save(variant)
        return saved.toResponse(effectiveMargin(saved))
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
