package com.example.ui.alerts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InventoryItem
import com.example.data.model.StockStatus
import com.example.data.model.TransactionType
import com.example.ui.barcode.BarcodeLabelDialog
import com.example.ui.inventory.ExportPdfDialog
import com.example.ui.theme.Amber400
import com.example.ui.theme.Amber500
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.Cyan400
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.Emerald500
import com.example.ui.theme.PdfRed
import com.example.ui.theme.Red500
import com.example.ui.theme.RedContainer
import com.example.ui.theme.Saffron400
import com.example.ui.theme.Saffron500
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.ui.util.IndianFormatUtils
import com.example.ui.util.currentStrings
import kotlinx.coroutines.launch

enum class AlertFilter {
    ALL,
    OUT_OF_STOCK,
    LOW_STOCK
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StockAlertsScreen(
    alertItems: List<InventoryItem>,
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
    var selectedFilter by remember { mutableStateOf(AlertFilter.ALL) }
    var isExportPdfDialogOpen by remember { mutableStateOf(false) }
    var itemForQuickRestock by remember { mutableStateOf<InventoryItem?>(null) }
    var itemForBarcodeDialog by remember { mutableStateOf<InventoryItem?>(null) }

    val filteredAlerts = remember(alertItems, selectedFilter) {
        when (selectedFilter) {
            AlertFilter.ALL -> alertItems
            AlertFilter.OUT_OF_STOCK -> alertItems.filter { it.stockStatus == StockStatus.OUT_OF_STOCK }
            AlertFilter.LOW_STOCK -> alertItems.filter { it.stockStatus == StockStatus.LOW_STOCK }
        }
    }

    val outOfStockCount = remember(alertItems) { alertItems.count { it.stockStatus == StockStatus.OUT_OF_STOCK } }
    val lowStockCount = remember(alertItems) { alertItems.count { it.stockStatus == StockStatus.LOW_STOCK } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .testTag("stock_alerts_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 84.dp)
    ) {
        // 1. Header Alert Summary Hero Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    1.dp,
                    if (outOfStockCount > 0) Red500.copy(alpha = 0.5f) else Saffron500.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (outOfStockCount > 0) RedContainer else AmberContainer,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (outOfStockCount > 0) Icons.Default.Warning else Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = if (outOfStockCount > 0) Red500 else Saffron500,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (alertItems.isEmpty()) strings.stockHealthy else strings.reorderRequired,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = strings.stockHealthyDesc,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (alertItems.isNotEmpty()) {
                                Surface(
                                    onClick = { isExportPdfDialogOpen = true },
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Transparent,
                                    border = BorderStroke(1.dp, PdfRed.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("export_alerts_pdf_btn")
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PictureAsPdf,
                                            contentDescription = strings.pdfExportTitle,
                                            tint = PdfRed,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            // Barcode scan shortcut
                            Surface(
                                onClick = onNavigateToScanner,
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Transparent,
                                border = BorderStroke(1.dp, if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Scan",
                                        tint = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Alert stats count row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = RedContainer,
                            border = BorderStroke(1.dp, Red500.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(strings.outOfStock, fontSize = 10.sp, color = Red500, fontWeight = FontWeight.Bold)
                                    Text("0 units", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("$outOfStockCount", fontSize = 18.sp, color = Red500, fontWeight = FontWeight.Black)
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = AmberContainer,
                            border = BorderStroke(1.dp, Amber500.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(strings.lowStock, fontSize = 10.sp, color = Amber400, fontWeight = FontWeight.Bold)
                                    Text("< Min limit", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("$lowStockCount", fontSize = 18.sp, color = Amber400, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }

        // 1.5. Reorder Requirements Alert block
        if (alertItems.isNotEmpty()) {
            item {
                val totalRequiredStock = remember(alertItems) { alertItems.sumOf { it.requiredStock } }
                val reqValuation = remember(alertItems) { alertItems.sumOf { it.requiredStockValuationRetail } }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Red500.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, Red500.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Red500,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Reorder Requirements Alert",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Red500
                                )
                            }
                            
                            // Quick PDF export button
                            OutlinedButton(
                                onClick = {
                                    isExportPdfDialogOpen = true
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Red500
                                ),
                                border = BorderStroke(1.dp, Red500.copy(alpha = 0.4f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Reorder Items
                            Column(modifier = Modifier.weight(1.1f)) {
                                Text(
                                    text = "Reorder Items",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${alertItems.size} SKU",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            // Units Needed
                            Column(modifier = Modifier.weight(1.1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Units Needed",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$totalRequiredStock Units",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            // Est. Valuation Cost
                            Column(modifier = Modifier.weight(1.3f), horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Required Valuation",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = IndianFormatUtils.formatInr(reqValuation, compact = true),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Filter Selector Chips
        stickyHeader {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    AlertFilter.ALL to "${strings.allAlerts} (${alertItems.size})",
                    AlertFilter.OUT_OF_STOCK to "${strings.outOfStock} ($outOfStockCount)",
                    AlertFilter.LOW_STOCK to "${strings.lowStock} ($lowStockCount)"
                )
                filters.forEach { (filter, label) ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color.Transparent,
                            selectedLabelColor = if (filter == AlertFilter.OUT_OF_STOCK) Red500 else if (filter == AlertFilter.LOW_STOCK) Amber500 else (if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black),
                            containerColor = Color.Transparent,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = BorderStroke(1.dp, if (isSelected) (if (filter == AlertFilter.OUT_OF_STOCK) Red500 else if (filter == AlertFilter.LOW_STOCK) Amber500 else (if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black)) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    )
                }
            }
        }

        // 3. Alert Items List
        if (filteredAlerts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Emerald500,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = strings.stockHealthy,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.stockHealthyDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }
        } else {
            items(filteredAlerts, key = { it.id }) { item ->
                AlertItemCard(
                    item = item,
                    onRestockClick = { itemForQuickRestock = item },
                    onClick = { onViewItemDetails(item) },
                    onShowBarcode = { itemForBarcodeDialog = item }
                )
            }
        }
    }

    // Quick Reorder Modal
    itemForQuickRestock?.let { item ->
        var restockQuantityText by remember { mutableStateOf("10") }
        var restockNote by remember { mutableStateOf("Purchase Challan") }

        AlertDialog(
            onDismissRequest = { itemForQuickRestock = null },
            shape = RoundedCornerShape(14.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "${strings.quickStockIn}: ${item.name}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${strings.sku}: ${item.sku} • ${strings.location}: ${item.locationRack}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${strings.stockQuantity}: ${item.quantity} ${item.unit} (Min: ${item.minStockThreshold})",
                        color = if (item.quantity == 0) Red500 else Saffron400,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )

                    OutlinedButton(
                        onClick = { restockQuantityText = "10" },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("+10 Standard Pack", color = Saffron400, fontSize = 12.sp)
                    }

                    OutlinedTextField(
                        value = restockQuantityText,
                        onValueChange = { restockQuantityText = it.filter { c -> c.isDigit() } },
                        label = { Text("${strings.stockQuantity} (${item.unit})") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = restockNote,
                        onValueChange = { restockNote = it },
                        label = { Text(strings.notes) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = restockQuantityText.toIntOrNull() ?: 10
                        if (qty > 0) {
                            coroutineScope.launch {
                                val res = onAdjustStock(item.id, qty, TransactionType.STOCK_IN, restockNote)
                                if (res.isSuccess) {
                                    snackbarHostState.showSnackbar("Restocked +$qty ${item.unit} to ${item.name}")
                                }
                            }
                        }
                        itemForQuickRestock = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Saffron500),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(strings.save, color = Slate950, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemForQuickRestock = null }) {
                    Text(strings.cancel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    itemForBarcodeDialog?.let { item ->
        BarcodeLabelDialog(
            item = item,
            onDismiss = { itemForBarcodeDialog = null }
        )
    }

    if (isExportPdfDialogOpen) {
        ExportPdfDialog(
            allItems = alertItems,
            initialCategory = "All",
            isRequirementReport = true,
            showCars = showCars,
            showBikes = showBikes,
            showSpareParts = showSpareParts,
            onDismiss = { isExportPdfDialogOpen = false }
        )
    }
}

@Composable
fun AlertItemCard(
    item: InventoryItem,
    onRestockClick: () -> Unit,
    onClick: () -> Unit,
    onShowBarcode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = currentStrings()
    val isOut = item.stockStatus == StockStatus.OUT_OF_STOCK

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (isOut) Red500.copy(alpha = 0.5f) else Saffron500.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Status badge & Rack
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (isOut) RedContainer else AmberContainer,
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = (if (isOut) strings.outOfStock else strings.lowStock).uppercase(),
                            color = if (isOut) Red500 else Saffron400,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        when (item.category) {
                            "Cars" -> Icons.Default.DirectionsCar
                            "Bikes" -> Icons.Default.TwoWheeler
                            else -> Icons.Default.Build
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.category.uppercase(),
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
                        fontWeight = FontWeight.Medium
                    )
                    IconButton(onClick = onShowBarcode, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.QrCode, contentDescription = "Barcode", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Item Name
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Fitment subtitle
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

            Spacer(modifier = Modifier.height(10.dp))

            // Current Stock vs Threshold Row & Restock Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${strings.stockQuantity}: ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        Text(
                            text = "${item.quantity} ${item.unit}",
                            color = if (isOut) Red500 else Saffron400,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = "Min: ${item.minStockThreshold}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }

                // Retail Price in INR (₹)
                Text(
                    text = IndianFormatUtils.formatInr(item.sellingPrice, compact = item.sellingPrice >= 100000),
                    color = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                // Quick Restock Button (Liquid Glass)
                val isDark = MaterialTheme.colorScheme.surface == DarkSurface
                val restockBgGradient = if (isDark) {
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.85f),
                            Color.White.copy(alpha = 0.40f)
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .background(
                            brush = restockBgGradient,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .border(
                            BorderStroke(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(
                                        if (isDark) Color.White.copy(alpha = 0.50f) else Color.Black.copy(alpha = 0.20f),
                                        if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f)
                                    )
                                )
                            ),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable(onClick = onRestockClick)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = if (isDark) Color.White else Color.Black,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            strings.quickStockIn,
                            color = if (isDark) Color.White else Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
