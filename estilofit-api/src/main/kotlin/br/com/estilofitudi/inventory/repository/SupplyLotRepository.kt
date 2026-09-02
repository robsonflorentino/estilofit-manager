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

    /**
     * Fornecedor de cada item de lote com a data do lote — base para descobrir o
     * "último fornecedor" de uma variação (o de maior receivedAt, resolvido no service).
     * Ordenado por data desc para facilitar o "primeiro encontrado por variação".
     */
    @Query("""
        SELECT i.variant.id AS variantId,
               l.supplier.id AS supplierId,
               l.supplier.name AS supplierName,
               l.receivedAt AS receivedAt
        FROM SupplyLotItem i JOIN i.lot l
        ORDER BY l.receivedAt DESC, l.createdAt DESC
    """)
    fun variantSupplierHistory(): List<VariantSupplierRow>
}

/** Fornecedor de uma variação num lote (para resolver o último fornecedor). */
interface VariantSupplierRow {
    val variantId: java.util.UUID
    val supplierId: java.util.UUID
    val supplierName: String
    val receivedAt: LocalDate
}
