package br.com.estilofitudi.user.domain

import br.com.estilofitudi.shared.domain.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "users",
    indexes = [Index(name = "idx_users_email", columnList = "email", unique = true)],
)
class User(

    @Column(name = "name", nullable = false, length = 200)
    var name: String,

    @Column(name = "email", nullable = false, unique = true, length = 200)
    var email: String,

    @Column(name = "password_hash", nullable = false, length = 255)
    var passwordHash: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    var role: Role,

    @Column(name = "active", nullable = false)
    var active: Boolean = true,

) : BaseEntity()
