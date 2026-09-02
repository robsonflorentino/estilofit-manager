package br.com.estilofitudi.inventory.repository

import br.com.estilofitudi.inventory.domain.SupplyLot
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
interface SupplyLotRepository : JpaRepository<SupplyLot, UUID> {

    @EntityGraph(attributePaths = ["supplier", "createdBy", "items"])
    @Query("""
        SELECT l FROM SupplyLot l
        WHERE (:supplierId IS NULL OR l.supplier.id = :supplierId)
          AND (CAST(:startDate AS date) IS NULL OR l.receivedAt >= :startDate)
          AND (CAST(:endDate AS date) IS NULL OR l.receivedAt <= :endDate)
    """)
    fun findAllWithFilters(
        @Param("supplierId") supplierId: UUID?,
        @Param("startDate") startDate: LocalDate?,
        @Param("endDate") endDate: LocalDate?,
        pageable: Pageable,
    ): Page<SupplyLot>

    @EntityGraph(attributePaths = ["supplier", "createdBy", "items", "items.variant"])
    override fun findById(id: UUID): Optional<SupplyLot>
}
