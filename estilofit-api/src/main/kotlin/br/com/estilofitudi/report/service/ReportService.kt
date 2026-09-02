package br.com.estilofitudi.report.service

import br.com.estilofitudi.inventory.service.SettingsReader
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
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Service
@Transactional(readOnly = true)
class ReportService(
    private val saleRepository: SaleRepository,
    private val saleItemRepository: SaleItemRepository,
    private val settingsReader: SettingsReader,
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
        // Lucro = faturamento - custo das mercadorias - comissão dos vendedores
        val profit = agg.revenue.subtract(agg.cost).subtract(agg.commission)

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

    /**
     * Meta de vendas por mês para atingir o pró-labore desejado.
     *
     * meta_faturamento = pró-labore desejado / (margem de lucro × PRO_LABORE_PCT/100)
     * onde a margem de lucro é a real do mês (lucro/faturamento); meses sem vendas usam
     * a margem padrão configurada (DEFAULT_PROFIT_MARGIN convertida para margem sobre a venda).
     */
    fun salesTarget(months: Int): SalesTargetResponse {
        val safeMonths = months.coerceIn(1, 24)
        val targetProLabore = settingsReader.targetProLabore()
        val proLaborePct = settingsReader.proLaborePct()

        // Janela: início do mês corrente menos (safeMonths-1) meses até o início do mês seguinte
        val currentMonth = YearMonth.now()
        val firstMonth = currentMonth.minusMonths((safeMonths - 1).toLong())
        val start = firstMonth.atDay(1).atStartOfDay()
        val end = currentMonth.plusMonths(1).atDay(1).atStartOfDay()

        val byMonth = saleRepository.monthlyAggregate(start, end)
            .associateBy { YearMonth.of(it.year, it.month) }

        val fmt = DateTimeFormatter.ofPattern("yyyy-MM")
        // Fração do faturamento que vira lucro, pela margem padrão (markup): margin/(100+margin)
        val defaultMargin = settingsReader.defaultProfitMargin()
        val fallbackProfitFraction = defaultMargin.divide(
            BigDecimal(100).add(defaultMargin), 6, RoundingMode.HALF_UP,
        )

        val monthsList = (0 until safeMonths).map { offset ->
            val ym = firstMonth.plusMonths(offset.toLong())
            val agg = byMonth[ym]
            val revenue = agg?.revenue ?: BigDecimal.ZERO
            val cost = agg?.cost ?: BigDecimal.ZERO
            val commission = agg?.commission ?: BigDecimal.ZERO

            // Fração de lucro real do mês (líquida de custo e comissão); sem vendas usa a margem padrão
            val profitFraction = if (revenue > BigDecimal.ZERO) {
                revenue.subtract(cost).subtract(commission).divide(revenue, 6, RoundingMode.HALF_UP)
            } else {
                fallbackProfitFraction
            }

            // meta = salário / (fração de lucro × proLaborePct/100)
            val denominator = profitFraction.multiply(proLaborePct).divide(BigDecimal(100), 6, RoundingMode.HALF_UP)
            val target = if (denominator > BigDecimal.ZERO) {
                targetProLabore.divide(denominator, 2, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO.setScale(2)
            }

            SalesTargetMonthResponse(
                month = ym.format(fmt),
                revenue = revenue.setScale(2, RoundingMode.HALF_UP),
                target = target,
                profitMarginPct = profitFraction.multiply(BigDecimal(100)).setScale(2, RoundingMode.HALF_UP),
                achieved = revenue >= target && target > BigDecimal.ZERO,
            )
        }

        return SalesTargetResponse(
            targetProLabore = targetProLabore.setScale(2, RoundingMode.HALF_UP),
            proLaborePct = proLaborePct.setScale(2, RoundingMode.HALF_UP),
            months = monthsList,
        )
    }

    /** Lucratividade por canal no período, ordenada do mais lucrativo (lucro absoluto) para o menos. */
    fun profitByChannel(startDate: LocalDate, endDate: LocalDate): List<ChannelProfitResponse> {
        val (start, end) = window(startDate, endDate)
        return saleRepository.profitByChannel(start, end)
            .map { r ->
                val profit = r.revenue.subtract(r.cost)
                val marginPct = if (r.revenue > BigDecimal.ZERO) {
                    profit.multiply(BigDecimal(100)).divide(r.revenue, 2, RoundingMode.HALF_UP)
                } else {
                    BigDecimal.ZERO.setScale(2)
                }
                ChannelProfitResponse(
                    channel = r.label,
                    revenue = r.revenue.setScale(2, RoundingMode.HALF_UP),
                    cost = r.cost.setScale(2, RoundingMode.HALF_UP),
                    profit = profit.setScale(2, RoundingMode.HALF_UP),
                    marginPct = marginPct,
                    saleCount = r.saleCount,
                )
            }
            .sortedByDescending { it.profit }
    }

    /**
     * Sugestão de compra para o próximo lote, com base nas vendas dos últimos [referenceDays]
     * dias e na posição atual de estoque. Variações que não venderam no período não entram.
     * Ordenado pela urgência (menor cobertura de estoque primeiro).
     */
    fun purchaseSuggestion(referenceDays: Int): PurchaseSuggestionReportResponse {
        val refDays = referenceDays.coerceIn(1, 365)
        val coverageTarget = settingsReader.purchaseCoverageDays()
        val lowStockThreshold = settingsReader.lowStockThreshold()

        val end = LocalDate.now().plusDays(1).atStartOfDay()
        val start = LocalDate.now().minusDays((refDays - 1).toLong()).atStartOfDay()

        val items = saleItemRepository.salesForPurchaseSuggestion(start, end).map { row ->
            // velocidade = unidades vendidas / dias do período de referência
            val dailyVelocity = BigDecimal(row.soldQty).divide(BigDecimal(refDays), 4, RoundingMode.HALF_UP)

            // dias de cobertura = estoque / velocidade (null quando não há velocidade relevante)
            val coverageDays = if (dailyVelocity > BigDecimal.ZERO) {
                BigDecimal(row.stockQuantity).divide(dailyVelocity, 0, RoundingMode.FLOOR).toInt()
            } else {
                null
            }

            // demanda para o horizonte-alvo e sugestão de compra (o que falta, arredondado pra cima)
            val demand = dailyVelocity.multiply(BigDecimal(coverageTarget))
            val suggested = demand.subtract(BigDecimal(row.stockQuantity))
                .setScale(0, RoundingMode.CEILING).toInt()
                .coerceAtLeast(0)

            val avgCost = row.averageCost ?: BigDecimal.ZERO
            val estimatedCost = avgCost.multiply(BigDecimal(suggested)).setScale(2, RoundingMode.HALF_UP)

            PurchaseSuggestionResponse(
                variantId = row.variantId,
                sku = row.sku,
                productName = row.productName,
                size = row.size,
                color = row.color,
                stockQuantity = row.stockQuantity,
                soldQty = row.soldQty,
                dailyVelocity = dailyVelocity.setScale(2, RoundingMode.HALF_UP),
                coverageDays = coverageDays,
                suggestedQty = suggested,
                belowMinimum = row.stockQuantity < lowStockThreshold,
                estimatedCost = estimatedCost,
            )
        }.sortedWith(
            // urgência: menor cobertura primeiro; cobertura nula (sem giro) vai por último
            compareBy(nullsLast()) { it.coverageDays },
        )

        return PurchaseSuggestionReportResponse(
            referenceDays = refDays,
            coverageTargetDays = coverageTarget,
            items = items,
        )
    }

    /** Ranking de vendedores por faturamento no período (posição 1 = maior faturamento). */
    fun sellerRanking(startDate: LocalDate, endDate: LocalDate): List<SellerRankingResponse> {
        val (start, end) = window(startDate, endDate)
        return saleRepository.sellerRanking(start, end).mapIndexed { index, r ->
            val averageTicket = if (r.saleCount > 0) {
                r.revenue.divide(BigDecimal(r.saleCount), 2, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO.setScale(2)
            }
            SellerRankingResponse(
                position = index + 1,
                sellerId = r.sellerId,
                sellerName = r.sellerName,
                revenue = r.revenue.setScale(2, RoundingMode.HALF_UP),
                saleCount = r.saleCount,
                averageTicket = averageTicket,
            )
        }
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
