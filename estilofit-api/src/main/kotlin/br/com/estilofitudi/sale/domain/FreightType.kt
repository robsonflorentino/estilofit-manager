package br.com.estilofitudi.sale.domain

/**
 * Tipo de frete de uma venda. O frete é um repasse (ex.: entregador de app) — não é
 * receita nem lucro da loja; apenas soma ao total pago pelo cliente.
 */
enum class FreightType {
    NONE,  // sem frete
    FREE,  // frete grátis (loja não cobra)
    PAID,  // frete cobrado do cliente (valor em freightAmount)
}
