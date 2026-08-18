package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.InventoryItem
import com.example.data.model.StockTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {

    @Query("SELECT * FROM inventory_items ORDER BY lastRestockedTimestamp DESC")
    fun getAllItems(): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items WHERE id = :id")
    fun getItemById(id: Long): Flow<InventoryItem?>

    @Query("SELECT * FROM inventory_items WHERE id = :id LIMIT 1")
    suspend fun getItemByIdDirect(id: Long): InventoryItem?

    @Query("SELECT * FROM inventory_items WHERE barcode = :barcode LIMIT 1")
    suspend fun getItemByBarcode(barcode: String): InventoryItem?

    @Query("SELECT * FROM inventory_items WHERE sku = :sku LIMIT 1")
    suspend fun getItemBySku(sku: String): InventoryItem?

    @Query("SELECT * FROM inventory_items WHERE barcode = :code OR sku = :code LIMIT 1")
    suspend fun getItemByBarcodeOrSku(code: String): InventoryItem?

    @Query("SELECT * FROM inventory_items WHERE quantity <= minStockThreshold ORDER BY quantity ASC")
    fun getAlertItems(): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items WHERE quantity = 0 ORDER BY lastRestockedTimestamp DESC")
    fun getOutOfStockItems(): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items WHERE quantity > 0 AND quantity <= minStockThreshold ORDER BY quantity ASC")
    fun getLowStockItems(): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items WHERE category = :category ORDER BY name ASC")
    fun getItemsByCategory(category: String): Flow<List<InventoryItem>>

    @Query("""
        SELECT * FROM inventory_items 
        WHERE name LIKE '%' || :query || '%' 
           OR sku LIKE '%' || :query || '%' 
           OR barcode LIKE '%' || :query || '%' 
           OR fitment LIKE '%' || :query || '%' 
           OR subcategory LIKE '%' || :query || '%' 
           OR locationRack LIKE '%' || :query || '%'
        ORDER BY name ASC
    """)
    fun searchItems(query: String): Flow<List<InventoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<InventoryItem>)

    @Update
    suspend fun updateItem(item: InventoryItem)

    @Delete
    suspend fun deleteItem(item: InventoryItem)

    @Query("UPDATE inventory_items SET quantity = :newQty, lastRestockedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateQuantity(id: Long, newQty: Int, timestamp: Long)

    @Query("SELECT COUNT(*) FROM inventory_items")
    suspend fun getItemCount(): Int

    // Transactions
    @Query("SELECT * FROM stock_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<StockTransaction>>

    @Query("SELECT * FROM stock_transactions WHERE itemId = :itemId ORDER BY timestamp DESC")
    fun getTransactionsForItem(itemId: Long): Flow<List<StockTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: StockTransaction): Long
}
