package br.com.estilofitudi.sale.service

import br.com.estilofitudi.sale.domain.InstallmentStatus
import br.com.estilofitudi.sale.dto.*
import br.com.estilofitudi.sale.repository.SaleInstallmentRepository
import br.com.estilofitudi.shared.dto.PageResponse
import br.com.estilofitudi.shared.exception.BusinessException
import br.com.estilofitudi.shared.exception.EntityNotFoundException
import br.com.estilofitudi.user.repository.UserRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service
@Transactional(readOnly = true)
class InstallmentService(
    private val installmentRepository: SaleInstallmentRepository,
    private val userRepository: UserRepository,
) {

    fun findAll(
        status: InstallmentStatus?,
        saleId: UUID?,
        startDue: LocalDate?,
        endDue: LocalDate?,
        pageable: Pageable,
    ): PageResponse<InstallmentWithSaleResponse> {
        val page = installmentRepository.findAllWithFilters(status, saleId, startDue, endDue, pageable)
        return PageResponse.from(page.map { it.toWithSaleResponse() })
    }

    @Transactional
    fun receive(id: UUID, request: ReceiveInstallmentRequest, userEmail: String): InstallmentResponse {
        val installment = installmentRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Parcela", id) }

        if (installment.status != InstallmentStatus.PENDING) {
            throw BusinessException("Só é possível dar baixa em parcelas pendentes.")
        }

        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { EntityNotFoundException("Usuário", userEmail) }

        installment.status = InstallmentStatus.RECEIVED
        installment.receivedAt = (request.receivedAt ?: LocalDate.now()).atStartOfDay()
        installment.receivedBy = user

        return installmentRepository.save(installment).toResponse()
    }

    /** Fluxo de caixa projetado: parcelas pendentes agrupadas por mês de vencimento (RN-028). */
    fun projected(months: Int): List<ProjectedMonthResponse> {
        val today = LocalDate.now()
        val end = today.plusMonths(months.toLong())
        val pending = installmentRepository.findPendingBetween(today, end)

        val monthFmt = DateTimeFormatter.ofPattern("yyyy-MM")

        return pending
            .groupBy { it.dueDate.format(monthFmt) }
            .toSortedMap()
            .map { (month, list) ->
                ProjectedMonthResponse(
                    month = month,
                    totalGross = list.fold(BigDecimal.ZERO) { acc, i -> acc + i.grossAmount },
                    totalNet = list.fold(BigDecimal.ZERO) { acc, i -> acc + i.netAmount },
                    installments = list.map { it.toWithSaleResponse() },
                )
            }
    }
}
