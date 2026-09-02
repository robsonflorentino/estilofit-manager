package br.com.estilofitudi.inventory.service

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import java.math.BigDecimal

/**
 * Leitura simples de configurações de system_settings enquanto o módulo de Settings
 * não é implementado (decisão 3 do tech design 009). Faz fallback quando a chave não existe.
 */
@Component
class SettingsReader(private val entityManager: EntityManager) {

    fun defaultProfitMargin(): BigDecimal =
        readDecimal("DEFAULT_PROFIT_MARGIN", fallback = BigDecimal("100"))

    fun lowStockThreshold(): Int =
        readDecimal("LOW_STOCK_THRESHOLD", fallback = BigDecimal("2")).toInt()

    /** Percentual do lucro destinado ao pró-labore, para o KPI estimado do dashboard. */
    fun proLaborePct(): BigDecimal =
        readDecimal("PRO_LABORE_PCT", fallback = BigDecimal("30"))

    /** Dias sem venda para uma variação virar alerta de promoção. */
    fun promotionAlertDays(): Int =
        readDecimal("PROMOTION_ALERT_DAYS", fallback = BigDecimal("60")).toInt()

    private fun readDecimal(key: String, fallback: BigDecimal): BigDecimal {
        return try {
            val value = entityManager
                .createNativeQuery("SELECT value FROM system_settings WHERE key = :k")
                .setParameter("k", key)
                .resultList
                .firstOrNull() as? String
            value?.toBigDecimalOrNull() ?: fallback
        } catch (ex: Exception) {
            fallback
        }
    }
}
