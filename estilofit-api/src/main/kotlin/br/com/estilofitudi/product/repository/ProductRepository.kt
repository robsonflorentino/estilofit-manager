package br.com.estilofitudi.product.repository

import br.com.estilofitudi.product.domain.Product
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ProductRepository : JpaRepository<Product, UUID> {

    /**
     * Carrega as variações junto (EntityGraph) para que variantCount e totalStock
     * possam ser calculados no mapeamento sem lazy loading fora da transação.
     */
    @EntityGraph(attributePaths = ["variants", "category"])
    @Query("""
        SELECT p FROM Product p
        WHERE (:name = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))
          AND (:categoryId IS NULL OR p.category.id = :categoryId)
          AND (:active IS NULL OR p.active = :active)
    """)
    fun findAllWithFilters(
        @Param("name") name: String,
        @Param("categoryId") categoryId: UUID?,
        @Param("active") active: Boolean?,
        pageable: Pageable,
    ): Page<Product>
}
