package br.com.estilofitudi.dashboard.dto

import java.math.BigDecimal

/**
 * KPIs do dashboard referentes ao mês corrente.
 *
 * Vendedores recebem apenas os indicadores das próprias vendas; os campos de gestão
 * (estoque e pró-labore) vêm nulos e não são exibidos para esse perfil.
 */
data class DashboardKpisResponse(
    val monthRevenue: BigDecimal,      // faturamento do mês (finalAmount de vendas confirmadas)
    val saleCount: Long,               // número de vendas do mês
    val stockItems: Long?,             // itens em estoque (variações ativas) — só gestão
    val estimatedProLabore: BigDecimal?, // pró-labore estimado (lucro × pct) — só gestão
)
