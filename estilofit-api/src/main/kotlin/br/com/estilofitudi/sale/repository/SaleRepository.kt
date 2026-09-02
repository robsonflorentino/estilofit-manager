package br.com.estilofitudi.sale.repository

import br.com.estilofitudi.sale.domain.PaymentMethod
import br.com.estilofitudi.sale.domain.Sale
import br.com.estilofitudi.sale.domain.SaleStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface SaleRepository : JpaRepository<Sale, UUID> {

    @EntityGraph(attributePaths = ["channel", "seller"])
    @Query("""
        SELECT s FROM Sale s
        WHERE (:channelId IS NULL OR s.channel.id = :channelId)
          AND (:paymentMethod IS NULL OR s.paymentMethod = :paymentMethod)
          AND (:status IS NULL OR s.status = :status)
          AND (:sellerId IS NULL OR s.seller.id = :sellerId)
          AND (CAST(:startDate AS timestamp) IS NULL OR s.confirmedAt >= :startDate)
          AND (CAST(:endDate AS timestamp) IS NULL OR s.confirmedAt <= :endDate)
    """)
    fun findAllWithFilters(
        @Param("channelId") channelId: UUID?,
        @Param("paymentMethod") paymentMethod: PaymentMethod?,
        @Param("status") status: SaleStatus?,
        @Param("sellerId") sellerId: UUID?,
        @Param("startDate") startDate: LocalDateTime?,
        @Param("endDate") endDate: LocalDateTime?,
        pageable: Pageable,
    ): Page<Sale>

    @EntityGraph(attributePaths = ["channel", "seller", "items", "items.variant", "items.variant.product", "installmentSchedule"])
    override fun findById(id: UUID): Optional<Sale>

    /**
     * Agregados de vendas confirmadas num período, opcionalmente por vendedor.
     * - revenue: soma do finalAmount (faturamento líquido de desconto)
     * - saleCount: número de vendas
     * - cost: custo das mercadorias vendidas (quantidade × custo médio da variação)
     * Vendas canceladas são excluídas (status = CONFIRMED).
     */
    @Query("""
        SELECT COALESCE(SUM(s.finalAmount), 0) AS revenue,
               COUNT(s)                        AS saleCount,
               COALESCE(SUM(
                   (SELECT COALESCE(SUM(i.quantity * COALESCE(i.variant.averageCost, 0)), 0)
                    FROM SaleItem i WHERE i.sale = s)
               ), 0)                           AS cost
        FROM Sale s
        WHERE s.status = br.com.estilofitudi.sale.domain.SaleStatus.CONFIRMED
          AND s.confirmedAt >= :start
          AND s.confirmedAt < :end
          AND (:sellerId IS NULL OR s.seller.id = :sellerId)
    """)
    fun aggregateConfirmed(
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime,
        @Param("sellerId") sellerId: UUID?,
    ): SalesAggregate
}

/** Projeção dos agregados de vendas para os KPIs do dashboard. */
interface SalesAggregate {
    val revenue: java.math.BigDecimal
    val saleCount: Long
    val cost: java.math.BigDecimal
}
