package com.example.ui.billing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.InvoiceReceipt
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Red500
import com.example.ui.util.IndianFormatUtils
import com.example.ui.util.InvoicePdfItem
import com.example.ui.util.PdfReportGenerator
import com.example.ui.util.currentStrings
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingHistoryDialog(
    historyList: List<InvoiceReceipt>,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val strings = currentStrings()
    val keyboardController = LocalSoftwareKeyboardController.current

    var searchQuery by remember { mutableStateOf("") }
    var expandedInvoiceId by remember { mutableStateOf<String?>(null) }

    // Filtered list based on search
    val filteredHistory = remember(searchQuery, historyList) {
        if (searchQuery.isBlank()) {
            historyList
        } else {
            historyList.filter { receipt ->
                receipt.invoiceId.contains(searchQuery, ignoreCase = true) ||
                        receipt.customerName.contains(searchQuery, ignoreCase = true) ||
                        receipt.customerPhone.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onDismissRequest) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Go Back"
                            )
                        }
                    },
                    title = {
                        Text(
                            "Invoices & Billing History",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by Invoice ID, customer name or phone...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("history_search_input"),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // History List / Empty State
                if (filteredHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(64.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (searchQuery.isEmpty()) "No billing history found" else "No matching invoices found",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (searchQuery.isEmpty()) "Create invoices in the Retail Billing section to start populating this history." else "Try adjusting your search terms.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredHistory, key = { it.invoiceId }) { receipt ->
                            val isExpanded = expandedInvoiceId == receipt.invoiceId
                            InvoiceHistoryCard(
                                receipt = receipt,
                                isExpanded = isExpanded,
                                onToggleExpand = {
                                    expandedInvoiceId = if (isExpanded) null else receipt.invoiceId
                                },
                                onPreviewPdf = {
                                    val pdfItems = receipt.items.map { cartItem ->
                                        InvoicePdfItem(
                                            name = cartItem.item.name,
                                            sku = cartItem.item.sku,
                                            unitPrice = cartItem.item.sellingPrice,
                                            quantity = cartItem.quantity,
                                            total = cartItem.item.sellingPrice * cartItem.quantity
                                        )
                                    }
                                    val pdfFile = PdfReportGenerator.generateInvoicePdfReport(
                                        context = context,
                                        invoiceId = receipt.invoiceId,
                                        customerName = receipt.customerName,
                                        customerPhone = receipt.customerPhone,
                                        items = pdfItems,
                                        subtotal = receipt.subtotal,
                                        gst = receipt.gst,
                                        totalAmount = receipt.total,
                                        paymentMethod = receipt.paymentMethod,
                                        timestamp = receipt.timestamp
                                    )
                                    PdfReportGenerator.viewPdfFile(context, pdfFile)
                                },
                                onPrintPdf = {
                                    val pdfItems = receipt.items.map { cartItem ->
                                        InvoicePdfItem(
                                            name = cartItem.item.name,
                                            sku = cartItem.item.sku,
                                            unitPrice = cartItem.item.sellingPrice,
                                            quantity = cartItem.quantity,
                                            total = cartItem.item.sellingPrice * cartItem.quantity
                                        )
                                    }
                                    val pdfFile = PdfReportGenerator.generateInvoicePdfReport(
                                        context = context,
                                        invoiceId = receipt.invoiceId,
                                        customerName = receipt.customerName,
                                        customerPhone = receipt.customerPhone,
                                        items = pdfItems,
                                        subtotal = receipt.subtotal,
                                        gst = receipt.gst,
                                        totalAmount = receipt.total,
                                        paymentMethod = receipt.paymentMethod,
                                        timestamp = receipt.timestamp
                                    )
                                    PdfReportGenerator.printPdfFile(context, pdfFile, receipt.invoiceId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InvoiceHistoryCard(
    receipt: InvoiceReceipt,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onPreviewPdf: () -> Unit,
    onPrintPdf: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val formattedDate = remember(receipt.timestamp) { dateFormatter.format(Date(receipt.timestamp)) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Header: Invoice ID and Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = receipt.invoiceId,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formattedDate,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (receipt.paymentMethod) {
                        "UPI" -> Emerald500.copy(alpha = 0.12f)
                        "Card" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                    },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = receipt.paymentMethod,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (receipt.paymentMethod) {
                            "UPI" -> Emerald500
                            "Card" -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.secondary
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(8.dp))

            // Customer Name & Total Amount Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Customer:",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = receipt.customerName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (receipt.customerPhone != "N/A" && receipt.customerPhone.isNotBlank()) {
                        Text(
                            text = receipt.customerPhone,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total Amount:",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = IndianFormatUtils.formatInr(receipt.total),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Expanded Items Details
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Items Purchased:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    receipt.items.forEach { cartItem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cartItem.item.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "SKU: ${cartItem.item.sku}  •  ${cartItem.quantity} x ${IndianFormatUtils.formatInr(cartItem.item.sellingPrice)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = IndianFormatUtils.formatInr(cartItem.item.sellingPrice * cartItem.quantity),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // PDF Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onPreviewPdf,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Preview Invoice", fontSize = 11.sp)
                        }

                        Button(
                            onClick = onPrintPdf,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Print PDF", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
