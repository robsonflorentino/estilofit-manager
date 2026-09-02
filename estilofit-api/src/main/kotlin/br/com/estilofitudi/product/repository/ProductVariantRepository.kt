package br.com.estilofitudi.product.repository

import br.com.estilofitudi.product.domain.ProductVariant
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ProductVariantRepository : JpaRepository<ProductVariant, UUID> {

    fun existsByProductIdAndSizeIgnoreCaseAndColorIgnoreCase(
        productId: UUID,
        size: String,
        color: String,
    ): Boolean

    fun existsBySku(sku: String): Boolean

    /** Conta variações cujo SKU começa com o prefixo (ex: "BLU-") para gerar o próximo sequencial. */
    @Query("SELECT COUNT(v) FROM ProductVariant v WHERE v.sku LIKE CONCAT(:prefix, '%')")
    fun countBySkuPrefix(@Param("prefix") prefix: String): Long

    /**
     * Resumo de estoque por variação, com produto e categoria carregados.
     * Filtros opcionais: produto, categoria, tamanho, cor e apenas estoque baixo (< threshold).
     */
    @EntityGraph(attributePaths = ["product", "product.category"])
    @Query("""
        SELECT v FROM ProductVariant v
        WHERE (:productId IS NULL OR v.product.id = :productId)
          AND (:categoryId IS NULL OR v.product.category.id = :categoryId)
          AND (:variantSize = '' OR LOWER(v.size) = LOWER(:variantSize))
          AND (:color = '' OR LOWER(v.color) = LOWER(:color))
          AND (:lowStockThreshold IS NULL OR v.stockQuantity < :lowStockThreshold)
        ORDER BY v.product.name ASC, v.sku ASC
    """)
    fun findStockSummary(
        @Param("productId") productId: UUID?,
        @Param("categoryId") categoryId: UUID?,
        @Param("variantSize") variantSize: String,
        @Param("color") color: String,
        @Param("lowStockThreshold") lowStockThreshold: Int?,
        pageable: Pageable,
    ): Page<ProductVariant>

    /** Total de itens em estoque (soma das quantidades das variações ativas) — KPI do dashboard. */
    @Query("SELECT COALESCE(SUM(v.stockQuantity), 0) FROM ProductVariant v WHERE v.active = true")
    fun sumActiveStock(): Long

    /**
     * Variações ativas com estoque disponível, com a data da última venda confirmada
     * (lastSaleAt, nulo se nunca vendeu) e a data da primeira entrada em estoque
     * (firstEntryAt, via movimentação ENTRY). Base do relatório de produtos parados.
     */
    @Query("""
        SELECT v.id AS variantId,
               v.sku AS sku,
               v.product.name AS productName,
               v.size AS size,
               v.color AS color,
               v.stockQuantity AS stockQuantity,
               v.salePrice AS salePrice,
               v.averageCost AS averageCost,
               (SELECT MAX(s.confirmedAt) FROM SaleItem i JOIN i.sale s
                 WHERE i.variant = v
                   AND s.status = br.com.estilofitudi.sale.domain.SaleStatus.CONFIRMED) AS lastSaleAt,
               (SELECT MIN(m.createdAt) FROM StockMovement m
                 WHERE m.variant = v
                   AND m.type = br.com.estilofitudi.inventory.domain.StockMovementType.ENTRY) AS firstEntryAt,
               v.createdAt AS variantCreatedAt
        FROM ProductVariant v
        WHERE v.active = true AND v.stockQuantity > 0
    """)
    fun findStaleCandidates(): List<StaleProductRow>
}

/** Projeção de candidata a promoção (variação parada). */
interface StaleProductRow {
    val variantId: UUID
    val sku: String
    val productName: String
    val size: String
    val color: String
    val stockQuantity: Int
    val salePrice: java.math.BigDecimal?
    val averageCost: java.math.BigDecimal?
    val lastSaleAt: java.time.LocalDateTime?
    val firstEntryAt: java.time.LocalDateTime?
    val variantCreatedAt: java.time.LocalDateTime?
}
