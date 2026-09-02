package br.com.estilofitudi.product.domain

import br.com.estilofitudi.category.domain.Category
import br.com.estilofitudi.shared.domain.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "products")
class Product(

    @Column(name = "name", nullable = false, length = 200)
    var name: String,

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    var category: Category,

    @Column(name = "active", nullable = false)
    var active: Boolean = true,

    @OneToMany(mappedBy = "product", cascade = [CascadeType.ALL], orphanRemoval = false, fetch = FetchType.LAZY)
    val variants: MutableList<ProductVariant> = mutableListOf(),

) : BaseEntity()
