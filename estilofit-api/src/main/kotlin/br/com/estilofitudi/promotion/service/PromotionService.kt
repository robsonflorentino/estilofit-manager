package br.com.estilofitudi.promotion.service

import br.com.estilofitudi.inventory.service.SettingsReader
import br.com.estilofitudi.product.repository.ProductVariantRepository
import br.com.estilofitudi.product.repository.StaleProductRow
import br.com.estilofitudi.promotion.dto.StaleProductResponse
import br.com.estilofitudi.promotion.dto.StalePromotionResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
@Transactional(readOnly = true)
class PromotionService(
    private val variantRepository: ProductVariantRepository,
    private val settingsReader: SettingsReader,
) {

    /**
     * Lista variações "paradas": sem venda confirmada há mais de [days] dias.
     * Para variações que nunca venderam, a referência é a data da primeira entrada
     * em estoque (fallback: data de criação da variação).
     */
    fun stale(days: Int?): StalePromotionResponse {
        val threshold = days ?: settingsReader.promotionAlertDays()
        val now = LocalDateTime.now()

        val items = variantRepository.findStaleCandidates()
            .map { row -> toResponse(row, now) }
            .filter { it.daysStale >= threshold }
            .sortedByDescending { it.daysStale }

        val totalStockValue = items.fold(BigDecimal.ZERO) { acc, it -> acc + it.stockValue }

        return StalePromotionResponse(
            thresholdDays = threshold,
            staleCount = items.size,
            totalStockValue = totalStockValue,
            items = items,
        )
    }

    private fun toResponse(row: StaleProductRow, now: LocalDateTime): StaleProductResponse {
        val neverSold = row.lastSaleAt == null
        // Referência para "parado": última venda; se nunca vendeu, a entrada em estoque
        val reference: LocalDateTime = row.lastSaleAt
            ?: row.firstEntryAt
            ?: row.variantCreatedAt
            ?: now
        val daysStale = ChronoUnit.DAYS.between(reference, now).coerceAtLeast(0)

        val avgCost = row.averageCost ?: BigDecimal.ZERO
        val stockValue = avgCost.multiply(BigDecimal(row.stockQuantity))

        return StaleProductResponse(
            variantId = row.variantId,
            sku = row.sku,
            productName = row.productName,
            size = row.size,
            color = row.color,
            stockQuantity = row.stockQuantity,
            salePrice = row.salePrice,
            averageCost = row.averageCost,
            lastSaleAt = row.lastSaleAt,
            daysStale = daysStale,
            neverSold = neverSold,
            stockValue = stockValue,
        )
    }
}
