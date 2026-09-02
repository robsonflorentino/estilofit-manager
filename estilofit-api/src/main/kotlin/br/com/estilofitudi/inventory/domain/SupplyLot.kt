package br.com.estilofitudi.inventory.domain

import br.com.estilofitudi.shared.domain.BaseEntity
import br.com.estilofitudi.supplier.domain.Supplier
import br.com.estilofitudi.user.domain.User
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "supply_lots")
class SupplyLot(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    var supplier: Supplier,

    @Column(name = "received_at", nullable = false)
    var receivedAt: LocalDate,

    @Column(name = "freight_cost", nullable = false, precision = 10, scale = 2)
    var freightCost: BigDecimal = BigDecimal.ZERO,

    @Column(name = "total_cost", nullable = false, precision = 10, scale = 2)
    var totalCost: BigDecimal = BigDecimal.ZERO,

    @Column(name = "notes", columnDefinition = "TEXT")
    var notes: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    var createdBy: User,

    @OneToMany(mappedBy = "lot", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val items: MutableList<SupplyLotItem> = mutableListOf(),

) : BaseEntity()
