package com.example.data.model

data class RecentScanRecord(
    val barcode: String,
    val timestamp: Long = System.currentTimeMillis(),
    val itemName: String? = null,
    val itemSku: String? = null,
    val isRegistered: Boolean = true
)
