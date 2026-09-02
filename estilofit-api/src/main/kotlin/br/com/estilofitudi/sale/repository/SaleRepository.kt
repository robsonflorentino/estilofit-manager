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
               ), 0)                           AS cost,
               COALESCE(SUM(s.commissionAmount), 0) AS commission
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

    // ── Relatórios (período [start, end), apenas vendas confirmadas) ──────────

    /** Faturamento e número de vendas por dia. */
    @Query("""
        SELECT CAST(s.confirmedAt AS date) AS day,
               COALESCE(SUM(s.finalAmount), 0) AS revenue,
               COUNT(s) AS saleCount
        FROM Sale s
        WHERE s.status = br.com.estilofitudi.sale.domain.SaleStatus.CONFIRMED
          AND s.confirmedAt >= :start AND s.confirmedAt < :end
        GROUP BY CAST(s.confirmedAt AS date)
        ORDER BY CAST(s.confirmedAt AS date) ASC
    """)
    fun revenueByDay(
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime,
    ): List<DailyRevenueRow>

    /** Faturamento e número de vendas por canal. */
    @Query("""
        SELECT s.channel.name AS label,
               COALESCE(SUM(s.finalAmount), 0) AS revenue,
               COUNT(s) AS saleCount
        FROM Sale s
        WHERE s.status = br.com.estilofitudi.sale.domain.SaleStatus.CONFIRMED
          AND s.confirmedAt >= :start AND s.confirmedAt < :end
        GROUP BY s.channel.name
        ORDER BY SUM(s.finalAmount) DESC
    """)
    fun revenueByChannel(
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime,
    ): List<GroupRevenueRow>

    /** Faturamento e número de vendas por forma de pagamento. */
    @Query("""
        SELECT CAST(s.paymentMethod AS string) AS label,
               COALESCE(SUM(s.finalAmount), 0) AS revenue,
               COUNT(s) AS saleCount
        FROM Sale s
        WHERE s.status = br.com.estilofitudi.sale.domain.SaleStatus.CONFIRMED
          AND s.confirmedAt >= :start AND s.confirmedAt < :end
        GROUP BY s.paymentMethod
        ORDER BY SUM(s.finalAmount) DESC
    """)
    fun revenueByPayment(
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime,
    ): List<GroupRevenueRow>

    /**
     * Faturamento e custo das mercadorias vendidas agrupados por ano/mês, no período.
     * Base do relatório de meta de vendas. Apenas vendas confirmadas.
     */
    @Query("""
        SELECT EXTRACT(YEAR FROM s.confirmedAt) AS year,
               EXTRACT(MONTH FROM s.confirmedAt) AS month,
               COALESCE(SUM(s.finalAmount), 0) AS revenue,
               COALESCE(SUM(
                   (SELECT COALESCE(SUM(i.quantity * COALESCE(i.variant.averageCost, 0)), 0)
                    FROM SaleItem i WHERE i.sale = s)
               ), 0) AS cost,
               COALESCE(SUM(s.commissionAmount), 0) AS commission
        FROM Sale s
        WHERE s.status = br.com.estilofitudi.sale.domain.SaleStatus.CONFIRMED
          AND s.confirmedAt >= :start AND s.confirmedAt < :end
        GROUP BY EXTRACT(YEAR FROM s.confirmedAt), EXTRACT(MONTH FROM s.confirmedAt)
        ORDER BY EXTRACT(YEAR FROM s.confirmedAt) ASC, EXTRACT(MONTH FROM s.confirmedAt) ASC
    """)
    fun monthlyAggregate(
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime,
    ): List<MonthlyAggregateRow>

    /**
     * Faturamento, custo das mercadorias vendidas e nº de vendas por canal, no período.
     * Base do relatório de lucratividade por canal. Apenas vendas confirmadas.
     */
    @Query("""
        SELECT s.channel.name AS label,
               COALESCE(SUM(s.finalAmount), 0) AS revenue,
               COALESCE(SUM(
                   (SELECT COALESCE(SUM(i.quantity * COALESCE(i.variant.averageCost, 0)), 0)
                    FROM SaleItem i WHERE i.sale = s)
               ), 0) + COALESCE(SUM(s.commissionAmount), 0) AS cost,
               COUNT(s) AS saleCount
        FROM Sale s
        WHERE s.status = br.com.estilofitudi.sale.domain.SaleStatus.CONFIRMED
          AND s.confirmedAt >= :start AND s.confirmedAt < :end
        GROUP BY s.channel.name
    """)
    fun profitByChannel(
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime,
    ): List<ChannelProfitRow>

    /**
     * Ranking de vendedores por faturamento no período (apenas vendas confirmadas).
     */
    @Query("""
        SELECT s.seller.id AS sellerId,
               s.seller.name AS sellerName,
               COALESCE(SUM(s.finalAmount), 0) AS revenue,
               COUNT(s) AS saleCount
        FROM Sale s
        WHERE s.status = br.com.estilofitudi.sale.domain.SaleStatus.CONFIRMED
          AND s.confirmedAt >= :start AND s.confirmedAt < :end
        GROUP BY s.seller.id, s.seller.name
        ORDER BY SUM(s.finalAmount) DESC
    """)
    fun sellerRanking(
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime,
    ): List<SellerRankingRow>

    /**
     * Comissão a pagar por vendedor no período (soma dos snapshots de comissão das
     * vendas confirmadas). Só vendedores com alguma comissão aparecem.
     */
    @Query("""
        SELECT s.seller.id AS sellerId,
               s.seller.name AS sellerName,
               COALESCE(SUM(s.finalAmount), 0) AS revenue,
               COALESCE(SUM(s.commissionAmount), 0) AS commissionAmount,
               COUNT(s) AS saleCount
        FROM Sale s
        WHERE s.status = br.com.estilofitudi.sale.domain.SaleStatus.CONFIRMED
          AND s.confirmedAt >= :start AND s.confirmedAt < :end
        GROUP BY s.seller.id, s.seller.name
        HAVING SUM(s.commissionAmount) > 0
        ORDER BY SUM(s.commissionAmount) DESC
    """)
    fun commissionBySeller(
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime,
    ): List<SellerCommissionRow>
}

/** Comissão agregada por vendedor. */
interface SellerCommissionRow {
    val sellerId: java.util.UUID
    val sellerName: String
    val revenue: java.math.BigDecimal
    val commissionAmount: java.math.BigDecimal
    val saleCount: Long
}

/** Faturamento e nº de vendas agregados por vendedor. */
interface SellerRankingRow {
    val sellerId: java.util.UUID
    val sellerName: String
    val revenue: java.math.BigDecimal
    val saleCount: Long
}

/** Faturamento, custo e nº de vendas agregados por canal. */
interface ChannelProfitRow {
    val label: String
    val revenue: java.math.BigDecimal
    val cost: java.math.BigDecimal
    val saleCount: Long
}

/** Faturamento e custo agregados por ano/mês. */
interface MonthlyAggregateRow {
    val year: Int
    val month: Int
    val revenue: java.math.BigDecimal
    val cost: java.math.BigDecimal
    val commission: java.math.BigDecimal
}

/** Projeção dos agregados de vendas para os KPIs do dashboard. */
interface SalesAggregate {
    val revenue: java.math.BigDecimal
    val saleCount: Long
    val cost: java.math.BigDecimal
    val commission: java.math.BigDecimal
}

/** Faturamento diário para o relatório de série temporal. */
interface DailyRevenueRow {
    val day: java.time.LocalDate
    val revenue: java.math.BigDecimal
    val saleCount: Long
}

/** Faturamento agrupado (por canal ou por forma de pagamento). */
interface GroupRevenueRow {
    val label: String
    val revenue: java.math.BigDecimal
    val saleCount: Long
}
