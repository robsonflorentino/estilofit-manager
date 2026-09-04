package br.com.estilofitudi.sale.domain

import br.com.estilofitudi.product.domain.ProductVariant
import jakarta.persistence.*
import java.math.BigDecimal
import java.util.*

@Entity
@Table(name = "sale_items")
class SaleItem(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    var sale: Sale,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    var variant: ProductVariant,

    @Column(name = "quantity", nullable = false)
    var quantity: Int,

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    var unitPrice: BigDecimal,

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    var totalPrice: BigDecimal,

    // Custo médio da variação congelado no momento da venda. O lucro histórico usa
    // este valor (não o custo atual), para que correções de custo não afetem vendas passadas.
    @Column(name = "unit_cost", nullable = false, precision = 10, scale = 2)
    var unitCost: BigDecimal = BigDecimal.ZERO,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID()
}
