package br.com.estilofitudi.settings.domain

import br.com.estilofitudi.user.domain.User
import jakarta.persistence.*
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime
import java.util.*

/**
 * Configuração do sistema (par chave/valor). A tabela `system_settings` (V11) tem
 * `updated_by` (FK usuário) e `updated_at`, mas não `created_at` — por isso não estende BaseEntity.
 */
@Entity
@Table(
    name = "system_settings",
    uniqueConstraints = [UniqueConstraint(name = "uq_system_settings_key", columnNames = ["key"])],
)
class SystemSetting(

    @Column(name = "key", nullable = false, unique = true, length = 100)
    var key: String,

    @Column(name = "value", nullable = false, length = 500)
    var value: String,

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    var updatedBy: User? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID()

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
}
