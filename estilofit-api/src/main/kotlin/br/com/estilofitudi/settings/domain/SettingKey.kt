package br.com.estilofitudi.settings.domain

import java.math.BigDecimal

/** Tipo do valor de uma configuração — orienta validação e edição na UI. */
enum class SettingType { INTEGER, DECIMAL }

/**
 * Catálogo das configurações conhecidas do sistema. Fixo no código (as chaves livres
 * não são permitidas): cada uma define rótulo, tipo, faixa válida e valor de fallback.
 */
enum class SettingKey(
    val key: String,
    val label: String,
    val type: SettingType,
    val min: BigDecimal,
    val max: BigDecimal?,
    val fallback: String,
) {
    DEFAULT_PROFIT_MARGIN(
        "DEFAULT_PROFIT_MARGIN", "Margem de lucro padrão (%)",
        SettingType.DECIMAL, BigDecimal.ZERO, null, "100",
    ),
    LOW_STOCK_THRESHOLD(
        "LOW_STOCK_THRESHOLD", "Alerta de estoque baixo (unidades)",
        SettingType.INTEGER, BigDecimal.ZERO, null, "2",
    ),
    PRO_LABORE_PCT(
        "PRO_LABORE_PCT", "Percentual de pró-labore (%)",
        SettingType.DECIMAL, BigDecimal.ZERO, BigDecimal("100"), "30",
    ),
    PROMOTION_ALERT_DAYS(
        "PROMOTION_ALERT_DAYS", "Dias sem venda para alerta de promoção",
        SettingType.INTEGER, BigDecimal.ONE, null, "60",
    );

    companion object {
        fun fromKey(key: String): SettingKey? = entries.firstOrNull { it.key == key }
    }
}
