package br.com.estilofitudi.inventory.domain

import br.com.estilofitudi.product.domain.ProductVariant
import jakarta.persistence.*
import java.math.BigDecimal
import java.util.*

@Entity
@Table(name = "supply_lot_items")
class SupplyLotItem(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id", nullable = false)
    var lot: SupplyLot,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    var variant: ProductVariant,

    @Column(name = "quantity", nullable = false)
    var quantity: Int,

    @Column(name = "unit_cost", nullable = false, precision = 10, scale = 2)
    var unitCost: BigDecimal,

    @Column(name = "freight_share", nullable = false, precision = 10, scale = 2)
    var freightShare: BigDecimal = BigDecimal.ZERO,

    @Column(name = "real_unit_cost", nullable = false, precision = 10, scale = 2)
    var realUnitCost: BigDecimal,
)
