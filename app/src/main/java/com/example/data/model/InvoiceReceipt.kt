package com.example.data.model

// Represents an item added to the active bill/cart
data class CartItem(
    val item: InventoryItem,
    val quantity: Int
)

// Custom receipt holder
data class InvoiceReceipt(
    val invoiceId: String,
    val customerName: String,
    val customerPhone: String,
    val items: List<CartItem>,
    val subtotal: Double,
    val gst: Double,
    val total: Double,
    val paymentMethod: String,
    val timestamp: Long
)
