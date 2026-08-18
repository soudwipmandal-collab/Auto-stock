package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.initial.SampleData
import com.example.data.model.InventoryItem
import com.example.data.model.StockTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [InventoryItem::class, StockTransaction::class],
    version = 1,
    exportSchema = false
)
abstract class AutoStockDatabase : RoomDatabase() {
    abstract fun inventoryDao(): InventoryDao

    companion object {
        @Volatile
        private var INSTANCE: AutoStockDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AutoStockDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AutoStockDatabase::class.java,
                    "autostock_inventory_db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialDatabase(database.inventoryDao())
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        val count = database.inventoryDao().getItemCount()
                        if (count == 0) {
                            populateInitialDatabase(database.inventoryDao())
                        }
                    }
                }
            }

            suspend fun populateInitialDatabase(dao: InventoryDao) {
                val sampleItems = SampleData.getInitialInventory()
                dao.insertItems(sampleItems)
                val sampleTransactions = SampleData.getInitialTransactions()
                sampleTransactions.forEach { dao.insertTransaction(it) }
            }
        }
    }
}
