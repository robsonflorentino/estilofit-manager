package br.com.estilofitudi.promotion.dto

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

/** Variação parada (candidata a promoção). */
data class StaleProductResponse(
    val variantId: UUID,
    val sku: String,
    val productName: String,
    val size: String,
    val color: String,
    val stockQuantity: Int,
    val salePrice: BigDecimal?,
    val averageCost: BigDecimal?,
    val lastSaleAt: LocalDateTime?,   // nulo = nunca vendeu
    val daysStale: Long,              // dias desde a última venda ou desde a entrada em estoque
    val neverSold: Boolean,
    val stockValue: BigDecimal,       // estoque × custo médio (capital parado do item)
)

/** Resposta do alerta de promoção: parâmetros usados, resumo e a lista. */
data class StalePromotionResponse(
    val thresholdDays: Int,
    val staleCount: Int,
    val totalStockValue: BigDecimal,  // capital parado total
    val items: List<StaleProductResponse>,
)
