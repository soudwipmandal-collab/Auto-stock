package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sku: String,
    val barcode: String,
    val name: String,
    val category: String, // "Cars", "Bikes", "Spare Parts"
    val subcategory: String, // e.g. "Engine", "Brakes", "Sedan", "Sportbike", "Suspension", "Fluids", "Tyres"
    val fitment: String = "", // e.g. "Fits Honda Civic 2018-2023", "Yamaha MT-07, R7", "Universal"
    val quantity: Int,
    val minStockThreshold: Int = 5,
    val costPrice: Double,
    val sellingPrice: Double,
    val locationRack: String = "Bay A-01",
    val supplier: String = "Direct OEM",
    val description: String = "",
    val unit: String = "Units", // "Units", "Sets", "Litres", "Pairs"
    val lastRestockedTimestamp: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
) {
    val stockStatus: StockStatus
        get() = when {
            quantity <= 0 -> StockStatus.OUT_OF_STOCK
            quantity <= minStockThreshold -> StockStatus.LOW_STOCK
            else -> StockStatus.IN_STOCK
        }

    val profitMargin: Double
        get() = if (sellingPrice > 0) ((sellingPrice - costPrice) / sellingPrice) * 100 else 0.0

    val requiredStock: Int
        get() = maxOf(0, minStockThreshold - quantity)

    val requiredStockValuationCost: Double
        get() = requiredStock * costPrice

    val requiredStockValuationRetail: Double
        get() = requiredStock * sellingPrice

    val totalValuationCost: Double
        get() = quantity * costPrice

    val totalValuationRetail: Double
        get() = quantity * sellingPrice
}
