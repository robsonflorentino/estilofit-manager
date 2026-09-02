package br.com.estilofitudi.commission.service

import br.com.estilofitudi.commission.dto.CommissionReportResponse
import br.com.estilofitudi.commission.dto.SellerCommissionResponse
import br.com.estilofitudi.sale.repository.SaleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class CommissionService(
    private val saleRepository: SaleRepository,
) {

    /** Comissões a pagar por vendedor no período (inclusivo). */
    fun report(startDate: LocalDate, endDate: LocalDate): CommissionReportResponse {
        val start = startDate.atStartOfDay()
        val end = endDate.plusDays(1).atStartOfDay()

        val sellers = saleRepository.commissionBySeller(start, end).map { r ->
            SellerCommissionResponse(
                sellerId = r.sellerId,
                sellerName = r.sellerName,
                revenue = r.revenue.setScale(2, RoundingMode.HALF_UP),
                commissionAmount = r.commissionAmount.setScale(2, RoundingMode.HALF_UP),
                saleCount = r.saleCount,
            )
        }
        val total = sellers.fold(BigDecimal.ZERO) { acc, s -> acc + s.commissionAmount }

        return CommissionReportResponse(
            totalCommission = total.setScale(2, RoundingMode.HALF_UP),
            sellers = sellers,
        )
    }
}
