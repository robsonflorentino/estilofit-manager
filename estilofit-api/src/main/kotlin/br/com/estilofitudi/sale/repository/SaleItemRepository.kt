package br.com.estilofitudi.sale.repository

import br.com.estilofitudi.sale.domain.SaleItem
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface SaleItemRepository : JpaRepository<SaleItem, UUID> {

    /**
     * Ranking de variações mais vendidas no período, considerando apenas vendas confirmadas.
     * Ordenado por quantidade vendida (desc). Use Pageable para limitar (ex.: top 10).
     */
    @Query("""
        SELECT i.variant.id AS variantId,
               i.variant.sku AS sku,
               i.variant.product.name AS productName,
               i.variant.size AS size,
               i.variant.color AS color,
               COALESCE(SUM(i.quantity), 0) AS quantity,
               COALESCE(SUM(i.totalPrice), 0) AS revenue
        FROM SaleItem i
        WHERE i.sale.status = br.com.estilofitudi.sale.domain.SaleStatus.CONFIRMED
          AND i.sale.confirmedAt >= :start AND i.sale.confirmedAt < :end
        GROUP BY i.variant.id, i.variant.sku, i.variant.product.name, i.variant.size, i.variant.color
        ORDER BY SUM(i.quantity) DESC
    """)
    fun topProducts(
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime,
        pageable: Pageable,
    ): List<TopProductRow>

    /**
     * Vendas por variação no período (apenas confirmadas), junto com a posição atual de
     * estoque. Base da sugestão de compra. Considera só variações ativas que venderam
     * (soldQty > 0). O custo médio acompanha para estimar o valor do lote sugerido.
     */
    @Query("""
        SELECT i.variant.id AS variantId,
               i.variant.sku AS sku,
               i.variant.product.name AS productName,
               i.variant.size AS size,
               i.variant.color AS color,
               i.variant.stockQuantity AS stockQuantity,
               i.variant.averageCost AS averageCost,
               COALESCE(SUM(i.quantity), 0) AS soldQty
        FROM SaleItem i
        WHERE i.sale.status = br.com.estilofitudi.sale.domain.SaleStatus.CONFIRMED
          AND i.sale.confirmedAt >= :start AND i.sale.confirmedAt < :end
          AND i.variant.active = true
        GROUP BY i.variant.id, i.variant.sku, i.variant.product.name,
                 i.variant.size, i.variant.color, i.variant.stockQuantity, i.variant.averageCost
        HAVING SUM(i.quantity) > 0
    """)
    fun salesForPurchaseSuggestion(
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime,
    ): List<PurchaseSuggestionRow>
}

/** Linha do ranking de produtos mais vendidos. */
interface TopProductRow {
    val variantId: UUID
    val sku: String
    val productName: String
    val size: String
    val color: String
    val quantity: Long
    val revenue: java.math.BigDecimal
}

/** Vendas + estoque de uma variação, para a sugestão de compra. */
interface PurchaseSuggestionRow {
    val variantId: UUID
    val sku: String
    val productName: String
    val size: String
    val color: String
    val stockQuantity: Int
    val averageCost: java.math.BigDecimal?
    val soldQty: Long
}
