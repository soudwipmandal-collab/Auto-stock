package com.example.ui.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InventoryItem
import com.example.data.model.StockStatus
import com.example.data.model.StockTransaction
import com.example.ui.inventory.ExportPdfDialog
import com.example.ui.theme.Amber400
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.Cyan400
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.Cyan600
import com.example.ui.theme.Emerald500
import com.example.ui.theme.EmeraldContainer
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import com.example.ui.util.PdfReportGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Toast

@Composable
fun AnalyticsScreen(
    items: List<InventoryItem>,
    transactions: List<StockTransaction>,
    modifier: Modifier = Modifier,
    showCars: Boolean = true,
    showBikes: Boolean = true,
    showSpareParts: Boolean = true
) {
    val strings = currentStrings()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isExportPdfDialogOpen by remember { mutableStateOf(false) }
    var selectedPdfCategory by remember { mutableStateOf("All") }

    val totalItemsCount = remember(items) { items.sumOf { it.quantity } }
    val totalCostValuation = remember(items) { items.sumOf { it.totalValuationCost } }
    val totalRetailValuation = remember(items) { items.sumOf { it.totalValuationRetail } }
    val totalEstimatedProfit = remember(totalRetailValuation, totalCostValuation) { totalRetailValuation - totalCostValuation }
    val avgProfitMargin = remember(totalRetailValuation, totalEstimatedProfit) {
        if (totalRetailValuation > 0) (totalEstimatedProfit / totalRetailValuation) * 100 else 0.0
    }

    val cars = remember(items) { items.filter { it.category.equals("Cars", ignoreCase = true) } }
    val bikes = remember(items) { items.filter { it.category.equals("Bikes", ignoreCase = true) } }
    val parts = remember(items) { items.filter { it.category.equals("Spare Parts", ignoreCase = true) } }

    val inStockCount = remember(items) { items.count { it.stockStatus == StockStatus.IN_STOCK } }
    val lowStockCount = remember(items) { items.count { it.stockStatus == StockStatus.LOW_STOCK } }
    val outOfStockCount = remember(items) { items.count { it.stockStatus == StockStatus.OUT_OF_STOCK } }

    val sortedTransactions = remember(transactions) { transactions.sortedByDescending { it.timestamp } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .testTag("analytics_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 84.dp)
    ) {
        // 1. Valuation Hero Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White.copy(0.3f) else Color.Black.copy(0.2f))
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
                        Column {
                            Text(
                                text = strings.totalValuation.uppercase(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = IndianFormatUtils.formatInr(totalRetailValuation, compact = true),
                                color = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Surface(
                            onClick = {
                                selectedPdfCategory = "All"
                                isExportPdfDialogOpen = true
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, PdfRed.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("share_pdf_button")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = strings.sharePdf,
                                    tint = PdfRed,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Secondary Metrics: Cost Valuation & Projected Gross Profit
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(strings.costPrice, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = IndianFormatUtils.formatInr(totalCostValuation, compact = true),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldContainer,
                            border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(strings.marginProfit, color = Emerald500, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "+${"%.1f".format(avgProfitMargin)}% (${IndianFormatUtils.formatInr(totalEstimatedProfit, compact = true)})",
                                    color = Emerald500,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Health & Stock Status Segment Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = "${strings.totalItems}: $totalItemsCount units (${items.size} SKUs)",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Segmented color bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    ) {
                        if (inStockCount > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(inStockCount.toFloat())
                                    .fillMaxSize()
                                    .background(Emerald500)
                            )
                        }
                        if (lowStockCount > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(lowStockCount.toFloat())
                                    .fillMaxSize()
                                    .background(Saffron500)
                            )
                        }
                        if (outOfStockCount > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(outOfStockCount.toFloat())
                                    .fillMaxSize()
                                    .background(Red500)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatusLegendItem(strings.inStock, inStockCount, Emerald500)
                        StatusLegendItem(strings.lowStock, lowStockCount, Saffron400)
                        StatusLegendItem(strings.outOfStock, outOfStockCount, Red500)
                    }
                }
            }
        }

        // 3. Category Breakdown (Cars, Bikes, Spare Parts)
        if (showCars || showBikes || showSpareParts) {
            item {
                Text(
                    text = strings.selectCategory,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showCars) {
                        CategoryStatCard(
                            title = strings.catCars,
                            count = cars.sumOf { it.quantity },
                            value = cars.sumOf { it.totalValuationRetail },
                            icon = Icons.Default.DirectionsCar,
                            color = Saffron500,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (showBikes) {
                        CategoryStatCard(
                            title = strings.catBikes,
                            count = bikes.sumOf { it.quantity },
                            value = bikes.sumOf { it.totalValuationRetail },
                            icon = Icons.Default.TwoWheeler,
                            color = Cyan600,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (showSpareParts) {
                        CategoryStatCard(
                            title = strings.catSpareParts,
                            count = parts.sumOf { it.quantity },
                            value = parts.sumOf { it.totalValuationRetail },
                            icon = Icons.Default.Build,
                            color = Emerald500,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 4. Stock Movement Ledger
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.auditHistory,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${transactions.size} records",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }

                if (transactions.isNotEmpty()) {
                    Surface(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    val pdfFile = withContext(Dispatchers.IO) {
                                        PdfReportGenerator.generateLedgerPdfReport(
                                            context = context,
                                            transactions = transactions.sortedByDescending { it.timestamp },
                                            scopeName = "Stock Movement Ledger"
                                        )
                                    }
                                    PdfReportGenerator.sharePdfFile(context, pdfFile, "Stock Movement Ledger")
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error exporting Ledger PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, PdfRed.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("export_ledger_pdf_btn")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "Export Ledger PDF",
                                tint = PdfRed,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }

        if (transactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.noTransactions,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            items(sortedTransactions, key = { it.id }) { txn ->
                val isIn = txn.quantityDelta > 0
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(
                                        if (isIn) EmeraldContainer else RedContainer,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isIn) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (isIn) Emerald500 else Red500,
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = txn.itemName,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = txn.reasonOrNote.ifBlank { if (isIn) strings.quickStockIn else strings.quickStockOut },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = IndianFormatUtils.formatDate(txn.timestamp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isIn) "+${txn.quantityDelta}" else "${txn.quantityDelta}",
                                color = if (isIn) Emerald500 else Red500,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Bal: ${txn.newQuantity}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (isExportPdfDialogOpen) {
        ExportPdfDialog(
            allItems = items,
            initialCategory = selectedPdfCategory,
            showCars = showCars,
            showBikes = showBikes,
            showSpareParts = showSpareParts,
            onDismiss = { isExportPdfDialogOpen = false }
        )
    }
}

@Composable
private fun StatusLegendItem(label: String, count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$label: $count",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CategoryStatCard(
    title: String,
    count: Int,
    value: Double,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text("$count Units", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(
                text = IndianFormatUtils.formatInr(value, compact = true),
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
