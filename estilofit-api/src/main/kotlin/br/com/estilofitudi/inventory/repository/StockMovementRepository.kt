package br.com.estilofitudi.inventory.repository

import br.com.estilofitudi.inventory.domain.StockMovement
import br.com.estilofitudi.inventory.domain.StockMovementType
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
interface StockMovementRepository : JpaRepository<StockMovement, UUID> {

    @EntityGraph(attributePaths = ["variant", "variant.product", "user"])
    @Query("""
        SELECT m FROM StockMovement m
        WHERE (:variantId IS NULL OR m.variant.id = :variantId)
          AND (:type IS NULL OR m.type = :type)
          AND (CAST(:start AS timestamp) IS NULL OR m.createdAt >= :start)
          AND (CAST(:end AS timestamp) IS NULL OR m.createdAt <= :end)
    """)
    fun findAllWithFilters(
        @Param("variantId") variantId: UUID?,
        @Param("type") type: StockMovementType?,
        @Param("start") start: LocalDateTime?,
        @Param("end") end: LocalDateTime?,
        pageable: Pageable,
    ): Page<StockMovement>
}
