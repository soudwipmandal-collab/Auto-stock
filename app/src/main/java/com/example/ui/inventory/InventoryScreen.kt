package com.example.ui.inventory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.LightSurface
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ripple
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InventoryItem
import com.example.data.model.StockStatus
import com.example.data.model.TransactionType
import com.example.ui.barcode.BarcodeLabelDialog
import com.example.ui.theme.Amber500
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Cyan600
import com.example.ui.theme.Emerald500
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.PdfRed
import com.example.ui.theme.Red500
import com.example.ui.theme.RedContainer
import com.example.ui.theme.Saffron400
import com.example.ui.theme.Saffron500
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.ui.util.IndianFormatUtils
import com.example.ui.util.currentStrings
import kotlinx.coroutines.launch

enum class SortOption {
    NAME_ASC,
    NAME_DESC,
    STOCK_ASC,
    STOCK_DESC,
    PRICE_DESC,
    PRICE_ASC,
    RECENT
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun InventoryScreen(
    items: List<InventoryItem>,
    onAddItem: (InventoryItem) -> Unit,
    onUpdateItem: (InventoryItem) -> Unit,
    onDeleteItem: (InventoryItem) -> Unit,
    onAdjustStock: suspend (Long, Int, TransactionType, String) -> Result<InventoryItem>,
    onNavigateToScanner: () -> Unit,
    onViewItemDetails: (InventoryItem) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    showCars: Boolean = true,
    showBikes: Boolean = true,
    showSpareParts: Boolean = true
) {
    val strings = currentStrings()
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    var selectedStatusFilter by remember { mutableStateOf<StockStatus?>(null) }
    var selectedSortOption by remember { mutableStateOf(SortOption.RECENT) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }

    var isAddDialogOpen by remember { mutableStateOf(false) }
    var isExportPdfDialogOpen by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<InventoryItem?>(null) }
    var itemForBarcodeDialog by remember { mutableStateOf<InventoryItem?>(null) }

    data class CategoryTab(val id: String, val label: String, val icon: ImageVector)
    val categoryTabs = remember(showCars, showBikes, showSpareParts, strings) {
        buildList {
            add(CategoryTab("All", strings.catAll, Icons.Default.Inventory2))
            if (showCars) {
                add(CategoryTab("Cars", strings.catCars, Icons.Default.DirectionsCar))
            }
            if (showBikes) {
                add(CategoryTab("Bikes", strings.catBikes, Icons.Default.TwoWheeler))
            }
            if (showSpareParts) {
                add(CategoryTab("Spare Parts", strings.catSpareParts, Icons.Default.Build))
            }
        }
    }

    val safeCategoryIndex = remember(selectedCategoryIndex, categoryTabs) {
        selectedCategoryIndex.coerceIn(0, categoryTabs.size - 1)
    }

    // Filter and Sort Pipeline
    val filteredItems = remember(items, searchQuery, safeCategoryIndex, selectedStatusFilter, selectedSortOption, categoryTabs) {
        val selectedCategory = categoryTabs[safeCategoryIndex].id
        items.filter { item ->
            val matchesCategory = selectedCategory == "All" || item.category.equals(selectedCategory, ignoreCase = true)
            val matchesStatus = selectedStatusFilter == null || item.stockStatus == selectedStatusFilter
            val matchesSearch = searchQuery.isBlank() ||
                    item.name.contains(searchQuery, ignoreCase = true) ||
                    item.sku.contains(searchQuery, ignoreCase = true) ||
                    item.barcode.contains(searchQuery, ignoreCase = true) ||
                    item.fitment.contains(searchQuery, ignoreCase = true) ||
                    item.subcategory.contains(searchQuery, ignoreCase = true) ||
                    item.locationRack.contains(searchQuery, ignoreCase = true)

            matchesCategory && matchesStatus && matchesSearch
        }.let { list ->
            when (selectedSortOption) {
                SortOption.NAME_ASC -> list.sortedBy { it.name.lowercase() }
                SortOption.NAME_DESC -> list.sortedByDescending { it.name.lowercase() }
                SortOption.STOCK_ASC -> list.sortedBy { it.quantity }
                SortOption.STOCK_DESC -> list.sortedByDescending { it.quantity }
                SortOption.PRICE_DESC -> list.sortedByDescending { it.sellingPrice }
                SortOption.PRICE_ASC -> list.sortedBy { it.sellingPrice }
                SortOption.RECENT -> list.sortedByDescending { it.lastRestockedTimestamp }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("inventory_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Search Bar & Barcode Scan Shortcut Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Custom compact Search Bar matching Scanner capsule's size and shape
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("inventory_search_bar")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = strings.search,
                            tint = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = strings.searchPlaceholder,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            }
                            androidx.compose.foundation.text.BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Barcode Scanner Shortcut Button
                IconButton(
                    onClick = onNavigateToScanner,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Transparent, RoundedCornerShape(12.dp))
                        .border(1.dp, if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black, RoundedCornerShape(12.dp))
                        .testTag("nav_scanner_shortcut_btn")
                ) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = strings.scanCode,
                        tint = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Dynamic Category Dashboard Box
            val currentCategoryTab = categoryTabs[safeCategoryIndex]
            val categoryItems = remember(items, safeCategoryIndex) {
                if (currentCategoryTab.id == "All") {
                    items
                } else {
                    items.filter { it.category.equals(currentCategoryTab.id, ignoreCase = true) }
                }
            }

            val totalItemsCount = categoryItems.size
            val currentStockUnits = categoryItems.sumOf { it.quantity }
            val totalValuation = categoryItems.sumOf { it.totalValuationRetail }
            val lowStockCount = categoryItems.count { it.quantity <= it.minStockThreshold }

            var isDashboardCategoryMenuExpanded by remember { mutableStateOf(false) }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("inventory_lazy_column"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 84.dp)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .testTag("category_dashboard_card")
                    ) {
                        Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Row 1: Category title and Low Stock warning pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { isDashboardCategoryMenuExpanded = true }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = currentCategoryTab.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Category: ${currentCategoryTab.label}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Change Category",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = isDashboardCategoryMenuExpanded,
                                onDismissRequest = { isDashboardCategoryMenuExpanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                categoryTabs.forEachIndexed { index, tab ->
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = null,
                                                tint = if (selectedCategoryIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        text = {
                                            Text(
                                                text = tab.label,
                                                fontWeight = if (selectedCategoryIndex == index) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selectedCategoryIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                fontSize = 13.sp
                                            )
                                        },
                                        onClick = {
                                            selectedCategoryIndex = index
                                            isDashboardCategoryMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (lowStockCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Red500.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Red500.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Red500,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "$lowStockCount Low Stock",
                                        color = Red500,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Emerald500.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Emerald500,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "All in Stock",
                                        color = Emerald500,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Col 1: Total Items
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "Total Items",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$totalItemsCount",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Col 2: Current Stock
                        Column(
                            modifier = Modifier.weight(1.1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Current Stock",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$currentStockUnits Units",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Col 3: Total Stock Valuation
                        Column(
                            modifier = Modifier.weight(1.3f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "Total Stock Valuation",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = IndianFormatUtils.formatInr(totalValuation, compact = true),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }


                }
            }
            }

            // 2. Primary Category Tabs and Filter Chips as a COMBINED STICKY HEADER
            stickyHeader {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        TabRow(
                            selectedTabIndex = safeCategoryIndex,
                            containerColor = MaterialTheme.colorScheme.background,
                            contentColor = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black,
                            indicator = { tabPositions ->
                                if (safeCategoryIndex in tabPositions.indices) {
                                    TabRowDefaults.SecondaryIndicator(
                                        modifier = Modifier.tabIndicatorOffset(tabPositions[safeCategoryIndex]),
                                        color = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black,
                                        height = 2.5.dp
                                    )
                                }
                            },
                            divider = { Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.outlineVariant)) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            categoryTabs.forEachIndexed { index, tab ->
                                val isSelected = safeCategoryIndex == index
                                val count = if (tab.id == "All") items.size else items.count { it.category.equals(tab.id, ignoreCase = true) }
                                Tab(
                                    selected = isSelected,
                                    onClick = { selectedCategoryIndex = index },
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = null,
                                                tint = if (isSelected) (if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${tab.label} ($count)",
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) (if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LazyRow(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedStatusFilter == null,
                                    onClick = { selectedStatusFilter = null },
                                    label = { Text("${strings.catAll} (${categoryItems.size})", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color.Transparent,
                                        selectedLabelColor = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black,
                                        containerColor = Color.Transparent,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = BorderStroke(1.dp, if (selectedStatusFilter == null) (if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black) else MaterialTheme.colorScheme.outline)
                                )
                            }

                            item {
                                FilterChip(
                                    selected = selectedStatusFilter == StockStatus.IN_STOCK,
                                    onClick = {
                                        selectedStatusFilter = if (selectedStatusFilter == StockStatus.IN_STOCK) null else StockStatus.IN_STOCK
                                    },
                                    label = { Text(strings.inStock, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color.Transparent,
                                        selectedLabelColor = Emerald500,
                                        containerColor = Color.Transparent,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = BorderStroke(1.dp, if (selectedStatusFilter == StockStatus.IN_STOCK) Emerald500 else MaterialTheme.colorScheme.outline)
                                )
                            }

                            item {
                                FilterChip(
                                    selected = selectedStatusFilter == StockStatus.LOW_STOCK,
                                    onClick = {
                                        selectedStatusFilter = if (selectedStatusFilter == StockStatus.LOW_STOCK) null else StockStatus.LOW_STOCK
                                    },
                                    label = { Text(strings.lowStock, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color.Transparent,
                                        selectedLabelColor = Amber500,
                                        containerColor = Color.Transparent,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = BorderStroke(1.dp, if (selectedStatusFilter == StockStatus.LOW_STOCK) Amber500 else MaterialTheme.colorScheme.outline)
                                )
                            }

                            item {
                                FilterChip(
                                    selected = selectedStatusFilter == StockStatus.OUT_OF_STOCK,
                                    onClick = {
                                        selectedStatusFilter = if (selectedStatusFilter == StockStatus.OUT_OF_STOCK) null else StockStatus.OUT_OF_STOCK
                                    },
                                    label = { Text(strings.outOfStock, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color.Transparent,
                                        selectedLabelColor = Red500,
                                        containerColor = Color.Transparent,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = BorderStroke(1.dp, if (selectedStatusFilter == StockStatus.OUT_OF_STOCK) Red500 else MaterialTheme.colorScheme.outline)
                                )
                            }

                            // Sort Dropdown embedded inside the status panel row as a FilterChip
                            item {
                                Box {
                                    FilterChip(
                                        selected = true,
                                        onClick = { isSortMenuExpanded = true },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Sort,
                                                contentDescription = strings.sortBy,
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        },
                                        label = { Text(strings.sortBy, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                                            containerColor = Color.Transparent,
                                            labelColor = MaterialTheme.colorScheme.onSurface
                                        ),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                    )

                                    DropdownMenu(
                                        expanded = isSortMenuExpanded,
                                        onDismissRequest = { isSortMenuExpanded = false },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        val sortOptions = listOf(
                                            SortOption.RECENT to strings.recentStockLedger,
                                            SortOption.NAME_ASC to strings.sortNameAsc,
                                            SortOption.NAME_DESC to strings.sortNameDesc,
                                            SortOption.STOCK_ASC to strings.sortQtyLow,
                                            SortOption.STOCK_DESC to strings.sortQtyHigh,
                                            SortOption.PRICE_DESC to strings.sortPriceHigh,
                                            SortOption.PRICE_ASC to strings.sortPriceLow
                                        )
                                        sortOptions.forEach { (opt, label) ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = label,
                                                        fontWeight = if (selectedSortOption == opt) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (selectedSortOption == opt) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontSize = 13.sp
                                                    )
                                                },
                                                onClick = {
                                                    selectedSortOption = opt
                                                    isSortMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Compact PDF Export Capsule matching surrounding filter capsules
                        Surface(
                            onClick = { isExportPdfDialogOpen = true },
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, PdfRed.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .height(32.dp)
                                .width(32.dp)
                                .testTag("export_pdf_button")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = strings.exportPdf,
                                    tint = PdfRed,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Results count subhead
            item {
                Text(
                    text = "${filteredItems.size} ${if (filteredItems.size == 1) "item" else "items"}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }

            // 4. Items List
            if (filteredItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Inventory,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = strings.noItemsFound,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = strings.tryAdjustingFilters,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredItems, key = { it.id }) { item ->
                    InventoryItemCard(
                        item = item,
                        onQuickRestock = {
                            coroutineScope.launch {
                                val res = onAdjustStock(item.id, 1, TransactionType.STOCK_IN, strings.quickStockIn)
                                if (res.isSuccess) {
                                    snackbarHostState.showSnackbar("+1 -> ${item.name}")
                                }
                            }
                        },
                        onQuickStockOut = {
                            coroutineScope.launch {
                                val res = onAdjustStock(item.id, -1, TransactionType.STOCK_OUT, strings.quickStockOut)
                                if (res.isSuccess) {
                                    snackbarHostState.showSnackbar("-1 -> ${item.name}")
                                }
                            }
                        },
                        onClick = { onViewItemDetails(item) },
                        onShowBarcodeTag = { itemForBarcodeDialog = item },
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .testTag("inventory_item_${item.sku}")
                    )
                }
            }
        }
        }

        // Floating Action Button to Add New Item / Vehicle (Solid in Dark Theme, Liquid Glass in Light Theme)
        val isDark = MaterialTheme.colorScheme.surface == DarkSurface
        val fabShape = RoundedCornerShape(14.dp)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .size(56.dp)
                .shadow(
                    elevation = if (isDark) 8.dp else 12.dp,
                    shape = fabShape,
                    ambientColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.2f),
                    spotColor = if (isDark) Color.Black.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.35f)
                )
                .then(
                    if (isDark) {
                        Modifier
                            .background(DarkSurface, shape = fabShape)
                            .border(BorderStroke(1.5.dp, Color.White.copy(alpha = 0.25f)), shape = fabShape)
                    } else {
                        Modifier
                            .background(
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.95f),
                                        Color.White.copy(alpha = 0.60f)
                                    )
                                ),
                                shape = fabShape
                            )
                            .border(
                                BorderStroke(
                                    1.5.dp,
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.95f),
                                            Color.Black.copy(alpha = 0.15f)
                                        )
                                    )
                                ),
                                shape = fabShape
                            )
                    }
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, radius = 28.dp)
                ) {
                    isAddDialogOpen = true
                }
                .testTag("add_item_fab"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = strings.addItem,
                tint = if (isDark) Color.White else Color.Black,
                modifier = Modifier.size(28.dp)
            )
        }
    }

    // Add / Edit Item Dialog
    if (isAddDialogOpen || itemToEdit != null) {
        val currentCategoryTabId = categoryTabs.getOrNull(safeCategoryIndex)?.id
        val initialCategoryForDialog = if (currentCategoryTabId != null && currentCategoryTabId != "All") currentCategoryTabId else null

        AddEditItemDialog(
            itemToEdit = itemToEdit,
            initialCategory = initialCategoryForDialog,
            onDismiss = {
                isAddDialogOpen = false
                itemToEdit = null
            },
            onSave = { savedItem ->
                if (itemToEdit != null) {
                    onUpdateItem(savedItem)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(strings.save)
                    }
                } else {
                    onAddItem(savedItem)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(strings.addItem)
                    }
                }
                isAddDialogOpen = false
                itemToEdit = null
            },
            onDelete = { deletedItem ->
                onDeleteItem(deletedItem)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(strings.deleteItem)
                }
                itemToEdit = null
            },
            showCars = showCars,
            showBikes = showBikes,
            showSpareParts = showSpareParts
        )
    }

    // Barcode Label Dialog
    itemForBarcodeDialog?.let { item ->
        BarcodeLabelDialog(
            item = item,
            onDismiss = { itemForBarcodeDialog = null }
        )
    }

    // Export PDF Report Dialog
    if (isExportPdfDialogOpen) {
        ExportPdfDialog(
            allItems = items,
            initialCategory = categoryTabs[safeCategoryIndex].id,
            isRequirementReport = false,
            showCars = showCars,
            showBikes = showBikes,
            showSpareParts = showSpareParts,
            onDismiss = { 
                isExportPdfDialogOpen = false
            }
        )
    }
}

@Composable
fun InventoryItemCard(
    item: InventoryItem,
    onQuickRestock: () -> Unit,
    onQuickStockOut: () -> Unit,
    onClick: () -> Unit,
    onShowBarcodeTag: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = currentStrings()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            when {
                item.quantity <= 0 -> Red500.copy(alpha = 0.8f)
                item.quantity <= item.minStockThreshold -> Saffron500.copy(alpha = 0.6f)
                else -> Emerald500.copy(alpha = 0.35f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Low Stock Warning Banner when below user-defined threshold
            if (item.quantity <= item.minStockThreshold) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (item.quantity <= 0) RedContainer else AmberContainer,
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Stock Alert",
                        tint = if (item.quantity <= 0) Red500 else Saffron500,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (item.quantity <= 0) {
                            "${strings.outOfStock} - ${strings.reorderRecommended}"
                        } else {
                            "${strings.lowStock}: ${item.quantity} ${item.unit} <= ${strings.threshold} (${item.minStockThreshold})"
                        },
                        color = if (item.quantity <= 0) Red500 else Saffron400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Header Row: Category Badge, Location & Barcode action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        when (item.category) {
                            "Cars" -> Icons.Default.DirectionsCar
                            "Bikes" -> Icons.Default.TwoWheeler
                            else -> Icons.Default.Build
                        },
                        contentDescription = null,
                        tint = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${item.category.uppercase()} • ${item.subcategory}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.locationRack,
                        color = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .background(if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onShowBarcodeTag,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = "Barcode", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Item Name
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Fitment / Compatibility Subtitle
            if (item.fitment.isNotBlank()) {
                Text(
                    text = "${strings.fitment}: ${item.fitment}",
                    color = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // SKU and GST Info Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.sku,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "${IndianFormatUtils.getGstRateForCategory(item.category)}% GST (${IndianFormatUtils.getHsnCodeForCategory(item.category)})",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Metric & Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stock Status Pill Badge with Transparent Liquid Glass
                val statusColor = when {
                    item.quantity <= 0 -> Red500
                    item.quantity <= item.minStockThreshold -> Saffron500
                    else -> Emerald500
                }
                val statusTextColor = when {
                    item.quantity <= 0 -> Red500
                    item.quantity <= item.minStockThreshold -> Saffron400
                    else -> Emerald500
                }
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(
                                    statusColor.copy(alpha = 0.16f),
                                    statusColor.copy(alpha = 0.04f)
                                )
                            ),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .border(
                            BorderStroke(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(
                                        statusColor.copy(alpha = 0.45f),
                                        statusColor.copy(alpha = 0.15f)
                                    )
                                )
                            ),
                            shape = RoundedCornerShape(6.dp)
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(statusColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "${item.quantity} ${item.unit}",
                                color = statusTextColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when {
                                    item.quantity <= 0 -> strings.outOfStock
                                    item.quantity <= item.minStockThreshold -> "${strings.lowStock} (Min: ${item.minStockThreshold})"
                                    else -> strings.inStock
                                },
                                color = statusTextColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Retail Price in INR
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = IndianFormatUtils.formatInr(item.sellingPrice, compact = item.sellingPrice >= 100000),
                        color = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "₹ ${"%,.0f".format(item.costPrice)} ${strings.costPrice.take(2)}",
                        color = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                // Quick Increment / Decrement Stepper in a transparent liquid glass capsule
                val isDarkCard = MaterialTheme.colorScheme.surface == DarkSurface
                val stepperBgGradient = if (isDarkCard) {
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.14f),
                            Color.White.copy(alpha = 0.04f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.70f),
                            Color.White.copy(alpha = 0.30f)
                        )
                    )
                }

                val stepperShape = RoundedCornerShape(6.dp)

                Box(
                    modifier = Modifier
                        .width(76.dp)
                        .height(32.dp)
                        .background(
                            brush = stepperBgGradient,
                            shape = stepperShape
                        )
                        .border(
                            BorderStroke(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(
                                        if (isDarkCard) Color.White.copy(alpha = 0.40f) else Color.Black.copy(alpha = 0.20f),
                                        if (isDarkCard) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f)
                                    )
                                )
                            ),
                            shape = stepperShape
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Left 50% - Decrement (-)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .then(
                                    if (item.quantity > 0) {
                                        Modifier.clickable(onClick = onQuickStockOut)
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = strings.quickStockOut,
                                tint = if (item.quantity > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // 50-50 Vertical Glass Divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight(0.55f)
                                .background(if (isDarkCard) Color.White.copy(alpha = 0.20f) else Color.Black.copy(alpha = 0.12f))
                        )

                        // Right 50% - Increment (+) with subtle liquid glass highlight
                        val plusHighlightGradient = if (isDarkCard) {
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.25f),
                                    Color.White.copy(alpha = 0.08f)
                                )
                            )
                        } else {
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.12f),
                                    Color.Black.copy(alpha = 0.04f)
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(
                                    brush = plusHighlightGradient,
                                    shape = RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp)
                                )
                                .clickable(onClick = onQuickRestock),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = strings.quickStockIn,
                                tint = if (isDarkCard) Color.White else Color.Black,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
