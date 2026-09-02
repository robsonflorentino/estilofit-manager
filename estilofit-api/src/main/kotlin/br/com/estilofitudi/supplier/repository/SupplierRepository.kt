package br.com.estilofitudi.supplier.repository

import br.com.estilofitudi.supplier.domain.Supplier
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SupplierRepository : JpaRepository<Supplier, UUID> {

    fun existsByCnpj(cnpj: String): Boolean

    fun existsByCnpjAndIdNot(cnpj: String, id: UUID): Boolean

    @Query("""
        SELECT s FROM Supplier s
        WHERE (:name = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%')))
          AND (:active IS NULL OR s.active = :active)
    """)
    fun findAllWithFilters(
        @Param("name") name: String,
        @Param("active") active: Boolean?,
        pageable: Pageable,
    ): Page<Supplier>
}
