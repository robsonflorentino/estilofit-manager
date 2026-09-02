package br.com.estilofitudi.inventory.service

import br.com.estilofitudi.settings.domain.SettingKey
import br.com.estilofitudi.settings.service.SettingsService
import org.springframework.stereotype.Component
import java.math.BigDecimal

/**
 * Fachada de leitura de configurações para os módulos de domínio (estoque, dashboard,
 * promoções). Delega ao SettingsService (fonte única) e converte para o tipo esperado,
 * com fallback para o valor padrão da chave quando o valor armazenado é inválido.
 */
@Component
class SettingsReader(private val settingsService: SettingsService) {

    fun defaultProfitMargin(): BigDecimal = decimal(SettingKey.DEFAULT_PROFIT_MARGIN)

    fun lowStockThreshold(): Int = integer(SettingKey.LOW_STOCK_THRESHOLD)

    /** Percentual do lucro destinado ao pró-labore, para o KPI estimado do dashboard. */
    fun proLaborePct(): BigDecimal = decimal(SettingKey.PRO_LABORE_PCT)

    /** Dias sem venda para uma variação virar alerta de promoção. */
    fun promotionAlertDays(): Int = integer(SettingKey.PROMOTION_ALERT_DAYS)

    /** Pró-labore desejado por mês (R$), base da meta de vendas. */
    fun targetProLabore(): BigDecimal = decimal(SettingKey.TARGET_PRO_LABORE)

    private fun decimal(def: SettingKey): BigDecimal =
        settingsService.rawValue(def).toBigDecimalOrNull() ?: BigDecimal(def.fallback)

    private fun integer(def: SettingKey): Int =
        settingsService.rawValue(def).toBigDecimalOrNull()?.toInt() ?: BigDecimal(def.fallback).toInt()
}
