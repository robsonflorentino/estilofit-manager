package br.com.estilofitudi.sale.service

import br.com.estilofitudi.inventory.service.StockService
import br.com.estilofitudi.product.domain.ProductVariant
import br.com.estilofitudi.product.repository.ProductVariantRepository
import br.com.estilofitudi.sale.domain.*
import br.com.estilofitudi.sale.dto.*
import br.com.estilofitudi.sale.repository.SaleChannelRepository
import br.com.estilofitudi.sale.repository.SaleRepository
import br.com.estilofitudi.shared.dto.PageResponse
import br.com.estilofitudi.shared.exception.BusinessException
import br.com.estilofitudi.shared.exception.EntityNotFoundException
import br.com.estilofitudi.user.domain.Role
import br.com.estilofitudi.user.repository.UserRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional(readOnly = true)
class SaleService(
    private val saleRepository: SaleRepository,
    private val channelRepository: SaleChannelRepository,
    private val variantRepository: ProductVariantRepository,
    private val userRepository: UserRepository,
    private val stockService: StockService,
    private val installmentScheduler: InstallmentScheduler,
) {

    fun findAll(
        channelId: UUID?,
        paymentMethod: PaymentMethod?,
        status: SaleStatus?,
        sellerId: UUID?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        currentUserEmail: String,
        pageable: Pageable,
    ): PageResponse<SaleSummaryResponse> {
        val currentUser = userRepository.findByEmail(currentUserEmail)
            .orElseThrow { EntityNotFoundException("Usuário", currentUserEmail) }

        // Vendedor só vê as próprias vendas (filtro forçado — decisão 8)
        val effectiveSellerId = if (currentUser.role == Role.SELLER) currentUser.id else sellerId

        val page = saleRepository.findAllWithFilters(
            channelId, paymentMethod, status, effectiveSellerId, startDate, endDate, pageable,
        )
        return PageResponse.from(page.map { it.toSummaryResponse() })
    }

    fun findById(id: UUID, currentUserEmail: String): SaleDetailResponse {
        val sale = saleRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Venda", id) }

        val currentUser = userRepository.findByEmail(currentUserEmail)
            .orElseThrow { EntityNotFoundException("Usuário", currentUserEmail) }

        // Vendedor só acessa as próprias vendas
        if (currentUser.role == Role.SELLER && sale.seller.id != currentUser.id) {
            throw EntityNotFoundException("Venda", id)
        }
        return sale.toDetailResponse()
    }

    @Transactional
    fun create(request: CreateSaleRequest, userEmail: String): SaleDetailResponse {
        val channel = channelRepository.findById(request.channelId)
            .orElseThrow { EntityNotFoundException("Canal de venda", request.channelId) }
        if (!channel.active) {
            throw BusinessException("O canal de venda '${channel.name}' está inativo.")
        }

        val seller = userRepository.findByEmail(userEmail)
            .orElseThrow { EntityNotFoundException("Usuário", userEmail) }

        // Carrega variações, valida preço e estoque (RN-018/020, decisões 2/3/5)
        data class ResolvedItem(val variant: ProductVariant, val quantity: Int, val unitPrice: BigDecimal)
        val resolved = request.items.map { item ->
            val variant = variantRepository.findById(item.variantId)
                .orElseThrow { EntityNotFoundException("Variação", item.variantId) }
            if (!variant.active) {
                throw BusinessException("A variação ${variant.sku} está inativa.")
            }
            val price = variant.salePrice
                ?: throw BusinessException("A variação ${variant.sku} não tem preço de venda definido.")
            if (variant.stockQuantity < item.quantity) {
                throw BusinessException(
                    "Estoque insuficiente para ${variant.sku}. " +
                        "Disponível: ${variant.stockQuantity}, solicitado: ${item.quantity}.",
                )
            }
            ResolvedItem(variant, item.quantity, price)
        }

        // Totais
        val totalAmount = resolved.fold(BigDecimal.ZERO) { acc, r ->
            acc + r.unitPrice.multiply(BigDecimal(r.quantity))
        }
        val discount = request.discountAmount
        val finalAmount = totalAmount.subtract(discount)
        if (finalAmount <= BigDecimal.ZERO) {
            throw BusinessException("O valor final da venda deve ser maior que zero (verifique o desconto).")
        }

        // Regras de parcelamento (RN-022)
        val isInstallment = request.installments >= 2
        if (isInstallment) {
            if (request.paymentMethod != PaymentMethod.CREDIT_CARD) {
                throw BusinessException("Parcelamento só é permitido no cartão de crédito.")
            }
            if (request.cardFeePct == null) {
                throw BusinessException("A taxa da maquininha é obrigatória em vendas parceladas.")
            }
        }

        val sale = Sale(
            channel = channel,
            seller = seller,
            confirmedAt = LocalDateTime.now(),
            totalAmount = totalAmount,
            discountAmount = discount,
            finalAmount = finalAmount,
            paymentMethod = request.paymentMethod,
            installments = request.installments,
            cardFeePct = request.cardFeePct,
            cardFeePassed = request.cardFeePassed,
            notes = request.notes?.trim()?.ifBlank { null },
        )

        // Itens + baixa de estoque
        resolved.forEach { r ->
            sale.items.add(
                SaleItem(
                    sale = sale,
                    variant = r.variant,
                    quantity = r.quantity,
                    unitPrice = r.unitPrice,
                    totalPrice = r.unitPrice.multiply(BigDecimal(r.quantity)),
                )
            )
            stockService.registerSaleExit(r.variant, r.quantity, sale.id, seller)
        }

        // Parcelas (RN-023/024/025)
        if (isInstallment) {
            installmentScheduler.schedule(
                finalAmount = finalAmount,
                installments = request.installments,
                cardFeePct = request.cardFeePct!!,
                saleDate = sale.confirmedAt.toLocalDate(),
            ).forEach { plan ->
                sale.installmentSchedule.add(
                    SaleInstallment(
                        sale = sale,
                        installmentNum = plan.installmentNum,
                        dueDate = plan.dueDate,
                        grossAmount = plan.grossAmount,
                        netAmount = plan.netAmount,
                    )
                )
            }
        }

        return saleRepository.save(sale).toDetailResponse()
    }

    @Transactional
    fun cancel(id: UUID, request: CancelSaleRequest, userEmail: String): SaleDetailResponse {
        val sale = saleRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Venda", id) }

        if (sale.status == SaleStatus.CANCELLED) {
            throw BusinessException("Esta venda já está cancelada.")
        }

        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { EntityNotFoundException("Usuário", userEmail) }

        // Estorna estoque (RN-021)
        sale.items.forEach { item ->
            stockService.registerSaleReversal(item.variant, item.quantity, sale.id, user)
        }

        // Cancela parcelas pendentes (RN-027) — recebidas permanecem no histórico
        sale.installmentSchedule
            .filter { it.status == InstallmentStatus.PENDING }
            .forEach { it.status = InstallmentStatus.CANCELLED }

        sale.status = SaleStatus.CANCELLED
        sale.cancelledBy = user
        sale.cancelledAt = LocalDateTime.now()
        sale.notes = ((sale.notes ?: "") + "\n[Cancelamento] ${request.reason.trim()}").trim()

        return saleRepository.save(sale).toDetailResponse()
    }
}
