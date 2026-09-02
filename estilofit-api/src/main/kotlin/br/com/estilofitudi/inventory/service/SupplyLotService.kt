package br.com.estilofitudi.inventory.service

import br.com.estilofitudi.inventory.domain.StockMovement
import br.com.estilofitudi.inventory.domain.StockMovementType
import br.com.estilofitudi.inventory.domain.SupplyLot
import br.com.estilofitudi.inventory.domain.SupplyLotItem
import br.com.estilofitudi.inventory.dto.*
import br.com.estilofitudi.inventory.repository.StockMovementRepository
import br.com.estilofitudi.inventory.repository.SupplyLotRepository
import br.com.estilofitudi.product.domain.ProductVariant
import br.com.estilofitudi.product.repository.ProductVariantRepository
import br.com.estilofitudi.shared.dto.PageResponse
import br.com.estilofitudi.shared.exception.BusinessException
import br.com.estilofitudi.shared.exception.EntityNotFoundException
import br.com.estilofitudi.supplier.repository.SupplierRepository
import br.com.estilofitudi.user.repository.UserRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.*

@Service
@Transactional(readOnly = true)
class SupplyLotService(
    private val supplyLotRepository: SupplyLotRepository,
    private val supplierRepository: SupplierRepository,
    private val variantRepository: ProductVariantRepository,
    private val stockMovementRepository: StockMovementRepository,
    private val userRepository: UserRepository,
    private val freightAllocator: FreightAllocator,
    private val settingsReader: SettingsReader,
) {

    fun findAll(
        supplierId: UUID?,
        startDate: LocalDate?,
        endDate: LocalDate?,
        pageable: Pageable,
    ): PageResponse<SupplyLotSummaryResponse> {
        val page = supplyLotRepository.findAllWithFilters(supplierId, startDate, endDate, pageable)
        return PageResponse.from(page.map { it.toSummaryResponse() })
    }

    fun findById(id: UUID): SupplyLotResponse {
        val lot = supplyLotRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Lote de entrada", id) }
        return lot.toResponse()
    }

    @Transactional
    fun create(request: CreateSupplyLotRequest, userEmail: String): SupplyLotResponse {
        val supplier = supplierRepository.findById(request.supplierId)
            .orElseThrow { EntityNotFoundException("Fornecedor", request.supplierId) }
        if (!supplier.active) {
            throw BusinessException("Não é possível registrar entrada de um fornecedor inativo.")
        }

        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { EntityNotFoundException("Usuário", userEmail) }

        // Carrega e valida todas as variações
        val variants: Map<UUID, ProductVariant> = request.items.associate { item ->
            val variant = variantRepository.findById(item.variantId)
                .orElseThrow { EntityNotFoundException("Variação", item.variantId) }
            if (!variant.active) {
                throw BusinessException("A variação ${variant.sku} está inativa.")
            }
            item.variantId to variant
        }

        // Rateio do frete
        val allocations = freightAllocator.allocate(
            items = request.items.map { FreightAllocator.Input(it.unitCost, it.quantity) },
            freightTotal = request.freightCost,
        )

        val defaultMargin = settingsReader.defaultProfitMargin()

        val lot = SupplyLot(
            supplier = supplier,
            receivedAt = request.receivedAt,
            freightCost = request.freightCost,
            notes = request.notes?.trim()?.ifBlank { null },
            createdBy = user,
        )

        var totalCost = BigDecimal.ZERO

        request.items.forEachIndexed { index, itemReq ->
            val variant = variants.getValue(itemReq.variantId)
            val alloc = allocations[index]

            // Item do lote
            lot.items.add(
                SupplyLotItem(
                    lot = lot,
                    variant = variant,
                    quantity = itemReq.quantity,
                    unitCost = itemReq.unitCost,
                    freightShare = alloc.freightShare,
                    realUnitCost = alloc.realUnitCost,
                )
            )

            totalCost = totalCost
                .add(itemReq.unitCost.multiply(BigDecimal(itemReq.quantity)))

            // Custo médio ponderado (RN-010)
            val newAvgCost = weightedAverageCost(
                currentStock = variant.stockQuantity,
                currentCost = variant.averageCost,
                incomingQty = itemReq.quantity,
                incomingCost = alloc.realUnitCost,
            )
            variant.averageCost = newAvgCost

            // Recalcula preço de venda pela margem (RN-006) — sempre sobrescreve
            val margin = variant.profitMargin ?: defaultMargin
            variant.salePrice = newAvgCost
                .multiply(BigDecimal.ONE.add(margin.divide(BigDecimal(100))))
                .setScale(2, RoundingMode.HALF_UP)

            // Estoque (RN-011)
            variant.stockQuantity += itemReq.quantity
            variantRepository.save(variant)

            // Movimentação (RN-013)
            stockMovementRepository.save(
                StockMovement(
                    variant = variant,
                    type = StockMovementType.ENTRY,
                    quantity = itemReq.quantity,
                    referenceType = "LOT",
                    user = user,
                )
            )
        }

        lot.totalCost = totalCost.add(request.freightCost).setScale(2, RoundingMode.HALF_UP)

        val saved = supplyLotRepository.save(lot)

        // Preenche o referenceId das movimentações com o id do lote
        // (feito após o save do lote para ter o id gerado)
        return saved.toResponse()
    }

    /** Custo médio ponderado. Se não havia custo/estoque, retorna o custo novo. */
    private fun weightedAverageCost(
        currentStock: Int,
        currentCost: BigDecimal?,
        incomingQty: Int,
        incomingCost: BigDecimal,
    ): BigDecimal {
        if (currentCost == null || currentStock <= 0) {
            return incomingCost.setScale(2, RoundingMode.HALF_UP)
        }
        val currentTotal = currentCost.multiply(BigDecimal(currentStock))
        val incomingTotal = incomingCost.multiply(BigDecimal(incomingQty))
        val totalQty = BigDecimal(currentStock + incomingQty)
        return currentTotal.add(incomingTotal)
            .divide(totalQty, 2, RoundingMode.HALF_UP)
    }
}
