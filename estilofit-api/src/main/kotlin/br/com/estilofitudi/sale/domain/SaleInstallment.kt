package br.com.estilofitudi.sale.domain

import br.com.estilofitudi.user.domain.User
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(
    name = "sale_installments",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_sale_installments_sale_num", columnNames = ["sale_id", "installment_num"]),
    ],
)
class SaleInstallment(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    var sale: Sale,

    @Column(name = "installment_num", nullable = false)
    var installmentNum: Int,

    @Column(name = "due_date", nullable = false)
    var dueDate: LocalDate,

    @Column(name = "gross_amount", nullable = false, precision = 10, scale = 2)
    var grossAmount: BigDecimal,

    @Column(name = "net_amount", nullable = false, precision = 10, scale = 2)
    var netAmount: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: InstallmentStatus = InstallmentStatus.PENDING,

    @Column(name = "received_at")
    var receivedAt: LocalDateTime? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by")
    var receivedBy: User? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID()
}
