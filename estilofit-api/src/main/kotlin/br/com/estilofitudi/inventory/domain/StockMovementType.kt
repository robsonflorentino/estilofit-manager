package br.com.estilofitudi.inventory.domain

enum class StockMovementType {
    ENTRY,      // entrada por lote
    SALE,       // saída por venda
    ADJUSTMENT, // ajuste manual
}
