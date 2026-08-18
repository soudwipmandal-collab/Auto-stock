package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.InventoryItem
import com.example.data.model.StockStatus
import com.example.data.model.StockTransaction
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    private const val PAGE_WIDTH = 595 // A4 standard width in points (72 dpi)
    private const val PAGE_HEIGHT = 842 // A4 standard height in points (72 dpi)
    private const val MARGIN_X = 28f
    private const val MARGIN_TOP = 28f
    private const val MARGIN_BOTTOM = 36f

    /**
     * Generates a beautifully formatted PDF report for the given items and category.
     * Returns the generated File object.
     */
    fun generateInventoryPdfReport(
        context: Context,
        categoryName: String,
        items: List<InventoryItem>,
        includeValuation: Boolean = true,
        reportTitle: String = "Inventory Stock & Valuation Report"
    ): File {
        val reportsDir = File(context.cacheDir, "reports").apply {
            if (!exists()) mkdirs()
        }

        val sanitizedCategory = categoryName.replace("\\s+".toRegex(), "_")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
        val pdfFile = File(reportsDir, "Inventory_${sanitizedCategory}_$timestamp.pdf")

        val document = PdfDocument()

        // Setup Paints
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42) // Slate 900
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(254, 215, 170) // Saffron 200
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
            color = Color.rgb(226, 232, 240) // Slate 200
        }

        // Summary Calculations based on Available Current Stock
        val totalSkus = items.size
        val totalUnits = items.sumOf { it.quantity }
        val totalRetailValuation = items.sumOf { it.totalValuationRetail }
        val totalCostValuation = items.sumOf { it.totalValuationCost }
        val lowStockCount = items.count { it.quantity <= it.minStockThreshold }
        val outOfStockCount = items.count { it.quantity <= 0 }
        val formattedDate = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.ENGLISH).format(Date())

        // Row height & Pagination math
        val headerHeight = 110f
        val kpiSectionHeight = 52f
        val tableHeaderHeight = 22f
        val rowHeight = 24f

        val usableWidth = PAGE_WIDTH - (MARGIN_X * 2)

        // Column widths - focused on Available Current Stock and Valuation
        // Columns: [S.No (20), SKU/Barcode (65), Item Description (134), Subcat (55), Rack (38), Qty (38), Unit Price (55), Total Value (70), Status (60)]
        val colW = floatArrayOf(20f, 65f, 134f, 55f, 38f, 38f, 55f, 70f, 60f)
        val colTitles = arrayOf("#", "SKU / Code", "Item Description", "Subcategory", "Rack", "Qty", "Price (₹)", "Valuation (₹)", "Status")

        // Calculate rows per page
        val firstPageContentTop = MARGIN_TOP + headerHeight + kpiSectionHeight + 10f
        val subsequentPageContentTop = MARGIN_TOP + 40f

        val firstPageMaxRows = ((PAGE_HEIGHT - MARGIN_BOTTOM - firstPageContentTop - tableHeaderHeight) / rowHeight).toInt().coerceAtLeast(1)
        val subsequentPageMaxRows = ((PAGE_HEIGHT - MARGIN_BOTTOM - subsequentPageContentTop - tableHeaderHeight) / rowHeight).toInt().coerceAtLeast(1)

        val totalPages = if (items.isEmpty()) {
            1
        } else if (items.size <= firstPageMaxRows) {
            1
        } else {
            1 + ((items.size - firstPageMaxRows + subsequentPageMaxRows - 1) / subsequentPageMaxRows)
        }

        var itemIndex = 0

        for (pageNumber in 1..totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            var currentY = MARGIN_TOP

            if (pageNumber == 1) {
                // 1. Top Header Banner (Deep Slate with Saffron Accent)
                bgPaint.color = Color.rgb(15, 23, 42) // Slate 900
                canvas.drawRoundRect(RectF(MARGIN_X, currentY, PAGE_WIDTH - MARGIN_X, currentY + 70f), 8f, 8f, bgPaint)

                // Saffron Accent Strip on left edge of header
                bgPaint.color = Color.rgb(255, 153, 51) // Saffron 500
                canvas.drawRoundRect(RectF(MARGIN_X, currentY, MARGIN_X + 6f, currentY + 70f), 4f, 4f, bgPaint)

                // Header Title & Meta
                canvas.drawText("AutoStock Pro™ | Automotive Inventory Management", MARGIN_X + 16f, currentY + 22f, titlePaint)
                canvas.drawText("INVENTORY STOCK & VALUATION AUDIT: ${categoryName.uppercase()}", MARGIN_X + 16f, currentY + 38f, subtitlePaint)
                
                val datePaint = Paint(subtitlePaint).apply { color = Color.rgb(203, 213, 225) }
                canvas.drawText("Generated: $formattedDate • Scope: $categoryName Catalog • Available Stock Basis", MARGIN_X + 16f, currentY + 54f, datePaint)

                currentY += 78f

                // 2. KPI Summary Cards (4 Cards based on Current Available Stock)
                val kpiCardWidth = (usableWidth - (3 * 8f)) / 4f
                val kpiCardHeight = 44f

                // Card 1: Total SKUs
                drawKpiCard(
                    canvas, RectF(MARGIN_X, currentY, MARGIN_X + kpiCardWidth, currentY + kpiCardHeight),
                    "TOTAL SKUs", "$totalSkus Items", Color.rgb(241, 245, 249), Color.rgb(30, 41, 59)
                )

                // Card 2: Current Available Stock Units
                val c2Left = MARGIN_X + kpiCardWidth + 8f
                drawKpiCard(
                    canvas, RectF(c2Left, currentY, c2Left + kpiCardWidth, currentY + kpiCardHeight),
                    "AVAILABLE UNITS", "$totalUnits Units", Color.rgb(241, 245, 249), Color.rgb(30, 41, 59)
                )

                // Card 3: Total Stock Valuation in INR
                val c3Left = c2Left + kpiCardWidth + 8f
                drawKpiCard(
                    canvas, RectF(c3Left, currentY, c3Left + kpiCardWidth, currentY + kpiCardHeight),
                    "TOTAL VALUATION", IndianFormatUtils.formatInr(totalRetailValuation, compact = true),
                    Color.rgb(254, 243, 199), Color.rgb(180, 83, 9)
                )

                // Card 4: Low Stock Alert
                val c4Left = c3Left + kpiCardWidth + 8f
                val alertBgColor = if (lowStockCount > 0) Color.rgb(254, 226, 226) else Color.rgb(220, 252, 231)
                val alertTextColor = if (lowStockCount > 0) Color.rgb(220, 38, 38) else Color.rgb(22, 101, 52)
                drawKpiCard(
                    canvas, RectF(c4Left, currentY, c4Left + kpiCardWidth, currentY + kpiCardHeight),
                    "LOW / OUT OF STOCK", "$lowStockCount Alerts ($outOfStockCount Out)", alertBgColor, alertTextColor
                )

                currentY += kpiCardHeight + 14f
            } else {
                // Secondary Page Header
                bgPaint.color = Color.rgb(15, 23, 42)
                canvas.drawRect(RectF(MARGIN_X, currentY, PAGE_WIDTH - MARGIN_X, currentY + 24f), bgPaint)

                val miniHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 9f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText("AutoStock Pro™ | $categoryName Inventory Stock Report (Continued)", MARGIN_X + 8f, currentY + 16f, miniHeaderPaint)

                val miniDatePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(203, 213, 225)
                    textSize = 8f
                }
                val dateStr = formattedDate
                val dateWidth = miniDatePaint.measureText(dateStr)
                canvas.drawText(dateStr, PAGE_WIDTH - MARGIN_X - dateWidth - 8f, currentY + 16f, miniDatePaint)

                currentY += 30f
            }

            // 3. Table Column Headers
            bgPaint.color = Color.rgb(30, 41, 59) // Slate 800
            canvas.drawRoundRect(RectF(MARGIN_X, currentY, PAGE_WIDTH - MARGIN_X, currentY + tableHeaderHeight), 4f, 4f, bgPaint)

            var colX = MARGIN_X
            val thPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 8f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            colTitles.forEachIndexed { idx, title ->
                val w = colW[idx]
                val alignRight = idx in 5..7
                val alignCenter = idx == 8
                val textX = if (alignRight) {
                    val tw = thPaint.measureText(title)
                    colX + w - tw - 4f
                } else if (alignCenter) {
                    val tw = thPaint.measureText(title)
                    colX + (w - tw) / 2f
                } else {
                    colX + 4f
                }
                canvas.drawText(title, textX, currentY + 14f, thPaint)
                colX += w
            }

            currentY += tableHeaderHeight + 2f

            // 4. Render Table Rows for this page
            val rowsLimit = if (pageNumber == 1) firstPageMaxRows else subsequentPageMaxRows
            var rowsRenderedThisPage = 0

            while (itemIndex < items.size && rowsRenderedThisPage < rowsLimit) {
                val item = items[itemIndex]
                val isEven = rowsRenderedThisPage % 2 == 0

                // Row Background (Zebra Striping)
                bgPaint.color = if (item.quantity <= 0) {
                    Color.rgb(254, 242, 242) // Light red tint for Out of Stock
                } else if (item.quantity <= item.minStockThreshold) {
                    Color.rgb(254, 243, 199) // Light amber tint for Low Stock
                } else if (isEven) {
                    Color.rgb(248, 250, 252) // Light slate 50
                } else {
                    Color.WHITE
                }

                canvas.drawRect(RectF(MARGIN_X, currentY, PAGE_WIDTH - MARGIN_X, currentY + rowHeight), bgPaint)

                // Row bottom border
                strokePaint.color = Color.rgb(226, 232, 240)
                canvas.drawLine(MARGIN_X, currentY + rowHeight, PAGE_WIDTH - MARGIN_X, currentY + rowHeight, strokePaint)

                // Cells
                colX = MARGIN_X

                // 1. S.No
                textPaint.color = Color.rgb(100, 116, 139)
                canvas.drawText("${itemIndex + 1}", colX + 4f, currentY + 15f, textPaint)
                colX += colW[0]

                // 2. SKU
                boldPaint.color = Color.rgb(15, 23, 42)
                boldPaint.textSize = 7.8f
                val truncatedSku = truncateText(item.sku, colW[1] - 8f, boldPaint)
                canvas.drawText(truncatedSku, colX + 2f, currentY + 15f, boldPaint)
                colX += colW[1]

                // 3. Item Name
                textPaint.color = Color.rgb(15, 23, 42)
                textPaint.textSize = 8f
                val truncatedName = truncateText(item.name, colW[2] - 8f, textPaint)
                canvas.drawText(truncatedName, colX + 2f, currentY + 15f, textPaint)
                colX += colW[2]

                // 4. Subcategory
                textPaint.color = Color.rgb(71, 85, 105)
                textPaint.textSize = 7.5f
                val truncatedSub = truncateText(item.subcategory, colW[3] - 6f, textPaint)
                canvas.drawText(truncatedSub, colX + 2f, currentY + 15f, textPaint)
                colX += colW[3]

                // 5. Rack
                textPaint.color = Color.rgb(71, 85, 105)
                val truncatedRack = truncateText(item.locationRack, colW[4] - 6f, textPaint)
                canvas.drawText(truncatedRack, colX + 2f, currentY + 15f, textPaint)
                colX += colW[4]

                // 6. Current Qty
                val qtyColor = when {
                    item.quantity <= 0 -> Color.rgb(220, 38, 38)
                    item.quantity <= item.minStockThreshold -> Color.rgb(217, 119, 6)
                    else -> Color.rgb(15, 23, 42)
                }
                boldPaint.color = qtyColor
                boldPaint.textSize = 8.2f
                val qtyStr = "${item.quantity}"
                val qtyWidth = boldPaint.measureText(qtyStr)
                canvas.drawText(qtyStr, colX + colW[5] - qtyWidth - 6f, currentY + 15f, boldPaint)
                colX += colW[5]

                // 7. Unit Price (₹)
                textPaint.color = Color.rgb(15, 23, 42)
                val priceStr = IndianFormatUtils.formatIndianNumber(item.sellingPrice)
                val priceWidth = textPaint.measureText(priceStr)
                canvas.drawText(priceStr, colX + colW[6] - priceWidth - 4f, currentY + 15f, textPaint)
                colX += colW[6]

                // 8. Total Valuation (₹)
                boldPaint.color = Color.rgb(15, 23, 42)
                val totalValStr = IndianFormatUtils.formatIndianNumber(item.totalValuationRetail)
                val totalValWidth = boldPaint.measureText(totalValStr)
                canvas.drawText(totalValStr, colX + colW[7] - totalValWidth - 4f, currentY + 15f, boldPaint)
                colX += colW[7]

                // 9. Status Pill
                val statusText = when (item.stockStatus) {
                    StockStatus.IN_STOCK -> "IN STOCK"
                    StockStatus.LOW_STOCK -> "LOW"
                    StockStatus.OUT_OF_STOCK -> "OUT"
                }
                val (pillBg, pillTextCol) = when (item.stockStatus) {
                    StockStatus.IN_STOCK -> Pair(Color.rgb(220, 252, 231), Color.rgb(22, 101, 52))
                    StockStatus.LOW_STOCK -> Pair(Color.rgb(254, 243, 199), Color.rgb(180, 83, 9))
                    StockStatus.OUT_OF_STOCK -> Pair(Color.rgb(254, 226, 226), Color.rgb(220, 38, 38))
                }

                val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = pillBg
                    style = Paint.Style.FILL
                }
                val pillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = pillTextCol
                    textSize = 6.8f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }

                val pillWidth = colW[8] - 10f
                val pillHeight = 14f
                val pillLeft = colX + 4f
                val pillTop = currentY + 5f

                canvas.drawRoundRect(RectF(pillLeft, pillTop, pillLeft + pillWidth, pillTop + pillHeight), 4f, 4f, pillPaint)
                val stw = pillTextPaint.measureText(statusText)
                canvas.drawText(statusText, pillLeft + (pillWidth - stw) / 2f, pillTop + 10.5f, pillTextPaint)

                currentY += rowHeight
                itemIndex++
                rowsRenderedThisPage++
            }

            // If this is the last page, draw grand totals row
            if (pageNumber == totalPages) {
                currentY += 4f
                bgPaint.color = Color.rgb(241, 245, 249) // Slate 100
                canvas.drawRoundRect(RectF(MARGIN_X, currentY, PAGE_WIDTH - MARGIN_X, currentY + 22f), 4f, 4f, bgPaint)

                boldPaint.color = Color.rgb(15, 23, 42)
                boldPaint.textSize = 8.5f
                canvas.drawText("GRAND TOTALS ($categoryName)", MARGIN_X + 8f, currentY + 14f, boldPaint)

                // Total Units
                val totalQtyStr = "$totalUnits Units"
                val tqw = boldPaint.measureText(totalQtyStr)
                canvas.drawText(totalQtyStr, MARGIN_X + colW[0] + colW[1] + colW[2] + colW[3] + colW[4] + colW[5] - tqw - 4f, currentY + 14f, boldPaint)

                // Total Valuation
                val totalValStr = IndianFormatUtils.formatInr(totalRetailValuation)
                val tvw = boldPaint.measureText(totalValStr)
                boldPaint.color = Color.rgb(180, 83, 9) // Amber 700
                canvas.drawText(totalValStr, PAGE_WIDTH - MARGIN_X - tvw - 8f, currentY + 14f, boldPaint)
            }

            // 5. Page Footer
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(148, 163, 184) // Slate 400
                textSize = 7.5f
            }

            val footerY = PAGE_HEIGHT - 16f
            canvas.drawText("AutoStock Pro™ Enterprise Edition • Confidential Inventory Document", MARGIN_X, footerY, footerPaint)

            val pageStr = "Page $pageNumber of $totalPages"
            val pageStrWidth = footerPaint.measureText(pageStr)
            canvas.drawText(pageStr, PAGE_WIDTH - MARGIN_X - pageStrWidth, footerY, footerPaint)

            document.finishPage(page)
        }

        // Write output to file
        FileOutputStream(pdfFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return pdfFile
    }

    /**
     * Generates a beautifully formatted Export Requirement PDF report specifically for stock replenishment.
     * Calculates required stock and required stock valuation to keep the inventory up to the threshold.
     */
    fun generateRequirementPdfReport(
        context: Context,
        categoryName: String,
        items: List<InventoryItem>,
        reportTitle: String = "Export Requirement Report"
    ): File {
        val reportsDir = File(context.cacheDir, "reports").apply {
            if (!exists()) mkdirs()
        }

        val sanitizedCategory = categoryName.replace("\\s+".toRegex(), "_")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
        val pdfFile = File(reportsDir, "Requirement_${sanitizedCategory}_$timestamp.pdf")

        val document = PdfDocument()

        // Setup Paints
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42) // Slate 900
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(254, 215, 170) // Saffron 200
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
            color = Color.rgb(226, 232, 240) // Slate 200
        }

        // Summary Calculations based on Required Stock to Meet Threshold
        val totalSkus = items.size
        val totalRequiredUnits = items.sumOf { it.requiredStock }
        val totalRequiredValuation = items.sumOf { it.requiredStockValuationRetail }
        val lowStockCount = items.count { it.quantity <= it.minStockThreshold }
        val criticalShortageCount = items.count { it.quantity <= 0 }
        val formattedDate = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.ENGLISH).format(Date())

        val headerHeight = 110f
        val kpiSectionHeight = 52f
        val tableHeaderHeight = 22f
        val rowHeight = 24f

        val usableWidth = PAGE_WIDTH - (MARGIN_X * 2)

        // Columns: [S.No (18), SKU (52), Description (114), Category (48), Curr Qty (42), Threshold (42), Req Qty (42), Price (50), Req Value (75), Priority (56)]
        val colW = floatArrayOf(18f, 52f, 114f, 48f, 42f, 42f, 42f, 50f, 75f, 56f)
        val colTitles = arrayOf("#", "SKU / Code", "Item Description", "Category", "Curr Qty", "Min Stk", "Req. Stk", "Price", "Req Value", "Priority")

        val firstPageContentTop = MARGIN_TOP + headerHeight + kpiSectionHeight + 10f
        val subsequentPageContentTop = MARGIN_TOP + 40f

        val firstPageMaxRows = ((PAGE_HEIGHT - MARGIN_BOTTOM - firstPageContentTop - tableHeaderHeight) / rowHeight).toInt().coerceAtLeast(1)
        val subsequentPageMaxRows = ((PAGE_HEIGHT - MARGIN_BOTTOM - subsequentPageContentTop - tableHeaderHeight) / rowHeight).toInt().coerceAtLeast(1)

        val totalPages = if (items.isEmpty()) {
            1
        } else if (items.size <= firstPageMaxRows) {
            1
        } else {
            1 + ((items.size - firstPageMaxRows + subsequentPageMaxRows - 1) / subsequentPageMaxRows)
        }

        var itemIndex = 0

        for (pageNumber in 1..totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            var currentY = MARGIN_TOP

            if (pageNumber == 1) {
                // Header Banner (Slate dark theme)
                bgPaint.color = Color.rgb(15, 23, 42)
                canvas.drawRoundRect(RectF(MARGIN_X, currentY, PAGE_WIDTH - MARGIN_X, currentY + 70f), 8f, 8f, bgPaint)

                // Red Accent Strip for Requirement Report
                bgPaint.color = Color.rgb(220, 38, 38) // Red 600
                canvas.drawRoundRect(RectF(MARGIN_X, currentY, MARGIN_X + 6f, currentY + 70f), 4f, 4f, bgPaint)

                canvas.drawText("AutoStock Pro™ | Export Requirement Report", MARGIN_X + 16f, currentY + 22f, titlePaint)
                canvas.drawText("INVENTORY REPLENISHMENT & DEMAND PLANNING", MARGIN_X + 16f, currentY + 38f, subtitlePaint)

                val datePaint = Paint(subtitlePaint).apply { color = Color.rgb(203, 213, 225) }
                canvas.drawText("Generated: $formattedDate • Category: $categoryName • Threshold Target Valuation", MARGIN_X + 16f, currentY + 54f, datePaint)

                currentY += 78f

                // KPI Summary Cards
                val kpiCardWidth = (usableWidth - (3 * 8f)) / 4f
                val kpiCardHeight = 44f

                // Card 1: Shortage Items
                drawKpiCard(
                    canvas, RectF(MARGIN_X, currentY, MARGIN_X + kpiCardWidth, currentY + kpiCardHeight),
                    "SHORTAGE ITEMS", "$lowStockCount SKUs", Color.rgb(254, 226, 226), Color.rgb(220, 38, 38)
                )

                // Card 2: Required Stock to Meet Threshold
                val c2Left = MARGIN_X + kpiCardWidth + 8f
                drawKpiCard(
                    canvas, RectF(c2Left, currentY, c2Left + kpiCardWidth, currentY + kpiCardHeight),
                    "REQUIRED STOCK", "+$totalRequiredUnits Units", Color.rgb(254, 242, 242), Color.rgb(185, 28, 28)
                )

                // Card 3: Required Stock Value
                val c3Left = c2Left + kpiCardWidth + 8f
                drawKpiCard(
                    canvas, RectF(c3Left, currentY, c3Left + kpiCardWidth, currentY + kpiCardHeight),
                    "REPLENISHMENT VALUE", "₹" + IndianFormatUtils.formatInr(totalRequiredValuation, compact = true), Color.rgb(240, 253, 244), Color.rgb(21, 128, 61)
                )

                // Card 4: Critical Level
                val c4Left = c3Left + kpiCardWidth + 8f
                drawKpiCard(
                    canvas, RectF(c4Left, currentY, c4Left + kpiCardWidth, currentY + kpiCardHeight),
                    "CRITICAL STOCK (0)", "$criticalShortageCount Items", Color.rgb(254, 226, 226), Color.rgb(220, 38, 38)
                )

                currentY += kpiCardHeight + 14f
            } else {
                bgPaint.color = Color.rgb(15, 23, 42)
                canvas.drawRect(RectF(MARGIN_X, currentY, PAGE_WIDTH - MARGIN_X, currentY + 24f), bgPaint)

                val miniHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 9f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText("AutoStock Pro™ | Export Requirement Report (Continued)", MARGIN_X + 8f, currentY + 16f, miniHeaderPaint)

                val miniDatePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(203, 213, 225)
                    textSize = 8f
                }
                val dateWidth = miniDatePaint.measureText(formattedDate)
                canvas.drawText(formattedDate, PAGE_WIDTH - MARGIN_X - dateWidth - 8f, currentY + 16f, miniDatePaint)

                currentY += 30f
            }

            // Table Column Headers
            bgPaint.color = Color.rgb(30, 41, 59)
            canvas.drawRoundRect(RectF(MARGIN_X, currentY, PAGE_WIDTH - MARGIN_X, currentY + tableHeaderHeight), 4f, 4f, bgPaint)

            var colX = MARGIN_X
            val thPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 7.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            colTitles.forEachIndexed { idx, title ->
                val w = colW[idx]
                val alignRight = idx in 4..8
                val textX = if (alignRight) {
                    val tw = thPaint.measureText(title)
                    colX + w - tw - 4f
                } else {
                    colX + 4f
                }
                canvas.drawText(title, textX, currentY + 14f, thPaint)
                colX += w
            }

            currentY += tableHeaderHeight + 2f

            val rowsLimit = if (pageNumber == 1) firstPageMaxRows else subsequentPageMaxRows
            var rowsRenderedThisPage = 0

            while (itemIndex < items.size && rowsRenderedThisPage < rowsLimit) {
                val item = items[itemIndex]
                val isEven = rowsRenderedThisPage % 2 == 0

                bgPaint.color = if (isEven) Color.rgb(248, 250, 252) else Color.WHITE
                canvas.drawRect(RectF(MARGIN_X, currentY, PAGE_WIDTH - MARGIN_X, currentY + rowHeight), bgPaint)

                strokePaint.color = Color.rgb(241, 245, 249)
                canvas.drawLine(MARGIN_X, currentY + rowHeight, PAGE_WIDTH - MARGIN_X, currentY + rowHeight, strokePaint)

                colX = MARGIN_X

                // 1. S.No
                textPaint.color = Color.rgb(100, 116, 139)
                canvas.drawText("${itemIndex + 1}", colX + 4f, currentY + 15f, textPaint)
                colX += colW[0]

                // 2. SKU / Code
                boldPaint.color = Color.rgb(15, 23, 42)
                val truncSku = truncateText(item.sku, colW[1] - 6f, boldPaint)
                canvas.drawText(truncSku, colX + 2f, currentY + 15f, boldPaint)
                colX += colW[1]

                // 3. Item Description
                textPaint.color = Color.rgb(15, 23, 42)
                val truncName = truncateText(item.name, colW[2] - 6f, textPaint)
                canvas.drawText(truncName, colX + 2f, currentY + 15f, textPaint)
                colX += colW[2]

                // 4. Category
                textPaint.color = Color.rgb(71, 85, 105)
                val truncCat = truncateText(item.category, colW[3] - 4f, textPaint)
                canvas.drawText(truncCat, colX + 2f, currentY + 15f, textPaint)
                colX += colW[3]

                // 5. Current Qty
                val qtyStr = "${item.quantity}"
                val qw = textPaint.measureText(qtyStr)
                textPaint.color = if (item.quantity <= item.minStockThreshold) Color.rgb(220, 38, 38) else Color.rgb(15, 23, 42)
                canvas.drawText(qtyStr, colX + colW[4] - qw - 4f, currentY + 15f, textPaint)
                colX += colW[4]

                // 6. Min Threshold
                val minStr = "${item.minStockThreshold}"
                val mw = textPaint.measureText(minStr)
                textPaint.color = Color.rgb(71, 85, 105)
                canvas.drawText(minStr, colX + colW[5] - mw - 4f, currentY + 15f, textPaint)
                colX += colW[5]

                // 7. Required Stock to meet threshold
                val reqStr = "${item.requiredStock}"
                val rw = boldPaint.measureText(reqStr)
                boldPaint.color = if (item.requiredStock > 0) Color.rgb(185, 28, 28) else Color.rgb(15, 23, 42)
                canvas.drawText(reqStr, colX + colW[6] - rw - 4f, currentY + 15f, boldPaint)
                colX += colW[6]

                // 8. Unit Price
                val priceStr = IndianFormatUtils.formatInr(item.sellingPrice, compact = true)
                val pw = textPaint.measureText(priceStr)
                textPaint.color = Color.rgb(15, 23, 42)
                canvas.drawText(priceStr, colX + colW[7] - pw - 4f, currentY + 15f, textPaint)
                colX += colW[7]

                // 9. Required Stock Valuation
                val valueStr = IndianFormatUtils.formatInr(item.requiredStockValuationRetail, compact = true)
                val vw = boldPaint.measureText(valueStr)
                boldPaint.color = if (item.requiredStock > 0) Color.rgb(21, 128, 61) else Color.rgb(148, 163, 184)
                canvas.drawText(valueStr, colX + colW[8] - vw - 4f, currentY + 15f, boldPaint)
                colX += colW[8]

                // 10. Priority Pill
                val priorityText = if (item.quantity <= 0) {
                    "CRITICAL"
                } else if (item.quantity <= item.minStockThreshold) {
                    "WARNING"
                } else {
                    "HEALTHY"
                }

                val (pillBg, pillTextCol) = when (priorityText) {
                    "CRITICAL" -> Pair(Color.rgb(254, 226, 226), Color.rgb(220, 38, 38))
                    "WARNING" -> Pair(Color.rgb(254, 226, 226), Color.rgb(220, 38, 38))
                    else -> Pair(Color.rgb(220, 252, 231), Color.rgb(21, 128, 61))
                }

                val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pillBg; style = Paint.Style.FILL }
                val pillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pillTextCol; textSize = 6f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
                val pillW = colW[9] - 6f
                canvas.drawRoundRect(RectF(colX + 2f, currentY + 5f, colX + 2f + pillW, currentY + 19f), 4f, 4f, pillPaint)
                val ptw = pillTextPaint.measureText(priorityText)
                canvas.drawText(priorityText, colX + 2f + (pillW - ptw) / 2f, currentY + 14.5f, pillTextPaint)
                colX += colW[9]

                currentY += rowHeight
                itemIndex++
                rowsRenderedThisPage++
            }

            if (pageNumber == totalPages) {
                currentY += 4f
                bgPaint.color = Color.rgb(241, 245, 249)
                canvas.drawRoundRect(RectF(MARGIN_X, currentY, PAGE_WIDTH - MARGIN_X, currentY + 22f), 4f, 4f, bgPaint)

                boldPaint.color = Color.rgb(15, 23, 42)
                boldPaint.textSize = 8f
                canvas.drawText("REPLENISHMENT TOTALS", MARGIN_X + 8f, currentY + 14f, boldPaint)

                val summaryStr = "Required Qty: +$totalRequiredUnits Units | Investment Needed: ₹" + IndianFormatUtils.formatInr(totalRequiredValuation, compact = false)
                val sw = boldPaint.measureText(summaryStr)
                boldPaint.color = Color.rgb(15, 23, 42)
                canvas.drawText(summaryStr, PAGE_WIDTH - MARGIN_X - sw - 8f, currentY + 14f, boldPaint)
            }

            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(148, 163, 184)
                textSize = 7.5f
            }

            val footerY = PAGE_HEIGHT - 16f
            canvas.drawText("AutoStock Pro™ Enterprise Edition • Export Requirement Report Document", MARGIN_X, footerY, footerPaint)

            val pageStr = "Page $pageNumber of $totalPages"
            val pageStrWidth = footerPaint.measureText(pageStr)
            canvas.drawText(pageStr, PAGE_WIDTH - MARGIN_X - pageStrWidth, footerY, footerPaint)

            document.finishPage(page)
        }

        // Write output to file
        FileOutputStream(pdfFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return pdfFile
    }

    /**
     * Generates a distinct and beautifully formatted Stock Movement Ledger PDF report.
     * Returns the generated File object.
     */
    fun generateLedgerPdfReport(
        context: Context,
        transactions: List<StockTransaction>,
        scopeName: String = "Complete Ledger",
        reportTitle: String = "Stock Movement Ledger Audit"
    ): File {
        val reportsDir = File(context.cacheDir, "reports").apply {
            if (!exists()) mkdirs()
        }

        val sanitizedScope = scopeName.replace("\\s+".toRegex(), "_")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
        val pdfFile = File(reportsDir, "Ledger_${sanitizedScope}_$timestamp.pdf")

        val document = PdfDocument()

        // Setup Paints
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(254, 215, 170)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
            color = Color.rgb(226, 232, 240)
        }

        // Summary Calculations for Ledger
        val totalTxns = transactions.size
        val totalInflow = transactions.filter { it.quantityDelta > 0 }.sumOf { it.quantityDelta }
        val totalOutflow = transactions.filter { it.quantityDelta < 0 }.sumOf { -it.quantityDelta }
        val netMovement = totalInflow - totalOutflow
        val formattedDate = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.ENGLISH).format(Date())

        val headerHeight = 110f
        val kpiSectionHeight = 52f
        val tableHeaderHeight = 22f
        val rowHeight = 24f

        val usableWidth = PAGE_WIDTH - (MARGIN_X * 2)

        // Columns: [S.No (18), Date & Time (66), SKU (52), Item Name (118), Cat (48), Type (48), Delta (38), Bal (36), Notes (115)]
        val colW = floatArrayOf(18f, 66f, 52f, 118f, 48f, 48f, 38f, 36f, 115f)
        val colTitles = arrayOf("#", "Date & Time", "SKU", "Item Description", "Category", "Action", "Qty", "Bal", "Reason / Reference")

        val firstPageContentTop = MARGIN_TOP + headerHeight + kpiSectionHeight + 10f
        val subsequentPageContentTop = MARGIN_TOP + 40f

        val firstPageMaxRows = ((PAGE_HEIGHT - MARGIN_BOTTOM - firstPageContentTop - tableHeaderHeight) / rowHeight).toInt().coerceAtLeast(1)
        val subsequentPageMaxRows = ((PAGE_HEIGHT - MARGIN_BOTTOM - subsequentPageContentTop - tableHeaderHeight) / rowHeight).toInt().coerceAtLeast(1)

        val totalPages = if (transactions.isEmpty()) {
            1
        } else if (transactions.size <= firstPageMaxRows) {
            1
        } else {
            1 + ((transactions.size - firstPageMaxRows + subsequentPageMaxRows - 1) / subsequentPageMaxRows)
        }

        var txnIndex = 0

        for (pageNumber in 1..totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            var currentY = MARGIN_TOP

            if (pageNumber == 1) {
                // Header Banner
                bgPaint.color = Color.rgb(15, 23, 42) // Slate 900
                canvas.drawRoundRect(RectF(MARGIN_X, currentY, PAGE_WIDTH - MARGIN_X, currentY + 70f), 8f, 8f, bgPaint)

                // Emerald Accent Strip for Ledger
                bgPaint.color = Color.rgb(16, 185, 129) // Emerald 500
                canvas.drawRoundRect(RectF(MARGIN_X, currentY, MARGIN_X + 6f, currentY + 70f), 4f, 4f, bgPaint)

                canvas.drawText("AutoStock Pro™ | Stock Movement & Audit Register", MARGIN_X + 16f, currentY + 22f, titlePaint)
                canvas.drawText("TRANSACTION AUDIT LEDGER: ${scopeName.uppercase()}", MARGIN_X + 16f, currentY + 38f, subtitlePaint)

                val datePaint = Paint(subtitlePaint).apply { color = Color.rgb(203, 213, 225) }
                canvas.drawText("Generated: $formattedDate • Scope: $scopeName • Detailed Movement Trail", MARGIN_X + 16f, currentY + 54f, datePaint)

                currentY += 78f

                // KPI Summary Cards
                val kpiCardWidth = (usableWidth - (3 * 8f)) / 4f
                val kpiCardHeight = 44f

                // Card 1: Total Transactions
                drawKpiCard(
                    canvas, RectF(MARGIN_X, currentY, MARGIN_X + kpiCardWidth, currentY + kpiCardHeight),
                    "TOTAL LOGS", "$totalTxns Entries", Color.rgb(241, 245, 249), Color.rgb(30, 41, 59)
                )

                // Card 2: Inflow Units
                val c2Left = MARGIN_X + kpiCardWidth + 8f
                drawKpiCard(
                    canvas, RectF(c2Left, currentY, c2Left + kpiCardWidth, currentY + kpiCardHeight),
                    "TOTAL INFLOW (+)", "+$totalInflow Units", Color.rgb(220, 252, 231), Color.rgb(22, 101, 52)
                )

                // Card 3: Outflow Units
                val c3Left = c2Left + kpiCardWidth + 8f
                drawKpiCard(
                    canvas, RectF(c3Left, currentY, c3Left + kpiCardWidth, currentY + kpiCardHeight),
                    "TOTAL OUTFLOW (-)", "-$totalOutflow Units", Color.rgb(254, 226, 226), Color.rgb(220, 38, 38)
                )

                // Card 4: Net Movement
                val c4Left = c3Left + kpiCardWidth + 8f
                val netColor = if (netMovement >= 0) Color.rgb(22, 101, 52) else Color.rgb(220, 38, 38)
                val netBg = if (netMovement >= 0) Color.rgb(220, 252, 231) else Color.rgb(254, 226, 226)
                drawKpiCard(
                    canvas, RectF(c4Left, currentY, c4Left + kpiCardWidth, currentY + kpiCardHeight),
                    "NET MOVEMENT", "${if (netMovement > 0) "+" else ""}$netMovement Units", netBg, netColor
                )

                currentY += kpiCardHeight + 14f
            } else {
                bgPaint.color = Color.rgb(15, 23, 42)
                canvas.drawRect(RectF(MARGIN_X, currentY, PAGE_WIDTH - MARGIN_X, currentY + 24f), bgPaint)

                val miniHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 9f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText("AutoStock Pro™ | $scopeName Movement Ledger (Continued)", MARGIN_X + 8f, currentY + 16f, miniHeaderPaint)

                val miniDatePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(203, 213, 225)
                    textSize = 8f
                }
                val dateWidth = miniDatePaint.measureText(formattedDate)
                canvas.drawText(formattedDate, PAGE_WIDTH - MARGIN_X - dateWidth - 8f, currentY + 16f, miniDatePaint)

                currentY += 30f
            }

            // Table Column Headers
            bgPaint.color = Color.rgb(30, 41, 59)
            canvas.drawRoundRect(RectF(MARGIN_X, currentY, PAGE_WIDTH - MARGIN_X, currentY + tableHeaderHeight), 4f, 4f, bgPaint)

            var colX = MARGIN_X
            val thPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 7.8f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            colTitles.forEachIndexed { idx, title ->
                val w = colW[idx]
                val alignRight = idx in 6..7
                val textX = if (alignRight) {
                    val tw = thPaint.measureText(title)
                    colX + w - tw - 4f
                } else {
                    colX + 4f
                }
                canvas.drawText(title, textX, currentY + 14f, thPaint)
                colX += w
            }

            currentY += tableHeaderHeight + 2f

            val rowsLimit = if (pageNumber == 1) firstPageMaxRows else subsequentPageMaxRows
            var rowsRenderedThisPage = 0

            val dateFmt = SimpleDateFormat("dd/MM/yy HH:mm", Locale.ENGLISH)

            while (txnIndex < transactions.size && rowsRenderedThisPage < rowsLimit) {
                val txn = transactions[txnIndex]
                val isEven = rowsRenderedThisPage % 2 == 0
                val isIn = txn.quantityDelta > 0

                bgPaint.color = if (isIn) {
                    if (isEven) Color.rgb(240, 253, 244) else Color.WHITE
                } else {
                    if (isEven) Color.rgb(254, 242, 242) else Color.WHITE
                }

                canvas.drawRect(RectF(MARGIN_X, currentY, PAGE_WIDTH - MARGIN_X, currentY + rowHeight), bgPaint)
                strokePaint.color = Color.rgb(226, 232, 240)
                canvas.drawLine(MARGIN_X, currentY + rowHeight, PAGE_WIDTH - MARGIN_X, currentY + rowHeight, strokePaint)

                colX = MARGIN_X

                // 1. S.No
                textPaint.color = Color.rgb(100, 116, 139)
                canvas.drawText("${txnIndex + 1}", colX + 4f, currentY + 15f, textPaint)
                colX += colW[0]

                // 2. Date
                textPaint.color = Color.rgb(71, 85, 105)
                textPaint.textSize = 7.5f
                val dateStr = dateFmt.format(Date(txn.timestamp))
                canvas.drawText(dateStr, colX + 2f, currentY + 15f, textPaint)
                colX += colW[1]

                // 3. SKU
                boldPaint.color = Color.rgb(15, 23, 42)
                boldPaint.textSize = 7.5f
                val truncSku = truncateText(txn.sku, colW[2] - 6f, boldPaint)
                canvas.drawText(truncSku, colX + 2f, currentY + 15f, boldPaint)
                colX += colW[2]

                // 4. Item Name
                textPaint.color = Color.rgb(15, 23, 42)
                textPaint.textSize = 7.8f
                val truncName = truncateText(txn.itemName, colW[3] - 6f, textPaint)
                canvas.drawText(truncName, colX + 2f, currentY + 15f, textPaint)
                colX += colW[3]

                // 5. Category
                textPaint.color = Color.rgb(71, 85, 105)
                textPaint.textSize = 7.2f
                val truncCat = truncateText(txn.category, colW[4] - 4f, textPaint)
                canvas.drawText(truncCat, colX + 2f, currentY + 15f, textPaint)
                colX += colW[4]

                // 6. Action Type Pill
                val actionText = if (isIn) "STOCK IN" else "STOCK OUT"
                val (pillBg, pillTextCol) = if (isIn) Pair(Color.rgb(220, 252, 231), Color.rgb(22, 101, 52)) else Pair(Color.rgb(254, 226, 226), Color.rgb(220, 38, 38))
                val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pillBg; style = Paint.Style.FILL }
                val pillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pillTextCol; textSize = 6.2f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
                val pillW = colW[5] - 6f
                canvas.drawRoundRect(RectF(colX + 2f, currentY + 5f, colX + 2f + pillW, currentY + 19f), 4f, 4f, pillPaint)
                val atw = pillTextPaint.measureText(actionText)
                canvas.drawText(actionText, colX + 2f + (pillW - atw) / 2f, currentY + 14.5f, pillTextPaint)
                colX += colW[5]

                // 7. Qty Delta
                boldPaint.color = if (isIn) Color.rgb(22, 101, 52) else Color.rgb(220, 38, 38)
                boldPaint.textSize = 8.2f
                val deltaStr = if (isIn) "+${txn.quantityDelta}" else "${txn.quantityDelta}"
                val dw = boldPaint.measureText(deltaStr)
                canvas.drawText(deltaStr, colX + colW[6] - dw - 4f, currentY + 15f, boldPaint)
                colX += colW[6]

                // 8. Balance
                textPaint.color = Color.rgb(15, 23, 42)
                textPaint.textSize = 8f
                val balStr = "${txn.newQuantity}"
                val bw = textPaint.measureText(balStr)
                canvas.drawText(balStr, colX + colW[7] - bw - 4f, currentY + 15f, textPaint)
                colX += colW[7]

                // 9. Reason / Note
                textPaint.color = Color.rgb(71, 85, 105)
                textPaint.textSize = 7.2f
                val noteStr = txn.reasonOrNote.ifBlank { if (isIn) "Stock Received" else "Stock Dispatched" }
                val truncNote = truncateText(noteStr, colW[8] - 6f, textPaint)
                canvas.drawText(truncNote, colX + 2f, currentY + 15f, textPaint)
                colX += colW[8]

                currentY += rowHeight
                txnIndex++
                rowsRenderedThisPage++
            }

            if (pageNumber == totalPages) {
                currentY += 4f
                bgPaint.color = Color.rgb(241, 245, 249)
                canvas.drawRoundRect(RectF(MARGIN_X, currentY, PAGE_WIDTH - MARGIN_X, currentY + 22f), 4f, 4f, bgPaint)

                boldPaint.color = Color.rgb(15, 23, 42)
                boldPaint.textSize = 8.5f
                canvas.drawText("LEDGER AUDIT TOTALS", MARGIN_X + 8f, currentY + 14f, boldPaint)

                val netStr = "Net: ${if (netMovement > 0) "+" else ""}$netMovement Units (In: +$totalInflow, Out: -$totalOutflow)"
                val ntw = boldPaint.measureText(netStr)
                boldPaint.color = if (netMovement >= 0) Color.rgb(22, 101, 52) else Color.rgb(220, 38, 38)
                canvas.drawText(netStr, PAGE_WIDTH - MARGIN_X - ntw - 8f, currentY + 14f, boldPaint)
            }

            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(148, 163, 184)
                textSize = 7.5f
            }

            val footerY = PAGE_HEIGHT - 16f
            canvas.drawText("AutoStock Pro™ Enterprise Edition • Stock Movement Ledger Document", MARGIN_X, footerY, footerPaint)

            val pageStr = "Page $pageNumber of $totalPages"
            val pageStrWidth = footerPaint.measureText(pageStr)
            canvas.drawText(pageStr, PAGE_WIDTH - MARGIN_X - pageStrWidth, footerY, footerPaint)

            document.finishPage(page)
        }

        FileOutputStream(pdfFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return pdfFile
    }

    private fun drawKpiCard(
        canvas: Canvas,
        rect: RectF,
        title: String,
        value: String,
        bgColor: Int,
        textColor: Int
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, 6f, 6f, bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240)
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
        }
        canvas.drawRoundRect(rect, 6f, 6f, borderPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139)
            textSize = 6.8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(title, rect.left + 8f, rect.top + 14f, titlePaint)

        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(value, rect.left + 8f, rect.top + 32f, valuePaint)
    }

    private fun truncateText(text: String, maxWidth: Float, paint: Paint): String {
        if (paint.measureText(text) <= maxWidth) return text
        var truncated = text
        while (truncated.isNotEmpty() && paint.measureText("$truncated…") > maxWidth) {
            truncated = truncated.dropLast(1)
        }
        return "$truncated…"
    }

    /**
     * Opens Android System Sharesheet to share the generated PDF via WhatsApp, Email, Drive, etc.
     */
    fun sharePdfFile(context: Context, pdfFile: File, categoryName: String) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "AutoStock Inventory Report - $categoryName")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Please find attached the latest AutoStock inventory report for $categoryName.\nGenerated on ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(Date())}."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share $categoryName Inventory PDF")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to share PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Opens Android PrintManager to directly print the generated PDF document to any connected WiFi/Bluetooth printer or Save to PDF spooler.
     */
    fun printPdfFile(context: Context, pdfFile: File, categoryName: String) {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager != null) {
                val printAdapter: PrintDocumentAdapter = object : PrintDocumentAdapter() {
                    override fun onLayout(
                        oldAttributes: PrintAttributes?,
                        newAttributes: PrintAttributes?,
                        cancellationSignal: android.os.CancellationSignal?,
                        callback: LayoutResultCallback?,
                        extras: android.os.Bundle?
                    ) {
                        if (cancellationSignal?.isCanceled == true) {
                            callback?.onLayoutCancelled()
                            return
                        }
                        val info = android.print.PrintDocumentInfo.Builder("AutoStock_${categoryName}_Report.pdf")
                            .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .build()
                        callback?.onLayoutFinished(info, newAttributes != oldAttributes)
                    }

                    override fun onWrite(
                        pages: Array<out android.print.PageRange>?,
                        destination: android.os.ParcelFileDescriptor?,
                        cancellationSignal: android.os.CancellationSignal?,
                        callback: WriteResultCallback?
                    ) {
                        if (cancellationSignal?.isCanceled == true) {
                            callback?.onWriteCancelled()
                            return
                        }
                        try {
                            pdfFile.inputStream().use { input ->
                                java.io.FileOutputStream(destination?.fileDescriptor).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                        } catch (e: Exception) {
                            callback?.onWriteFailed(e.message)
                        }
                    }
                }

                printManager.print("AutoStock_$categoryName", printAdapter, PrintAttributes.Builder().build())
            } else {
                // Fallback to viewing the PDF
                viewPdfFile(context, pdfFile)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Printing error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Opens the PDF file using an external PDF viewer app.
     */
    fun viewPdfFile(context: Context, pdfFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                pdfFile
            )

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(viewIntent, "Open PDF Report"))
        } catch (e: Exception) {
            Toast.makeText(context, "No PDF viewer app found", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Generates a beautifully formatted PDF invoice/receipt for retail billing.
     */
    fun generateInvoicePdfReport(
        context: Context,
        invoiceId: String,
        customerName: String,
        customerPhone: String,
        items: List<InvoicePdfItem>,
        subtotal: Double,
        gst: Double,
        totalAmount: Double,
        paymentMethod: String,
        timestamp: Long
    ): File {
        val reportsDir = File(context.cacheDir, "reports").apply {
            if (!exists()) mkdirs()
        }

        val pdfFile = File(reportsDir, "Invoice_${invoiceId}.pdf")
        val document = PdfDocument()

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42) // Slate 900
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(209, 250, 229) // Emerald 100
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
            color = Color.rgb(226, 232, 240) // Slate 200
        }

        val formattedDate = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.ENGLISH).format(Date(timestamp))

        val metaHeight = 65f
        val tableHeaderHeight = 22f
        val rowHeight = 24f
        val usableWidth = PAGE_WIDTH - (MARGIN_X * 2)

        // Columns: [S.No (30), Item Name (230), SKU/Code (100), Unit Price (60), Qty (40), Total (79)]
        val colW = floatArrayOf(30f, 230f, 100f, 60f, 40f, 79f)
        val colTitles = arrayOf("#", "Item Description", "SKU / Barcode", "Unit Price", "Qty", "Total")

        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        var currentY = MARGIN_TOP

        // Header Banner (Slate dark theme with Emerald accent)
        bgPaint.color = Color.rgb(15, 23, 42) // Slate 900
        canvas.drawRoundRect(RectF(MARGIN_X, currentY, PAGE_WIDTH - MARGIN_X, currentY + 60f), 8f, 8f, bgPaint)

        // Emerald Accent Strip
        bgPaint.color = Color.rgb(16, 185, 129) // Emerald 500
        canvas.drawRoundRect(RectF(MARGIN_X, currentY, MARGIN_X + 6f, currentY + 60f), 4f, 4f, bgPaint)

        canvas.drawText("AutoStock | Invoice Receipt", MARGIN_X + 16f, currentY + 22f, titlePaint)
        canvas.drawText("RETAIL BILLING & INVENTORY TRANSACTION LEDGER", MARGIN_X + 16f, currentY + 38f, subtitlePaint)

        val datePaint = Paint(subtitlePaint).apply { color = Color.rgb(203, 213, 225) }
        canvas.drawText("Billing Date: $formattedDate • Payment System: $paymentMethod", MARGIN_X + 16f, currentY + 50f, datePaint)

        currentY += 68f

        // Metadata section card (Invoice No, Customer Info)
        bgPaint.color = Color.rgb(248, 250, 252) // Slate 50
        canvas.drawRoundRect(RectF(MARGIN_X, currentY, PAGE_WIDTH - MARGIN_X, currentY + metaHeight), 6f, 6f, bgPaint)
        canvas.drawRoundRect(RectF(MARGIN_X, currentY, PAGE_WIDTH - MARGIN_X, currentY + metaHeight), 6f, 6f, strokePaint)

        val infoTitlePaint = Paint(boldPaint).apply { textSize = 9.5f; color = Color.rgb(30, 41, 59) }
        canvas.drawText("INVOICE DETAILS", MARGIN_X + 12f, currentY + 16f, infoTitlePaint)
        canvas.drawText("CUSTOMER INFORMATION", MARGIN_X + usableWidth / 2f + 12f, currentY + 16f, infoTitlePaint)

        strokePaint.color = Color.rgb(203, 213, 225) // Slate 300
        canvas.drawLine(MARGIN_X + usableWidth / 2f, currentY + 8f, MARGIN_X + usableWidth / 2f, currentY + metaHeight - 8f, strokePaint)
        strokePaint.color = Color.rgb(226, 232, 240) // Slate 200

        // Left Column (Invoice Metadata)
        canvas.drawText("Invoice ID: #$invoiceId", MARGIN_X + 12f, currentY + 32f, textPaint)
        canvas.drawText("Payment Method: $paymentMethod", MARGIN_X + 12f, currentY + 44f, textPaint)
        canvas.drawText("Status: Paid & Dispatched", MARGIN_X + 12f, currentY + 56f, Paint(boldPaint).apply { color = Color.rgb(16, 185, 129); textSize = 9f })

        // Right Column (Customer Metadata)
        canvas.drawText("Name: $customerName", MARGIN_X + usableWidth / 2f + 12f, currentY + 32f, textPaint)
        val phoneStr = if (customerPhone.isNotBlank() && customerPhone != "N/A") "+91 $customerPhone" else "N/A"
        canvas.drawText("Contact: $phoneStr", MARGIN_X + usableWidth / 2f + 12f, currentY + 44f, textPaint)
        canvas.drawText("Place of Supply: Registered Outlet", MARGIN_X + usableWidth / 2f + 12f, currentY + 56f, textPaint)

        currentY += metaHeight + 12f

        // Table Header
        bgPaint.color = Color.rgb(241, 245, 249) // Slate 100
        canvas.drawRoundRect(RectF(MARGIN_X, currentY, PAGE_WIDTH - MARGIN_X, currentY + tableHeaderHeight), 4f, 4f, bgPaint)
        canvas.drawRoundRect(RectF(MARGIN_X, currentY, PAGE_WIDTH - MARGIN_X, currentY + tableHeaderHeight), 4f, 4f, strokePaint)

        var xOffset = MARGIN_X
        for (i in colTitles.indices) {
            val align = if (i == 0 || i == 4) Paint.Align.CENTER else if (i == 3 || i == 5) Paint.Align.RIGHT else Paint.Align.LEFT
            val titlePaintToUse = Paint(boldPaint).apply { textAlign = align }
            
            val drawX = when (align) {
                Paint.Align.CENTER -> xOffset + (colW[i] / 2f)
                Paint.Align.RIGHT -> xOffset + colW[i] - 6f
                else -> xOffset + 6f
            }
            canvas.drawText(colTitles[i], drawX, currentY + 14f, titlePaintToUse)
            xOffset += colW[i]
        }

        currentY += tableHeaderHeight

        // Table Rows
        items.forEachIndexed { index, cartItem ->
            if (index % 2 == 1) {
                bgPaint.color = Color.rgb(250, 250, 250) // Subtle alternate row bg
                canvas.drawRect(RectF(MARGIN_X, currentY, PAGE_WIDTH - MARGIN_X, currentY + rowHeight), bgPaint)
            }
            canvas.drawRect(RectF(MARGIN_X, currentY, PAGE_WIDTH - MARGIN_X, currentY + rowHeight), strokePaint)

            var cellX = MARGIN_X
            for (i in 0..5) {
                val align = if (i == 0 || i == 4) Paint.Align.CENTER else if (i == 3 || i == 5) Paint.Align.RIGHT else Paint.Align.LEFT
                val cellPaintToUse = Paint(textPaint).apply { textAlign = align }

                val text = when (i) {
                    0 -> (index + 1).toString()
                    1 -> truncateText(cartItem.name, colW[i] - 12f, textPaint)
                    2 -> cartItem.sku
                    3 -> "₹" + IndianFormatUtils.formatInr(cartItem.unitPrice)
                    4 -> cartItem.quantity.toString()
                    5 -> "₹" + IndianFormatUtils.formatInr(cartItem.total)
                    else -> ""
                }

                val drawX = when (align) {
                    Paint.Align.CENTER -> cellX + (colW[i] / 2f)
                    Paint.Align.RIGHT -> cellX + colW[i] - 6f
                    else -> cellX + 6f
                }

                canvas.drawText(text, drawX, currentY + 15f, cellPaintToUse)
                cellX += colW[i]
            }
            currentY += rowHeight
        }

        currentY += 12f

        // Summary Calculations Box on bottom right
        val summaryWidth = 200f
        val summaryHeight = 65f
        val summaryLeft = PAGE_WIDTH - MARGIN_X - summaryWidth

        bgPaint.color = Color.rgb(248, 250, 252) // Slate 50
        canvas.drawRoundRect(RectF(summaryLeft, currentY, PAGE_WIDTH - MARGIN_X, currentY + summaryHeight), 6f, 6f, bgPaint)
        canvas.drawRoundRect(RectF(summaryLeft, currentY, PAGE_WIDTH - MARGIN_X, currentY + summaryHeight), 6f, 6f, strokePaint)

        // Subtotal row
        canvas.drawText("Subtotal (Excl. GST):", summaryLeft + 12f, currentY + 16f, textPaint)
        val subPaint = Paint(boldPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("₹" + IndianFormatUtils.formatInr(subtotal), PAGE_WIDTH - MARGIN_X - 12f, currentY + 16f, subPaint)

        // GST row
        canvas.drawText("GST Taxes (18% Incl.):", summaryLeft + 12f, currentY + 32f, textPaint)
        canvas.drawText("₹" + IndianFormatUtils.formatInr(gst), PAGE_WIDTH - MARGIN_X - 12f, currentY + 32f, subPaint)

        // Divider
        strokePaint.color = Color.rgb(203, 213, 225) // Slate 300
        canvas.drawLine(summaryLeft + 8f, currentY + 40f, PAGE_WIDTH - MARGIN_X - 8f, currentY + 40f, strokePaint)

        // Grand Total Paid
        canvas.drawText("Grand Net Total Paid:", summaryLeft + 12f, currentY + 54f, Paint(boldPaint).apply { color = Color.rgb(15, 23, 42) })
        canvas.drawText("₹" + IndianFormatUtils.formatInr(totalAmount), PAGE_WIDTH - MARGIN_X - 12f, currentY + 54f, Paint(boldPaint).apply { color = Color.rgb(16, 185, 129); textAlign = Paint.Align.RIGHT })

        currentY += summaryHeight + 24f

        // Professional Footer separator cut-line
        val dashPaint = Paint(strokePaint).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = Color.rgb(148, 163, 184) // Slate 400
        }
        canvas.drawLine(MARGIN_X, currentY, PAGE_WIDTH - MARGIN_X, currentY, dashPaint)

        currentY += 16f
        val footerTextPaint = Paint(textPaint).apply {
            color = Color.rgb(100, 116, 139) // Slate 500
            textSize = 8f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Thank you for shopping with AutoStock! This is a computer-generated tax invoice.", PAGE_WIDTH / 2f, currentY, footerTextPaint)
        canvas.drawText("AutoStock Retail Terminal • Customer Support: support@autostock.co", PAGE_WIDTH / 2f, currentY + 12f, footerTextPaint)

        document.finishPage(page)

        try {
            val fileOutputStream = FileOutputStream(pdfFile)
            document.writeTo(fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            document.close()
        }

        return pdfFile
    }
}

data class InvoicePdfItem(
    val name: String,
    val sku: String,
    val unitPrice: Double,
    val quantity: Int,
    val total: Double
)
