package br.com.estilofitudi.report.service

import br.com.estilofitudi.report.dto.*
import br.com.estilofitudi.sale.repository.GroupRevenueRow
import br.com.estilofitudi.sale.repository.SaleItemRepository
import br.com.estilofitudi.sale.repository.SaleRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class ReportService(
    private val saleRepository: SaleRepository,
    private val saleItemRepository: SaleItemRepository,
) {

    /** Converte o intervalo de datas (inclusivo) em janela [start 00:00, endExclusive 00:00). */
    private fun window(startDate: LocalDate, endDate: LocalDate): Pair<LocalDateTime, LocalDateTime> {
        val start = startDate.atStartOfDay()
        val end = endDate.plusDays(1).atStartOfDay() // fim inclusivo -> exclusivo no dia seguinte
        return start to end
    }

    fun summary(startDate: LocalDate, endDate: LocalDate): ReportSummaryResponse {
        val (start, end) = window(startDate, endDate)
        val agg = saleRepository.aggregateConfirmed(start, end, null)

        val averageTicket = if (agg.saleCount > 0) {
            agg.revenue.divide(BigDecimal(agg.saleCount), 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO.setScale(2)
        }
        val profit = agg.revenue.subtract(agg.cost)

        return ReportSummaryResponse(
            revenue = agg.revenue.setScale(2, RoundingMode.HALF_UP),
            saleCount = agg.saleCount,
            averageTicket = averageTicket,
            estimatedProfit = profit.setScale(2, RoundingMode.HALF_UP),
        )
    }

    fun revenueByDay(startDate: LocalDate, endDate: LocalDate): List<DailyRevenueResponse> {
        val (start, end) = window(startDate, endDate)
        return saleRepository.revenueByDay(start, end).map {
            DailyRevenueResponse(it.day, it.revenue.setScale(2, RoundingMode.HALF_UP), it.saleCount)
        }
    }

    fun topProducts(startDate: LocalDate, endDate: LocalDate, limit: Int): List<TopProductResponse> {
        val (start, end) = window(startDate, endDate)
        return saleItemRepository.topProducts(start, end, PageRequest.of(0, limit)).map {
            TopProductResponse(
                variantId = it.variantId,
                sku = it.sku,
                productName = it.productName,
                size = it.size,
                color = it.color,
                quantity = it.quantity,
                revenue = it.revenue.setScale(2, RoundingMode.HALF_UP),
            )
        }
    }

    fun byChannel(startDate: LocalDate, endDate: LocalDate): List<RevenueSliceResponse> {
        val (start, end) = window(startDate, endDate)
        return toSlices(saleRepository.revenueByChannel(start, end))
    }

    fun byPayment(startDate: LocalDate, endDate: LocalDate): List<RevenueSliceResponse> {
        val (start, end) = window(startDate, endDate)
        return toSlices(saleRepository.revenueByPayment(start, end))
    }

    private fun toSlices(rows: List<GroupRevenueRow>): List<RevenueSliceResponse> {
        val total = rows.fold(BigDecimal.ZERO) { acc, r -> acc + r.revenue }
        return rows.map { r ->
            val pct = if (total > BigDecimal.ZERO) {
                r.revenue.multiply(BigDecimal(100)).divide(total, 2, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO.setScale(2)
            }
            RevenueSliceResponse(
                label = r.label,
                revenue = r.revenue.setScale(2, RoundingMode.HALF_UP),
                saleCount = r.saleCount,
                percentage = pct,
            )
        }
    }
}
