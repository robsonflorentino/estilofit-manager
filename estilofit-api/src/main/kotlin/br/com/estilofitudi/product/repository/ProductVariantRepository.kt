package br.com.estilofitudi.product.repository

import br.com.estilofitudi.product.domain.ProductVariant
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
}
