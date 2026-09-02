package br.com.estilofitudi.sale.repository

import br.com.estilofitudi.sale.domain.InstallmentStatus
import br.com.estilofitudi.sale.domain.SaleInstallment
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.*

@Repository
interface SaleInstallmentRepository : JpaRepository<SaleInstallment, UUID> {

    @EntityGraph(attributePaths = ["sale"])
    @Query("""
        SELECT i FROM SaleInstallment i
        WHERE (:status IS NULL OR i.status = :status)
          AND (:saleId IS NULL OR i.sale.id = :saleId)
          AND (CAST(:startDue AS date) IS NULL OR i.dueDate >= :startDue)
          AND (CAST(:endDue AS date) IS NULL OR i.dueDate <= :endDue)
        ORDER BY i.dueDate ASC
    """)
    fun findAllWithFilters(
        @Param("status") status: InstallmentStatus?,
        @Param("saleId") saleId: UUID?,
        @Param("startDue") startDue: LocalDate?,
        @Param("endDue") endDue: LocalDate?,
        pageable: Pageable,
    ): Page<SaleInstallment>

    /** Parcelas pendentes num intervalo, para o fluxo projetado. */
    @EntityGraph(attributePaths = ["sale"])
    @Query("""
        SELECT i FROM SaleInstallment i
        WHERE i.status = 'PENDING'
          AND i.dueDate >= :startDue
          AND i.dueDate <= :endDue
        ORDER BY i.dueDate ASC
    """)
    fun findPendingBetween(
        @Param("startDue") startDue: LocalDate,
        @Param("endDue") endDue: LocalDate,
    ): List<SaleInstallment>
}
