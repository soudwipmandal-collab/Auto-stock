package com.example.data.repository

import com.example.data.local.InventoryDao
import com.example.data.model.InventoryItem
import com.example.data.model.StockTransaction
import com.example.data.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class InventoryRepository(private val dao: InventoryDao) {

    val allItems: Flow<List<InventoryItem>> = dao.getAllItems()
    val alertItems: Flow<List<InventoryItem>> = dao.getAlertItems()
    val lowStockItems: Flow<List<InventoryItem>> = dao.getLowStockItems()
    val outOfStockItems: Flow<List<InventoryItem>> = dao.getOutOfStockItems()
    val allTransactions: Flow<List<StockTransaction>> = dao.getAllTransactions()

    fun getItemById(id: Long): Flow<InventoryItem?> = dao.getItemById(id)

    fun getItemsByCategory(category: String): Flow<List<InventoryItem>> = dao.getItemsByCategory(category)

    fun searchItems(query: String): Flow<List<InventoryItem>> = dao.searchItems(query)

    fun getTransactionsForItem(itemId: Long): Flow<List<StockTransaction>> = dao.getTransactionsForItem(itemId)

    suspend fun findByBarcodeOrSku(code: String): InventoryItem? = withContext(Dispatchers.IO) {
        val trimmed = code.trim()
        dao.getItemByBarcodeOrSku(trimmed)
    }

    suspend fun insertItem(item: InventoryItem): Long = withContext(Dispatchers.IO) {
        val id = dao.insertItem(item)
        // Log initial stock creation transaction if quantity > 0
        if (item.quantity > 0) {
            dao.insertTransaction(
                StockTransaction(
                    itemId = id,
                    itemName = item.name,
                    sku = item.sku,
                    category = item.category,
                    type = TransactionType.STOCK_IN.name,
                    quantityDelta = item.quantity,
                    previousQuantity = 0,
                    newQuantity = item.quantity,
                    reasonOrNote = "Initial inventory registration & stock-in"
                )
            )
        }
        id
    }

    suspend fun updateItem(item: InventoryItem) = withContext(Dispatchers.IO) {
        val existing = dao.getItemByIdDirect(item.id)
        dao.updateItem(item)
        if (existing != null && existing.quantity != item.quantity) {
            val delta = item.quantity - existing.quantity
            val type = if (delta > 0) TransactionType.STOCK_IN else TransactionType.STOCK_OUT
            dao.insertTransaction(
                StockTransaction(
                    itemId = item.id,
                    itemName = item.name,
                    sku = item.sku,
                    category = item.category,
                    type = type.name,
                    quantityDelta = delta,
                    previousQuantity = existing.quantity,
                    newQuantity = item.quantity,
                    reasonOrNote = "Manual inventory adjustment via edit"
                )
            )
        }
    }

    suspend fun deleteItem(item: InventoryItem) = withContext(Dispatchers.IO) {
        dao.deleteItem(item)
    }

    suspend fun adjustStock(
        itemId: Long,
        delta: Int,
        transactionType: TransactionType,
        note: String
    ): Result<InventoryItem> = withContext(Dispatchers.IO) {
        val item = dao.getItemByIdDirect(itemId)
            ?: return@withContext Result.failure(IllegalArgumentException("Item with ID $itemId not found"))

        val newQuantity = (item.quantity + delta).coerceAtLeast(0)
        val now = System.currentTimeMillis()

        dao.updateQuantity(itemId, newQuantity, now)
        val updatedItem = item.copy(quantity = newQuantity, lastRestockedTimestamp = now)

        dao.insertTransaction(
            StockTransaction(
                itemId = itemId,
                itemName = item.name,
                sku = item.sku,
                category = item.category,
                type = transactionType.name,
                quantityDelta = delta,
                previousQuantity = item.quantity,
                newQuantity = newQuantity,
                reasonOrNote = note.ifBlank { "Stock ${transactionType.label} via AutoStock Manager" },
                timestamp = now
            )
        )

        Result.success(updatedItem)
    }

    suspend fun quickRestock(itemId: Long, addQuantity: Int): Result<InventoryItem> {
        return adjustStock(
            itemId = itemId,
            delta = addQuantity,
            transactionType = TransactionType.STOCK_IN,
            note = "Quick Reorder Restock (+$addQuantity)"
        )
    }

    suspend fun quickStockOut(itemId: Long, subtractQuantity: Int, reason: String = "Quick Scan Stock-Out / Sale"): Result<InventoryItem> {
        return adjustStock(
            itemId = itemId,
            delta = -subtractQuantity,
            transactionType = TransactionType.STOCK_OUT,
            note = reason
        )
    }
}
