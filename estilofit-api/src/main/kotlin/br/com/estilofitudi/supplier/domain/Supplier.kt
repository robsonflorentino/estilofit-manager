package br.com.estilofitudi.supplier.domain

import br.com.estilofitudi.shared.domain.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "suppliers",
    uniqueConstraints = [UniqueConstraint(name = "uq_suppliers_cnpj", columnNames = ["cnpj"])],
)
class Supplier(

    @Column(name = "name", nullable = false, length = 200)
    var name: String,

    @Column(name = "contact_phone", length = 20)
    var contactPhone: String? = null,

    @Column(name = "contact_email", length = 200)
    var contactEmail: String? = null,

    @Column(name = "whatsapp", length = 20)
    var whatsapp: String? = null,

    @Column(name = "cnpj", length = 18)
    var cnpj: String? = null,

    @Column(name = "address", columnDefinition = "TEXT")
    var address: String? = null,

    @Column(name = "notes", columnDefinition = "TEXT")
    var notes: String? = null,

    @Column(name = "active", nullable = false)
    var active: Boolean = true,

) : BaseEntity()
