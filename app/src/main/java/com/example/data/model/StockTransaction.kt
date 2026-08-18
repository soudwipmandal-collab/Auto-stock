package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_transactions")
data class StockTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemId: Long,
    val itemName: String,
    val sku: String,
    val category: String,
    val type: String, // TransactionType.name
    val quantityDelta: Int, // e.g. +10 or -3
    val previousQuantity: Int,
    val newQuantity: Int,
    val reasonOrNote: String,
    val timestamp: Long = System.currentTimeMillis()
)
