package com.example.ui.inventory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
import com.example.ui.theme.DarkSurface
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InventoryItem
import com.example.data.model.StockStatus
import com.example.data.model.StockTransaction
import com.example.data.model.TransactionType
import com.example.ui.barcode.Barcode1DCanvas
import com.example.ui.barcode.BarcodeLabelDialog
import com.example.ui.theme.Amber500
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Cyan600
import com.example.ui.theme.Emerald500
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.Red500
import com.example.ui.theme.RedContainer
import com.example.ui.theme.Saffron400
import com.example.ui.theme.Saffron500
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

@Composable
fun ItemDetailDialog(
    item: InventoryItem,
    transactions: List<StockTransaction>,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onAdjustStock: suspend (Long, Int, TransactionType, String) -> Result<InventoryItem>
) {
    val strings = currentStrings()
    val coroutineScope = rememberCoroutineScope()
    var currentItem by remember { mutableStateOf(item) }
    var showBarcodeLabelDialog by remember { mutableStateOf(false) }
    var adjustDeltaText by remember { mutableStateOf("1") }
    var adjustNote by remember { mutableStateOf("") }
    var showAdjustDialog by remember { mutableStateOf(false) }
    var isRestocking by remember { mutableStateOf(true) }

    val itemTransactions = remember(transactions, currentItem.id) {
        transactions.filter { it.itemId == currentItem.id }
    }

    val statusLabel = when (currentItem.stockStatus) {
        StockStatus.OUT_OF_STOCK -> strings.outOfStock
        StockStatus.LOW_STOCK -> strings.lowStock
        StockStatus.IN_STOCK -> strings.inStock
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("item_detail_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = when (currentItem.stockStatus) {
                        StockStatus.OUT_OF_STOCK -> RedContainer
                        StockStatus.LOW_STOCK -> AmberContainer
                        StockStatus.IN_STOCK -> EmeraldContainer
                    },
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(
                        0.5.dp,
                        when (currentItem.stockStatus) {
                            StockStatus.OUT_OF_STOCK -> Red500.copy(alpha = 0.5f)
                            StockStatus.LOW_STOCK -> Amber500.copy(alpha = 0.5f)
                            StockStatus.IN_STOCK -> Emerald500.copy(alpha = 0.5f)
                        }
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    when (currentItem.stockStatus) {
                                        StockStatus.OUT_OF_STOCK -> Red500
                                        StockStatus.LOW_STOCK -> Amber500
                                        StockStatus.IN_STOCK -> Emerald500
                                    },
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusLabel.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (currentItem.stockStatus) {
                                StockStatus.OUT_OF_STOCK -> Red500
                                StockStatus.LOW_STOCK -> Amber500
                                StockStatus.IN_STOCK -> Emerald500
                            }
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = strings.editItem, tint = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = strings.cancel, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Item Header Info
                Text(
                    text = currentItem.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (currentItem.fitment.isNotBlank()) {
                    Text(
                        text = "${strings.fitment}: ${currentItem.fitment}",
                        color = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black,
                        fontSize = 12.sp
                    )
                }

                // Barcode Preview Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Barcode1DCanvas(
                            barcodeData = currentItem.barcode,
                            height = 42.dp,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${strings.sku}: ${currentItem.sku}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedButton(
                                onClick = { showBarcodeLabelDialog = true },
                                modifier = Modifier.height(30.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Icon(Icons.Default.QrCode, contentDescription = null, tint = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Shelf Tag", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                // Inventory Metrics Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricBox(
                        title = strings.inStock,
                        value = "${currentItem.quantity} ${currentItem.unit}",
                        subtitle = "${strings.threshold}: ${currentItem.minStockThreshold}",
                        color = when (currentItem.stockStatus) {
                            StockStatus.OUT_OF_STOCK -> Red500
                            StockStatus.LOW_STOCK -> Amber500
                            StockStatus.IN_STOCK -> Emerald500
                        },
                        modifier = Modifier.weight(1f)
                    )

                    MetricBox(
                        title = strings.location,
                        value = currentItem.locationRack,
                        subtitle = currentItem.category,
                        color = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Financial Valuation Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(strings.sellingPrice, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            Text(
                                text = IndianFormatUtils.formatInr(currentItem.sellingPrice, compact = false),
                                color = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(strings.costPrice, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            Text(
                                text = IndianFormatUtils.formatInr(currentItem.costPrice, compact = false),
                                color = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${strings.gstRate} & ${strings.hsnCode}:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            Text(
                                text = "${IndianFormatUtils.getGstRateForCategory(currentItem.category)}% (${IndianFormatUtils.getHsnCodeForCategory(currentItem.category)})",
                                color = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(strings.totalInventoryValue, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            Text(
                                text = IndianFormatUtils.formatInr(currentItem.totalValuationRetail, compact = currentItem.totalValuationRetail >= 100000),
                                color = Saffron500,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(strings.profitMargin, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            Text(
                                text = "${"%.1f".format(currentItem.profitMargin)}%",
                                color = Emerald500,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Description & Supplier
                if (currentItem.description.isNotBlank()) {
                    Text(
                        text = currentItem.description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }

                if (currentItem.supplier.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Supplier:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        Text(currentItem.supplier, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }

                // Stock Adjustment Action Buttons
                val isDark = MaterialTheme.colorScheme.surface == DarkSurface
                val stockInGlassGradient = if (isDark) {
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .background(
                                brush = stockInGlassGradient,
                                shape = RoundedCornerShape(8.dp)
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
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                isRestocking = true
                                adjustDeltaText = "5"
                                adjustNote = "Stock Receipt"
                                showAdjustDialog = true
                            },
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
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                strings.modeStockIn,
                                color = if (isDark) Color.White else Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Button(
                        onClick = {
                            isRestocking = false
                            adjustDeltaText = "1"
                            adjustNote = "Counter Sales / Dispatch"
                            showAdjustDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        enabled = currentItem.quantity > 0,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.modeStockOut, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Transaction History Ledger
                if (itemTransactions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${strings.stockAuditHistory} (${itemTransactions.size})",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        itemTransactions.take(5).forEach { txn ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = txn.reasonOrNote.ifBlank { "Stock Movement" },
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = IndianFormatUtils.formatDate(txn.timestamp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        )
                                    }

                                    Text(
                                        text = if (txn.quantityDelta > 0) "+${txn.quantityDelta}" else "${txn.quantityDelta}",
                                        color = if (txn.quantityDelta > 0) Emerald500 else Red500,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel, color = Saffron500)
            }
        }
    )

    // Stock Adjust Dialog
    if (showAdjustDialog) {
        AlertDialog(
            onDismissRequest = { showAdjustDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = if (isRestocking) strings.modeStockIn else strings.modeStockOut,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${strings.sku}: ${currentItem.name}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${strings.stockQuantity}: ${currentItem.quantity} ${currentItem.unit}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                    OutlinedTextField(
                        value = adjustDeltaText,
                        onValueChange = { adjustDeltaText = it.filter { c -> c.isDigit() } },
                        label = { Text("${strings.stockQuantity} (${currentItem.unit})") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = Saffron500,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    OutlinedTextField(
                        value = adjustNote,
                        onValueChange = { adjustNote = it },
                        label = { Text(strings.enterNotes) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = Saffron500,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = adjustDeltaText.toIntOrNull() ?: 1
                        if (qty > 0) {
                            val signedQty = if (isRestocking) qty else -qty
                            val type = if (isRestocking) TransactionType.STOCK_IN else TransactionType.STOCK_OUT
                            coroutineScope.launch {
                                val res = onAdjustStock(
                                    currentItem.id,
                                    signedQty,
                                    type,
                                    adjustNote.ifBlank { if (isRestocking) "Stock In" else "Stock Out" }
                                )
                                res.onSuccess { updated ->
                                    currentItem = updated
                                }
                            }
                        }
                        showAdjustDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isRestocking) (if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black) else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        if (isRestocking) strings.modeStockIn else strings.modeStockOut,
                        color = if (isRestocking) (if (MaterialTheme.colorScheme.surface == DarkSurface) Color.Black else Color.White) else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdjustDialog = false }) {
                    Text(strings.cancel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (showBarcodeLabelDialog) {
        BarcodeLabelDialog(
            item = currentItem,
            onDismiss = { showBarcodeLabelDialog = false }
        )
    }
}

@Composable
private fun MetricBox(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Text(text = title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
    }
}
