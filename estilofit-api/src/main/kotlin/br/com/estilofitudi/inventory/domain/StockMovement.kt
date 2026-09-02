package br.com.estilofitudi.inventory.domain

import br.com.estilofitudi.product.domain.ProductVariant
import br.com.estilofitudi.user.domain.User
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "stock_movements")
class StockMovement(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    var variant: ProductVariant,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    var type: StockMovementType,

    /** Positivo = entrada, negativo = saída. */
    @Column(name = "quantity", nullable = false)
    var quantity: Int,

    @Column(name = "reference_type", length = 20)
    var referenceType: String? = null,

    @Column(name = "reference_id")
    var referenceId: UUID? = null,

    @Column(name = "notes", columnDefinition = "TEXT")
    var notes: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
