package br.com.estilofitudi.product.domain

import br.com.estilofitudi.shared.domain.BaseEntity
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(
    name = "product_variants",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_product_variants_sku", columnNames = ["sku"]),
        UniqueConstraint(
            name = "uq_product_variants_product_size_color",
            columnNames = ["product_id", "size", "color"],
        ),
    ],
)
class ProductVariant(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product,

    @Column(name = "sku", nullable = false, unique = true, length = 50)
    var sku: String,

    @Column(name = "size", nullable = false, length = 10)
    var size: String,

    @Column(name = "color", nullable = false, length = 50)
    var color: String,

    @Column(name = "profit_margin", precision = 5, scale = 2)
    var profitMargin: BigDecimal? = null,

    @Column(name = "sale_price", precision = 10, scale = 2)
    var salePrice: BigDecimal? = null,

    @Column(name = "average_cost", precision = 10, scale = 2)
    var averageCost: BigDecimal? = null,

    @Column(name = "stock_quantity", nullable = false)
    var stockQuantity: Int = 0,

    @Column(name = "active", nullable = false)
    var active: Boolean = true,

) : BaseEntity()
