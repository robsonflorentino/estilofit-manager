package br.com.estilofitudi.sale.domain

import br.com.estilofitudi.shared.domain.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "sale_channels",
    uniqueConstraints = [UniqueConstraint(name = "uq_sale_channels_name", columnNames = ["name"])],
)
class SaleChannel(

    @Column(name = "name", nullable = false, unique = true, length = 100)
    var name: String,

    @Column(name = "active", nullable = false)
    var active: Boolean = true,

) : BaseEntity()
