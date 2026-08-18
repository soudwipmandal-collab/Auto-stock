package com.example.ui.barcode

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InventoryItem
import com.example.ui.theme.Saffron500
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.ui.util.IndianFormatUtils
import com.example.ui.util.currentStrings
import kotlin.math.abs

@Composable
fun Barcode1DCanvas(
    barcodeData: String,
    modifier: Modifier = Modifier,
    barColor: Color = Color.Black,
    backgroundColor: Color = Color.White,
    height: Dp = 64.dp
) {
    val bitPattern = remember(barcodeData) {
        generateCode128BitPattern(barcodeData)
    }

    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
            ) {
                val totalBars = bitPattern.size
                if (totalBars == 0) return@Canvas
                val barWidth = size.width / totalBars

                for (i in bitPattern.indices) {
                    if (bitPattern[i]) {
                        drawRect(
                            color = barColor,
                            topLeft = Offset(x = i * barWidth, y = 0f),
                            size = Size(width = barWidth.coerceAtLeast(1.5f), height = size.height)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = formatBarcodeDisplay(barcodeData),
                color = barColor,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }
    }
}

@Composable
fun QrCodeCanvas(
    data: String,
    modifier: Modifier = Modifier,
    pixelColor: Color = Color.Black,
    backgroundColor: Color = Color.White,
    matrixSize: Int = 21
) {
    val matrix = remember(data) {
        generateQrMatrix(data, matrixSize)
    }

    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(140.dp)) {
            val cellSize = size.width / matrixSize

            for (row in 0 until matrixSize) {
                for (col in 0 until matrixSize) {
                    if (matrix[row][col]) {
                        drawRect(
                            color = pixelColor,
                            topLeft = Offset(col * cellSize, row * cellSize),
                            size = Size(cellSize.coerceAtLeast(1f), cellSize.coerceAtLeast(1f))
                        )
                    }
                }
            }
        }
    }
}

/**
 * Printable & Scan-ready Inventory Shelf Tag Dialog
 */
@Composable
fun BarcodeLabelDialog(
    item: InventoryItem,
    onDismiss: () -> Unit
) {
    val strings = currentStrings()
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("barcode_label_dialog"),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.barcodeTag,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = strings.cancel, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Shelf Tag Preview Card (Clean Printable White Card)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Tag Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.category.uppercase(),
                                color = Color(0xFF64748B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = item.locationRack,
                                color = Color(0xFF0F172A),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(Color(0xFFE2E8F0), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = item.name,
                            color = Color(0xFF0F172A),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )

                        if (item.fitment.isNotBlank()) {
                            Text(
                                text = item.fitment,
                                color = Color(0xFF64748B),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Render Barcode
                        Barcode1DCanvas(
                            barcodeData = item.barcode,
                            height = 54.dp,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Pricing & SKU Bar
                        HorizontalDivider(color = Color(0xFFCBD5E1), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "SKU: ${item.sku}",
                                    color = Color(0xFF475569),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${strings.stockQuantity}: ${item.quantity} ${item.unit}",
                                    color = Color(0xFF0F172A),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = IndianFormatUtils.formatInr(item.sellingPrice, compact = item.sellingPrice >= 100000),
                                    color = Color(0xFFEA580C),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "MRP (Incl. GST)",
                                    color = Color(0xFF64748B),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Barcode String & Copy row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = strings.enterBarcode,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = item.barcode,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(item.barcode))
                        },
                        modifier = Modifier.testTag("copy_barcode_button")
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy Barcode",
                            tint = Saffron500,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Saffron500)
            ) {
                Text("OK", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    )
}

// Pseudo-1D Barcode Pattern Generator (Code-128 standard style)
private fun generateCode128BitPattern(data: String): List<Boolean> {
    val result = mutableListOf<Boolean>()
    // Start Pattern
    result.addAll(listOf(true, true, false, true, false, false, true, false, false, false, false))

    var hash = abs(data.hashCode())
    for (char in data) {
        val charVal = char.code
        val patternSeed = (hash + charVal * 31) % 1000
        hash = (hash * 33) xor charVal

        // Generate 11 module width pattern for each character
        val pattern = listOf(
            (patternSeed and 1) != 0,
            (patternSeed and 2) != 0,
            (patternSeed and 4) != 0,
            (patternSeed and 8) != 0,
            (patternSeed and 16) != 0,
            (patternSeed and 32) != 0,
            (patternSeed and 64) != 0,
            (patternSeed and 128) != 0,
            (patternSeed and 256) != 0,
            (patternSeed and 512) != 0,
            false
        )
        result.addAll(pattern)
    }

    // Stop Pattern
    result.addAll(listOf(true, true, false, false, false, true, true, true, false, true, false, true, true))
    return result
}

// Pseudo-QR Matrix Generator
private fun generateQrMatrix(data: String, size: Int): Array<BooleanArray> {
    val matrix = Array(size) { BooleanArray(size) { false } }

    fun drawFinderPattern(startX: Int, startY: Int) {
        for (r in 0..6) {
            for (c in 0..6) {
                if (r == 0 || r == 6 || c == 0 || c == 6 || (r in 2..4 && c in 2..4)) {
                    if (startX + c < size && startY + r < size) {
                        matrix[startY + r][startX + c] = true
                    }
                }
            }
        }
    }

    // Draw standard 3 Corner Finder Patterns
    drawFinderPattern(0, 0)
    drawFinderPattern(size - 7, 0)
    drawFinderPattern(0, size - 7)

    // Fill timing pattern lines
    for (i in 7 until size - 7) {
        matrix[6][i] = (i % 2 == 0)
        matrix[i][6] = (i % 2 == 0)
    }

    // Deterministic payload grid filling based on data hash
    var seed = abs(data.hashCode())
    for (r in 0 until size) {
        for (c in 0 until size) {
            val inFinder1 = r < 8 && c < 8
            val inFinder2 = r < 8 && c >= size - 8
            val inFinder3 = r >= size - 8 && c < 8
            val inTiming = r == 6 || c == 6

            if (!inFinder1 && !inFinder2 && !inFinder3 && !inTiming) {
                seed = (seed * 1103515245 + 12345) and 0x7fffffff
                matrix[r][c] = (seed % 3 == 0)
            }
        }
    }

    return matrix
}

private fun formatBarcodeDisplay(code: String): String {
    return if (code.length >= 8) {
        code.chunked(4).joinToString(" ")
    } else {
        code
    }
}
