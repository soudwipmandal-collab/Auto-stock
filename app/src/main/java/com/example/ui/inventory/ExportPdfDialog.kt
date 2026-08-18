package com.example.ui.inventory

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import com.example.ui.theme.DarkSurface
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.InventoryItem
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Cyan600
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
import com.example.ui.util.PdfReportGenerator
import com.example.ui.util.currentStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class PdfCategoryOption(
    val id: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun ExportPdfDialog(
    allItems: List<InventoryItem>,
    initialCategory: String = "All",
    isRequirementReport: Boolean = false,
    showCars: Boolean = true,
    showBikes: Boolean = true,
    showSpareParts: Boolean = true,
    onDismiss: () -> Unit
) {
    val strings = currentStrings()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val categories = remember(showCars, showBikes, showSpareParts, strings) {
        buildList {
            add(PdfCategoryOption("All", strings.catAll, Icons.Default.Inventory2))
            if (showCars) {
                add(PdfCategoryOption("Cars", strings.catCars, Icons.Default.DirectionsCar))
            }
            if (showBikes) {
                add(PdfCategoryOption("Bikes", strings.catBikes, Icons.Default.TwoWheeler))
            }
            if (showSpareParts) {
                add(PdfCategoryOption("Spare Parts", strings.catSpareParts, Icons.Default.Build))
            }
        }
    }

    val safeInitialCategory = remember(categories, initialCategory) {
        if (categories.any { it.id.equals(initialCategory, ignoreCase = true) }) {
            initialCategory
        } else {
            "All"
        }
    }

    var selectedCategory by remember { mutableStateOf(safeInitialCategory) }
    var isGeneratingPdf by remember { mutableStateOf(false) }

    // Items for selected category
    val exportItems = remember(selectedCategory, allItems) {
        if (selectedCategory.equals("All", ignoreCase = true)) {
            allItems
        } else {
            allItems.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }
    }

    val totalSkus = exportItems.size
    val totalUnits = if (isRequirementReport) exportItems.sumOf { it.requiredStock } else exportItems.sumOf { it.quantity }
    val totalValuation = if (isRequirementReport) exportItems.sumOf { it.requiredStockValuationRetail } else exportItems.sumOf { it.totalValuationRetail }
    val belowThresholdCount = exportItems.count { it.quantity <= it.minStockThreshold }

    fun executePdfAction(action: (File) -> Unit) {
        if (isGeneratingPdf) return
        isGeneratingPdf = true

        coroutineScope.launch {
            try {
                val pdfFile = withContext(Dispatchers.IO) {
                    if (isRequirementReport) {
                        PdfReportGenerator.generateRequirementPdfReport(
                            context = context,
                            categoryName = if (selectedCategory == "All") "All Inventory" else selectedCategory,
                            items = exportItems
                        )
                    } else {
                        PdfReportGenerator.generateInventoryPdfReport(
                            context = context,
                            categoryName = if (selectedCategory == "All") "All Inventory" else selectedCategory,
                            items = exportItems
                        )
                    }
                }
                isGeneratingPdf = false
                action(pdfFile)
            } catch (e: Exception) {
                isGeneratingPdf = false
                Toast.makeText(context, "Error generating PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!isGeneratingPdf) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("export_pdf_dialog"),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(RedContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = Red500,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isRequirementReport) "Export Requirement Report" else strings.pdfExportTitle,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isRequirementReport) "Export item requirements & replenishment values" else strings.pdfExportSubtitle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isGeneratingPdf,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = strings.cancel, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Select Category Chips
                Text(
                    text = strings.selectCategory.uppercase(),
                    color = Saffron500,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        val isSelected = selectedCategory.equals(cat.id, ignoreCase = true)
                        val count = if (cat.id == "All") allItems.size else allItems.count { it.category.equals(cat.id, ignoreCase = true) }
                        
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat.id },
                            leadingIcon = {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = "${cat.label} ($count)",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color.Transparent,
                                selectedLabelColor = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black,
                                selectedLeadingIconColor = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black,
                                containerColor = Color.Transparent,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = BorderStroke(1.dp, if (isSelected) (if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black) else MaterialTheme.colorScheme.outline),
                            modifier = Modifier.testTag("pdf_chip_${cat.id.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scope Preview Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${strings.category}: $selectedCategory",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (belowThresholdCount > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(RedContainer, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Red500, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("$belowThresholdCount ${strings.lowStock}", color = Red500, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(strings.totalItems, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                Text("$totalSkus", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text(
                                    text = if (isRequirementReport) strings.requiredStockUnits else strings.stockQuantity,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                                Text(
                                    "$totalUnits Units",
                                    color = if (isRequirementReport && totalUnits > 0) Red500 else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (isRequirementReport) strings.requiredStockValuation else strings.totalValuation,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                                Text(
                                    IndianFormatUtils.formatInr(totalValuation, compact = true),
                                    color = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Loading Indicator if Generating
                if (isGeneratingPdf) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Saffron500
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = strings.pdfGenerating,
                            color = Saffron500,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Action Buttons: Share, Print, View
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Share PDF Button
                    OutlinedButton(
                        onClick = {
                            executePdfAction { file ->
                                PdfReportGenerator.sharePdfFile(context, file, selectedCategory)
                            }
                        },
                        enabled = !isGeneratingPdf && exportItems.isNotEmpty(),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_pdf_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.sharePdf, color = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Print PDF Button
                    Button(
                        onClick = {
                            executePdfAction { file ->
                                PdfReportGenerator.printPdfFile(context, file, selectedCategory)
                            }
                        },
                        enabled = !isGeneratingPdf && exportItems.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan600),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("print_pdf_button")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "Print", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.printPdf, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Preview / Open PDF Button
                OutlinedButton(
                    onClick = {
                        executePdfAction { file ->
                            PdfReportGenerator.viewPdfFile(context, file)
                        }
                    },
                    enabled = !isGeneratingPdf && exportItems.isNotEmpty(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("view_pdf_button")
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = "Preview", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.previewPdf, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                }
            }
        }
    }
}
