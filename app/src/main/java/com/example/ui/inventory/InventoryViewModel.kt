package com.example.ui.inventory

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AutoStockDatabase
import com.example.data.model.InventoryItem
import com.example.data.model.RecentScanRecord
import com.example.data.model.StockTransaction
import com.example.data.model.TransactionType
import com.example.data.repository.InventoryRepository
import com.example.ui.util.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InventoryUiState(
    val items: List<InventoryItem> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

open class InventoryViewModel(application: Application) : AndroidViewModel(application) {

    protected val repository: InventoryRepository

    private val prefs = application.getSharedPreferences("autostock_scans_prefs", Context.MODE_PRIVATE)

    private val _rawRecentScans = MutableStateFlow<List<RecentScanRecord>>(emptyList())
    val recentScans: StateFlow<List<RecentScanRecord>>

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val selectedLanguage: StateFlow<AppLanguage> = _selectedLanguage.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("app_is_dark_theme", true))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _globalSafetyThreshold = MutableStateFlow(prefs.getInt("app_global_safety_threshold", 5))
    val globalSafetyThreshold: StateFlow<Int> = _globalSafetyThreshold.asStateFlow()

    private val _showCarsTab = MutableStateFlow(prefs.getBoolean("show_category_cars", true))
    val showCarsTab: StateFlow<Boolean> = _showCarsTab.asStateFlow()

    private val _showBikesTab = MutableStateFlow(prefs.getBoolean("show_category_bikes", true))
    val showBikesTab: StateFlow<Boolean> = _showBikesTab.asStateFlow()

    private val _showSparePartsTab = MutableStateFlow(prefs.getBoolean("show_category_spare_parts", true))
    val showSparePartsTab: StateFlow<Boolean> = _showSparePartsTab.asStateFlow()

    fun setShowCarsTab(value: Boolean) {
        _showCarsTab.value = value
        prefs.edit().putBoolean("show_category_cars", value).apply()
    }

    fun setShowBikesTab(value: Boolean) {
        _showBikesTab.value = value
        prefs.edit().putBoolean("show_category_bikes", value).apply()
    }

    fun setShowSparePartsTab(value: Boolean) {
        _showSparePartsTab.value = value
        prefs.edit().putBoolean("show_category_spare_parts", value).apply()
    }

    fun toggleTheme() {
        val newValue = !_isDarkTheme.value
        _isDarkTheme.value = newValue
        prefs.edit().putBoolean("app_is_dark_theme", newValue).apply()
    }

    fun setGlobalThreshold(newThreshold: Int) {
        viewModelScope.launch {
            _globalSafetyThreshold.value = newThreshold
            prefs.edit().putInt("app_global_safety_threshold", newThreshold).apply()
            val currentItems = allItems.value
            currentItems.forEach { item ->
                if (item.minStockThreshold != newThreshold) {
                    repository.updateItem(item.copy(minStockThreshold = newThreshold))
                }
            }
        }
    }

    companion object {
        fun isCategoryAllowed(category: String, showCars: Boolean, showBikes: Boolean, showSpareParts: Boolean): Boolean {
            val norm = category.trim().lowercase()
            if (norm == "cars" || norm == "car") return showCars
            if (norm == "bikes" || norm == "bike") return showBikes
            if (norm == "spare parts" || norm == "spare part" || norm == "spare_parts" || norm == "spares" || norm == "parts") return showSpareParts
            return true
        }
    }

    val allItems: StateFlow<List<InventoryItem>>
    val alertItems: StateFlow<List<InventoryItem>>
    val lowStockItems: StateFlow<List<InventoryItem>>
    val outOfStockItems: StateFlow<List<InventoryItem>>
    val transactions: StateFlow<List<StockTransaction>>

    private val _rawBillingHistory = MutableStateFlow<List<com.example.data.model.InvoiceReceipt>>(emptyList())
    val billingHistory: StateFlow<List<com.example.data.model.InvoiceReceipt>>

    init {
        val db = AutoStockDatabase.getDatabase(application, viewModelScope)
        repository = InventoryRepository(db.inventoryDao())

        allItems = combine(
            repository.allItems,
            _showCarsTab,
            _showBikesTab,
            _showSparePartsTab
        ) { items, showCars, showBikes, showSpareParts ->
            items.filter { isCategoryAllowed(it.category, showCars, showBikes, showSpareParts) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        alertItems = combine(
            repository.alertItems,
            _showCarsTab,
            _showBikesTab,
            _showSparePartsTab
        ) { items, showCars, showBikes, showSpareParts ->
            items.filter { isCategoryAllowed(it.category, showCars, showBikes, showSpareParts) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        lowStockItems = combine(
            repository.lowStockItems,
            _showCarsTab,
            _showBikesTab,
            _showSparePartsTab
        ) { items, showCars, showBikes, showSpareParts ->
            items.filter { isCategoryAllowed(it.category, showCars, showBikes, showSpareParts) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        outOfStockItems = combine(
            repository.outOfStockItems,
            _showCarsTab,
            _showBikesTab,
            _showSparePartsTab
        ) { items, showCars, showBikes, showSpareParts ->
            items.filter { isCategoryAllowed(it.category, showCars, showBikes, showSpareParts) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        transactions = combine(
            repository.allTransactions,
            _showCarsTab,
            _showBikesTab,
            _showSparePartsTab
        ) { txs, showCars, showBikes, showSpareParts ->
            txs.filter { isCategoryAllowed(it.category, showCars, showBikes, showSpareParts) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        billingHistory = combine(
            _rawBillingHistory,
            _showCarsTab,
            _showBikesTab,
            _showSparePartsTab
        ) { history, showCars, showBikes, showSpareParts ->
            history.mapNotNull { receipt ->
                val visibleCartItems = receipt.items.filter { 
                    isCategoryAllowed(it.item.category, showCars, showBikes, showSpareParts) 
                }
                if (visibleCartItems.isEmpty() && receipt.items.isNotEmpty()) {
                    null
                } else {
                    receipt.copy(items = visibleCartItems)
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        recentScans = combine(
            _rawRecentScans,
            repository.allItems,
            _showCarsTab,
            _showBikesTab,
            _showSparePartsTab
        ) { scans, items, showCars, showBikes, showSpareParts ->
            val itemsByBarcode = items.associateBy { it.barcode.trim().lowercase() }
            val itemsBySku = items.associateBy { it.sku.trim().lowercase() }
            scans.filter { scan ->
                val matched = itemsByBarcode[scan.barcode.trim().lowercase()] ?: itemsBySku[scan.barcode.trim().lowercase()]
                if (matched != null) {
                    isCategoryAllowed(matched.category, showCars, showBikes, showSpareParts)
                } else {
                    true
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            loadRecentScans()
            repository.allItems.collect { items ->
                if (items.isNotEmpty()) {
                    loadBillingHistory(items)
                }
            }
        }
    }

    private fun loadRecentScans() {
        val saved = prefs.getString("recent_barcodes_v1", null)
        if (!saved.isNullOrBlank()) {
            try {
                val records = saved.split(";;;").mapNotNull { line ->
                    val parts = line.split("|||")
                    if (parts.size >= 5) {
                        RecentScanRecord(
                            barcode = parts[0],
                            timestamp = parts[1].toLongOrNull() ?: System.currentTimeMillis(),
                            itemName = parts[2].ifBlank { null },
                            itemSku = parts[3].ifBlank { null },
                            isRegistered = parts[4].toBooleanStrictOrNull() ?: true
                        )
                    } else null
                }
                _rawRecentScans.value = records.take(10)
            } catch (_: Exception) {}
        }
    }

    fun addRecentScan(barcode: String, item: InventoryItem?) {
        if (barcode.isBlank()) return
        val current = _rawRecentScans.value.toMutableList()
        current.removeAll { it.barcode == barcode }
        val record = RecentScanRecord(
            barcode = barcode,
            timestamp = System.currentTimeMillis(),
            itemName = item?.name,
            itemSku = item?.sku,
            isRegistered = (item != null)
        )
        current.add(0, record)
        val trimmed = current.take(10)
        _rawRecentScans.value = trimmed

        val serialized = trimmed.joinToString(";;;") { r ->
            "${r.barcode}|||${r.timestamp}|||${r.itemName ?: ""}|||${r.itemSku ?: ""}|||${r.isRegistered}"
        }
        prefs.edit().putString("recent_barcodes_v1", serialized).apply()
    }

    fun clearRecentScans() {
        _rawRecentScans.value = emptyList()
        prefs.edit().remove("recent_barcodes_v1").apply()
    }

    val uiState: StateFlow<InventoryUiState> = combine(
        allItems,
        _searchQuery,
        _selectedCategory
    ) { items, query, category ->
        val filtered = items.filter { item ->
            val matchesCategory = (category == "All" || item.category.equals(category, ignoreCase = true))
            val matchesSearch = query.isBlank() ||
                item.name.contains(query, ignoreCase = true) ||
                item.sku.contains(query, ignoreCase = true) ||
                item.barcode.contains(query, ignoreCase = true) ||
                item.fitment.contains(query, ignoreCase = true) ||
                item.subcategory.contains(query, ignoreCase = true)
            matchesCategory && matchesSearch
        }
        InventoryUiState(
            items = filtered,
            searchQuery = query,
            selectedCategory = category,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InventoryUiState(isLoading = true)
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setLanguage(language: AppLanguage) {
        _selectedLanguage.value = language
    }

    fun toggleLanguage() {
        _selectedLanguage.value = if (_selectedLanguage.value == AppLanguage.ENGLISH) {
            AppLanguage.HINDI
        } else {
            AppLanguage.ENGLISH
        }
    }

    fun fetchItems() {
        // Triggers reactive data observation state update
    }

    fun addItem(item: InventoryItem, onComplete: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val id = repository.insertItem(item)
            onComplete?.invoke(id)
        }
    }

    fun updateItem(item: InventoryItem, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.updateItem(item)
            onComplete?.invoke()
        }
    }

    fun removeItem(item: InventoryItem, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.deleteItem(item)
            onComplete?.invoke()
        }
    }

    fun deleteItem(item: InventoryItem, onComplete: (() -> Unit)? = null) {
        removeItem(item, onComplete)
    }

    suspend fun adjustStock(
        itemId: Long,
        delta: Int,
        type: TransactionType = TransactionType.ADJUSTMENT,
        note: String = ""
    ): Result<InventoryItem> {
        return repository.adjustStock(itemId, delta, type, note)
    }

    suspend fun lookupByBarcodeOrSku(code: String): InventoryItem? {
        val item = repository.findByBarcodeOrSku(code) ?: return null
        return if (isCategoryAllowed(item.category, _showCarsTab.value, _showBikesTab.value, _showSparePartsTab.value)) {
            item
        } else {
            null
        }
    }

    private fun loadBillingHistory(items: List<InventoryItem>) {
        val saved = prefs.getString("billing_history_v2", null)
        if (!saved.isNullOrBlank()) {
            try {
                val itemsMap = items.associateBy { it.id }
                val list = saved.split(";;;").mapNotNull { line ->
                    if (line.isBlank()) null else {
                        val parts = line.split("|")
                        if (parts.size >= 9) {
                            val invoiceId = parts[0]
                            val customerName = parts[1]
                            val customerPhone = parts[2]
                            val subtotal = parts[3].toDoubleOrNull() ?: 0.0
                            val gst = parts[4].toDoubleOrNull() ?: 0.0
                            val total = parts[5].toDoubleOrNull() ?: 0.0
                            val paymentMethod = parts[6]
                            val timestamp = parts[7].toLongOrNull() ?: System.currentTimeMillis()
                            val itemsStr = parts[8]
                            val cartItems = if (itemsStr.isBlank()) emptyList() else {
                                itemsStr.split(",").mapNotNull { itemPart ->
                                    val ip = itemPart.split(":")
                                    if (ip.size == 2) {
                                        val itemId = ip[0].toLongOrNull() ?: return@mapNotNull null
                                        val quantity = ip[1].toIntOrNull() ?: return@mapNotNull null
                                        val item = itemsMap[itemId] ?: return@mapNotNull null
                                        com.example.data.model.CartItem(item, quantity)
                                    } else null
                                }
                            }
                            com.example.data.model.InvoiceReceipt(
                                invoiceId = invoiceId,
                                customerName = customerName,
                                customerPhone = customerPhone,
                                items = cartItems,
                                subtotal = subtotal,
                                gst = gst,
                                total = total,
                                paymentMethod = paymentMethod,
                                timestamp = timestamp
                            )
                        } else null
                    }
                }
                _rawBillingHistory.value = list.sortedByDescending { it.timestamp }
            } catch (_: Exception) {}
        }
    }

    fun addInvoiceReceipt(receipt: com.example.data.model.InvoiceReceipt) {
        val current = _rawBillingHistory.value.toMutableList()
        current.add(0, receipt)
        _rawBillingHistory.value = current

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val itemsStrList = current.map { r ->
                val itemsStr = r.items.joinToString(",") { "${it.item.id}:${it.quantity}" }
                "${r.invoiceId}|${r.customerName}|${r.customerPhone}|${r.subtotal}|${r.gst}|${r.total}|${r.paymentMethod}|${r.timestamp}|$itemsStr"
            }
            val serialized = itemsStrList.joinToString(";;;")
            prefs.edit().putString("billing_history_v2", serialized).apply()
        }
    }
}
