package br.com.estilofitudi.sale.domain

import br.com.estilofitudi.user.domain.User
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "sales")
class Sale(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    var channel: SaleChannel,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    var seller: User,

    @Column(name = "confirmed_at", nullable = false)
    var confirmedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    var totalAmount: BigDecimal,

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    var discountAmount: BigDecimal = BigDecimal.ZERO,

    @Column(name = "final_amount", nullable = false, precision = 10, scale = 2)
    var finalAmount: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    var paymentMethod: PaymentMethod,

    @Column(name = "installments", nullable = false)
    var installments: Int = 1,

    @Column(name = "card_fee_pct", precision = 5, scale = 2)
    var cardFeePct: BigDecimal? = null,

    @Column(name = "card_fee_passed")
    var cardFeePassed: Boolean = true,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: SaleStatus = SaleStatus.CONFIRMED,

    @Column(name = "notes", columnDefinition = "TEXT")
    var notes: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    var cancelledBy: User? = null,

    @Column(name = "cancelled_at")
    var cancelledAt: LocalDateTime? = null,

    @OneToMany(mappedBy = "sale", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val items: MutableList<SaleItem> = mutableListOf(),

    @OneToMany(mappedBy = "sale", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val installmentSchedule: MutableList<SaleInstallment> = mutableListOf(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID()

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
}
