package br.com.estilofitudi.category.domain

import br.com.estilofitudi.shared.domain.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "categories",
    indexes = [Index(name = "idx_categories_name", columnList = "name", unique = true)],
)
class Category(

    @Column(name = "name", nullable = false, unique = true, length = 100)
    var name: String,

    @Column(name = "active", nullable = false)
    var active: Boolean = true,

) : BaseEntity()
