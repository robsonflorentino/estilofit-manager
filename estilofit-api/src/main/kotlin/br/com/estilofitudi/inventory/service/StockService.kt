package br.com.estilofitudi.inventory.service

import br.com.estilofitudi.inventory.domain.StockMovement
import br.com.estilofitudi.inventory.domain.StockMovementType
import br.com.estilofitudi.inventory.dto.*
import br.com.estilofitudi.inventory.repository.StockMovementRepository
import br.com.estilofitudi.product.domain.ProductVariant
import br.com.estilofitudi.product.repository.ProductVariantRepository
import br.com.estilofitudi.shared.dto.PageResponse
import br.com.estilofitudi.shared.exception.BusinessException
import br.com.estilofitudi.shared.exception.EntityNotFoundException
import br.com.estilofitudi.user.domain.User
import br.com.estilofitudi.user.repository.UserRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional(readOnly = true)
class StockService(
    private val variantRepository: ProductVariantRepository,
    private val stockMovementRepository: StockMovementRepository,
    private val userRepository: UserRepository,
    private val settingsReader: SettingsReader,
) {

    fun summary(
        productId: UUID?,
        categoryId: UUID?,
        variantSize: String,
        color: String,
        lowStockOnly: Boolean,
        pageable: Pageable,
    ): PageResponse<StockSummaryItem> {
        val threshold = settingsReader.lowStockThreshold()
        val page = variantRepository.findStockSummary(
            productId = productId,
            categoryId = categoryId,
            variantSize = variantSize,
            color = color,
            lowStockThreshold = if (lowStockOnly) threshold else null,
            pageable = pageable,
        )
        return PageResponse.from(page.map { it.toStockSummaryItem(threshold) })
    }

    fun movements(
        variantId: UUID?,
        type: StockMovementType?,
        start: LocalDateTime?,
        end: LocalDateTime?,
        pageable: Pageable,
    ): PageResponse<StockMovementResponse> {
        val page = stockMovementRepository.findAllWithFilters(variantId, type, start, end, pageable)
        return PageResponse.from(page.map { it.toResponse() })
    }

    @Transactional
    fun adjust(request: StockAdjustmentRequest, userEmail: String): StockMovementResponse {
        if (request.quantity == 0) {
            throw BusinessException("A quantidade do ajuste não pode ser zero.")
        }

        val variant = variantRepository.findById(request.variantId)
            .orElseThrow { EntityNotFoundException("Variação", request.variantId) }

        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { EntityNotFoundException("Usuário", userEmail) }

        val newStock = variant.stockQuantity + request.quantity
        if (newStock < 0) {
            throw BusinessException(
                "Ajuste resultaria em estoque negativo para ${variant.sku}. " +
                    "Disponível: ${variant.stockQuantity}, ajuste: ${request.quantity}.",
            )
        }

        variant.stockQuantity = newStock
        variantRepository.save(variant)

        val movement = stockMovementRepository.save(
            StockMovement(
                variant = variant,
                type = StockMovementType.ADJUSTMENT,
                quantity = request.quantity,
                referenceType = "MANUAL",
                notes = request.notes.trim(),
                user = user,
            )
        )
        return movement.toResponse()
    }

    /**
     * Registra a saída de estoque de uma venda (movimentação SALE, quantidade negativa).
     * Deve ser chamado dentro da transação da venda. Assume que a validação de estoque
     * suficiente já foi feita pelo chamador.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    fun registerSaleExit(variant: ProductVariant, quantity: Int, saleId: UUID, user: User) {
        variant.stockQuantity -= quantity
        variantRepository.save(variant)
        stockMovementRepository.save(
            StockMovement(
                variant = variant,
                type = StockMovementType.SALE,
                quantity = -quantity,
                referenceType = "SALE",
                referenceId = saleId,
                user = user,
            )
        )
    }

    /**
     * Estorna o estoque de uma venda cancelada (movimentação ADJUSTMENT positiva).
     * Deve ser chamado dentro da transação do cancelamento.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    fun registerSaleReversal(variant: ProductVariant, quantity: Int, saleId: UUID, user: User) {
        variant.stockQuantity += quantity
        variantRepository.save(variant)
        stockMovementRepository.save(
            StockMovement(
                variant = variant,
                type = StockMovementType.ADJUSTMENT,
                quantity = quantity,
                referenceType = "SALE_CANCEL",
                referenceId = saleId,
                notes = "Estorno de cancelamento de venda",
                user = user,
            )
        )
    }
}
