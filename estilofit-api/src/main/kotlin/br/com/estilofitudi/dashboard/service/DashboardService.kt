package br.com.estilofitudi.dashboard.service

import br.com.estilofitudi.dashboard.dto.DashboardKpisResponse
import br.com.estilofitudi.inventory.service.SettingsReader
import br.com.estilofitudi.product.repository.ProductVariantRepository
import br.com.estilofitudi.sale.repository.SaleRepository
import br.com.estilofitudi.shared.exception.EntityNotFoundException
import br.com.estilofitudi.user.domain.Role
import br.com.estilofitudi.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class DashboardService(
    private val saleRepository: SaleRepository,
    private val variantRepository: ProductVariantRepository,
    private val userRepository: UserRepository,
    private val settingsReader: SettingsReader,
) {

    fun kpis(currentUserEmail: String): DashboardKpisResponse {
        val currentUser = userRepository.findByEmail(currentUserEmail)
            .orElseThrow { EntityNotFoundException("Usuário", currentUserEmail) }

        val isSeller = currentUser.role == Role.SELLER
        // Vendedor só enxerga os próprios números (mesma regra de Vendas)
        val sellerId = if (isSeller) currentUser.id else null

        // Janela do mês corrente [primeiro dia 00:00, primeiro dia do mês seguinte 00:00)
        val firstDay = LocalDate.now().withDayOfMonth(1)
        val start: LocalDateTime = firstDay.atStartOfDay()
        val end: LocalDateTime = firstDay.plusMonths(1).atStartOfDay()

        val agg = saleRepository.aggregateConfirmed(start, end, sellerId)

        // Indicadores de gestão não são expostos ao vendedor
        val stockItems: Long? = if (isSeller) null else variantRepository.sumActiveStock()
        val estimatedProLabore: BigDecimal? = if (isSeller) {
            null
        } else {
            // Lucro = faturamento - custo das mercadorias - comissão dos vendedores
            val profit = agg.revenue.subtract(agg.cost).subtract(agg.commission)
            val positiveProfit = if (profit < BigDecimal.ZERO) BigDecimal.ZERO else profit
            positiveProfit
                .multiply(settingsReader.proLaborePct())
                .divide(BigDecimal(100))
                .setScale(2, RoundingMode.HALF_UP)
        }

        return DashboardKpisResponse(
            monthRevenue = agg.revenue.setScale(2, RoundingMode.HALF_UP),
            saleCount = agg.saleCount,
            stockItems = stockItems,
            estimatedProLabore = estimatedProLabore,
        )
    }
}
