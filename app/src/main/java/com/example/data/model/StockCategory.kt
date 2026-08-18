package com.example.data.model

enum class StockCategory(val displayName: String) {
    CAR("Cars"),
    BIKE("Bikes"),
    SPARE_PART("Spare Parts");

    companion object {
        fun fromString(value: String): StockCategory {
            return entries.firstOrNull { it.displayName.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true) }
                ?: SPARE_PART
        }
    }
}

enum class StockStatus(val label: String) {
    IN_STOCK("In Stock"),
    LOW_STOCK("Low Stock"),
    OUT_OF_STOCK("Out of Stock")
}

enum class TransactionType(val label: String, val isPositive: Boolean) {
    STOCK_IN("Restock (+)", true),
    STOCK_OUT("Sale/Dispatched (-)", false),
    ADJUSTMENT("Audit Adjustment", true),
    DAMAGE("Damaged / Scrapped (-)", false)
}
