package com.example.ui.billing

import android.Manifest
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.InventoryItem
import com.example.data.model.TransactionType
import com.example.data.model.CartItem
import com.example.data.model.InvoiceReceipt
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Red500
import com.example.ui.util.IndianFormatUtils
import com.example.ui.util.currentStrings
import com.example.ui.util.PdfReportGenerator
import com.example.ui.util.InvoicePdfItem
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class, ExperimentalGetImage::class)
@Composable
fun BillingDialog(
    allItems: List<InventoryItem>,
    onCheckout: suspend (Long, Int) -> Result<InventoryItem>,
    onDismissRequest: () -> Unit,
    snackbarHostState: SnackbarHostState,
    onInvoiceFinalized: (InvoiceReceipt) -> Unit = {}
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            BillingScreenContent(
                allItems = allItems,
                onCheckout = onCheckout,
                snackbarHostState = snackbarHostState,
                onInvoiceFinalized = onInvoiceFinalized,
                onDismissRequest = onDismissRequest
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BillingScreenContent(
    allItems: List<InventoryItem>,
    onCheckout: suspend (Long, Int) -> Result<InventoryItem>,
    snackbarHostState: SnackbarHostState,
    onInvoiceFinalized: (InvoiceReceipt) -> Unit = {},
    onDismissRequest: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val strings = currentStrings()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Cart State
    val cartItems = remember { mutableStateListOf<CartItem>() }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("UPI") } // UPI, Cash, Card

    // UI Navigation State inside Billing
    var showSuccessReceipt by remember { mutableStateOf<InvoiceReceipt?>(null) }
    var isScannerActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearchResultsDropdown by remember { mutableStateOf(false) }

    // Scan Throttle & Audio Feedback state
    var lastScannedCode by remember { mutableStateOf("") }
    var scanDebounceActive by remember { mutableStateOf(false) }
    var isAddingScannedItem by remember { mutableStateOf(false) }
    var scannerStatusMessage by remember { mutableStateOf("") }

    // Camera State
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // Dedicated ToneGenerator for POS barcode scanner beep
    val toneGenerator = remember {
        try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (_: Exception) {
            null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                toneGenerator?.release()
            } catch (_: Exception) {}
        }
    }

    // Filter items based on manual search query
    val filteredSearchItems = remember(searchQuery, allItems) {
        if (searchQuery.isBlank()) emptyList()
        else {
            allItems.filter { item ->
                item.name.contains(searchQuery, ignoreCase = true) ||
                        item.sku.contains(searchQuery, ignoreCase = true) ||
                        item.barcode.contains(searchQuery, ignoreCase = true)
            }.take(5)
        }
    }

    // Sound/Haptic notification
    fun triggerSuccessHaptic() {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(80)
            }
        } catch (_: Exception) {}
    }

    // POS Scanner Beep & Haptic for successful barcode recognition
    fun playScanSuccessBeepAndHaptic() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        } catch (_: Exception) {}
        triggerSuccessHaptic()
    }

    // Function to add a product to cart safely
    fun addProductToCart(item: InventoryItem, qty: Int = 1, triggerHaptic: Boolean = true) {
        val existingIndex = cartItems.indexOfFirst { it.item.id == item.id }
        if (existingIndex != -1) {
            val currentQty = cartItems[existingIndex].quantity
            if (currentQty + qty > item.quantity) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Cannot exceed available stock (${item.quantity} units)!")
                }
            } else {
                cartItems[existingIndex] = cartItems[existingIndex].copy(quantity = currentQty + qty)
                if (triggerHaptic) triggerSuccessHaptic()
            }
        } else {
            if (item.quantity <= 0) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("This product is Out of Stock!")
                }
            } else {
                cartItems.add(CartItem(item, qty))
                if (triggerHaptic) triggerSuccessHaptic()
            }
        }
    }

    // Scan handling logic: Beep immediately, display feedback, wait a bit, then increment quantity
    fun handleBarcodeScanned(barcode: String) {
        if (scanDebounceActive || barcode.isBlank()) return
        
        val cleanedBarcode = barcode.trim()
        val matchedItem = allItems.firstOrNull { 
            it.barcode.trim().equals(cleanedBarcode, ignoreCase = true) || 
            it.sku.trim().equals(cleanedBarcode, ignoreCase = true) 
        }
        
        if (matchedItem != null) {
            scanDebounceActive = true
            lastScannedCode = cleanedBarcode
            
            // 1. Trigger beep sound and vibration immediately on scan success
            playScanSuccessBeepAndHaptic()
            
            // 2. Check stock limit before scheduling addition
            val existingIndex = cartItems.indexOfFirst { it.item.id == matchedItem.id }
            val currentQtyInCart = if (existingIndex != -1) cartItems[existingIndex].quantity else 0
            
            if (currentQtyInCart + 1 > matchedItem.quantity) {
                scannerStatusMessage = "Limit reached: ${matchedItem.name} (${matchedItem.quantity} max)"
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Cannot exceed available stock (${matchedItem.quantity} units) for ${matchedItem.name}!")
                    delay(1500)
                    scanDebounceActive = false
                }
            } else {
                // 3. Show scan success status and wait a bit before adding +1 quantity
                isAddingScannedItem = true
                scannerStatusMessage = "✓ Scanned: ${matchedItem.name} • Adding +1..."
                
                coroutineScope.launch {
                    // Intentional short pause so the user hears the beep and sees the scan confirmation
                    delay(500)
                    
                    addProductToCart(matchedItem, qty = 1, triggerHaptic = false)
                    val newIndex = cartItems.indexOfFirst { it.item.id == matchedItem.id }
                    val finalQtyInCart = if (newIndex != -1) cartItems[newIndex].quantity else 1
                    
                    scannerStatusMessage = "✓ Added: ${matchedItem.name} (Qty in cart: $finalQtyInCart)"
                    isAddingScannedItem = false
                    
                    // Cooldown delay before next barcode scan is accepted
                    delay(1200)
                    scanDebounceActive = false
                }
            }
        } else {
            scanDebounceActive = true
            lastScannedCode = cleanedBarcode
            scannerStatusMessage = "Unregistered code: $cleanedBarcode"
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Barcode/SKU '$cleanedBarcode' is not in inventory!")
                delay(1500)
                scanDebounceActive = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header Bar
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Quick Retail Billing",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            navigationIcon = {
                if (onDismissRequest != null) {
                    IconButton(onClick = onDismissRequest) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close Billing")
                    }
                }
            },
                    actions = {
                        if (cartItems.isNotEmpty()) {
                            IconButton(onClick = { cartItems.clear() }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Clear Cart",
                                    tint = Red500
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                // Main Workspace Split or Stack
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(10.dp))

                    // 1. Customer & Search Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Manual Product Search autocomplete box
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = {
                                    searchQuery = it
                                    showSearchResultsDropdown = it.isNotEmpty()
                                },
                                placeholder = { Text("Search by name, SKU or barcode", fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = {
                                            searchQuery = ""
                                            showSearchResultsDropdown = false
                                        }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
                            )

                            // Search dropdown suggestions overlay
                            if (showSearchResultsDropdown && filteredSearchItems.isNotEmpty()) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 64.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column {
                                        filteredSearchItems.forEach { item ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = item.name,
                                                                fontWeight = FontWeight.SemiBold,
                                                                fontSize = 14.sp,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Text(
                                                                text = "SKU: ${item.sku} • Stock: ${item.quantity} ${item.unit}",
                                                                fontSize = 11.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                        Text(
                                                            text = IndianFormatUtils.formatInr(item.sellingPrice),
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    addProductToCart(item)
                                                    searchQuery = ""
                                                    showSearchResultsDropdown = false
                                                    keyboardController?.hide()
                                                }
                                            )
                                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        }
                                    }
                                }
                            }
                        }

                        // Camera Scan Toggle Capsule matching OutlinedTextField shape & size
                        OutlinedButton(
                            onClick = {
                                if (cameraPermissionState.status.isGranted) {
                                    isScannerActive = !isScannerActive
                                    if (isScannerActive) {
                                        scannerStatusMessage = "Align code within guidelines"
                                    }
                                } else {
                                    cameraPermissionState.launchPermissionRequest()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isScannerActive) Red500 else MaterialTheme.colorScheme.outline
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isScannerActive) Red500.copy(alpha = 0.12f) else Color.Transparent,
                                contentColor = if (isScannerActive) Red500 else MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isScannerActive) Icons.Default.VideocamOff else Icons.Default.QrCodeScanner,
                                    contentDescription = "Toggle Scan",
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (isScannerActive) "Stop" else "Scan",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // 2. Camera Barcode Scanner Section (If Active)
                    AnimatedVisibility(visible = isScannerActive) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    2.dp, 
                                    if (isAddingScannedItem) Emerald500 
                                    else if (scanDebounceActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) 
                                    else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (cameraPermissionState.status.isGranted) {
                                        CameraScannerPreview(
                                            onBarcodeDetected = { code ->
                                                handleBarcodeScanned(code)
                                            }
                                        )

                                        // Focus reticle visual with reactive feedback
                                        Box(
                                            modifier = Modifier
                                                .size(width = 200.dp, height = 80.dp)
                                                .align(Alignment.Center)
                                                .border(
                                                    2.dp, 
                                                    if (isAddingScannedItem) Emerald500 
                                                    else if (scanDebounceActive) Emerald500.copy(alpha = 0.5f) 
                                                    else Color.White.copy(alpha = 0.85f), 
                                                    RoundedCornerShape(8.dp)
                                                )
                                        )

                                        // Status Toast Text inside Camera View
                                        Surface(
                                            color = if (isAddingScannedItem) Emerald500.copy(alpha = 0.92f) else Color.Black.copy(alpha = 0.75f),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                            ) {
                                                if (isAddingScannedItem) {
                                                    CircularProgressIndicator(
                                                        color = Color.White,
                                                        strokeWidth = 2.dp,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                }
                                                Text(
                                                    text = scannerStatusMessage.ifBlank { "Align barcode within viewfinder" },
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isAddingScannedItem) FontWeight.Bold else FontWeight.SemiBold,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "Camera permission is required.",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Customer info collapsible input expansion
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            label = { Text("Customer Name *", fontSize = 10.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.2f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                        )
                        OutlinedTextField(
                            value = customerPhone,
                            onValueChange = { customerPhone = it },
                            label = { Text("Phone Number *", fontSize = 10.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(0.8f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. Cart items list
                    if (cartItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.AddShoppingCart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(72.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Your billing cart is empty.",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Search products manually or toggle camera scan to add items.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            }
                        }
                    } else {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "Billed Items (${cartItems.sumOf { it.quantity }})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            LazyColumn(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(cartItems) { cartItem ->
                                    CartItemRow(
                                        cartItem = cartItem,
                                        onIncrease = {
                                            if (cartItem.quantity + 1 > cartItem.item.quantity) {
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Cannot exceed available stock (${cartItem.item.quantity} units)!")
                                                }
                                            } else {
                                                val index = cartItems.indexOf(cartItem)
                                                if (index != -1) {
                                                    cartItems[index] = cartItem.copy(quantity = cartItem.quantity + 1)
                                                }
                                            }
                                        },
                                        onDecrease = {
                                            if (cartItem.quantity - 1 <= 0) {
                                                cartItems.remove(cartItem)
                                            } else {
                                                val index = cartItems.indexOf(cartItem)
                                                if (index != -1) {
                                                    cartItems[index] = cartItem.copy(quantity = cartItem.quantity - 1)
                                                }
                                            }
                                        },
                                        onRemove = {
                                            cartItems.remove(cartItem)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 4. Totals, GST, and Checkout Button
                    if (cartItems.isNotEmpty()) {
                        // Math details
                        val totalBillAmount = cartItems.sumOf { it.item.sellingPrice * it.quantity }
                        val costSubtotal = cartItems.sumOf { it.item.costPrice * it.quantity }
                        val estimatedProfit = totalBillAmount - costSubtotal

                        // 18% GST built-in calculation
                        val gstAmount = totalBillAmount * 0.18
                        val finalWithGst = totalBillAmount

                        Card(
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Billed Amount (Excl. Tax)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(IndianFormatUtils.formatInr(totalBillAmount - gstAmount), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("GST Taxes (CGST 9% + SGST 9%)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(IndianFormatUtils.formatInr(gstAmount), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Gross Margin Profit", fontSize = 11.sp, color = Emerald500, fontWeight = FontWeight.Bold)
                                    Text(IndianFormatUtils.formatInr(estimatedProfit), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Emerald500)
                                }

                                Divider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Net Payable Grand Total", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        text = IndianFormatUtils.formatInr(finalWithGst),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Payment Method Selection Capsule
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Payment System:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf("UPI", "Cash", "Card").forEach { method ->
                                            val isSelected = paymentMethod == method
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                        shape = RoundedCornerShape(16.dp)
                                                    )
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline,
                                                        shape = RoundedCornerShape(16.dp)
                                                    )
                                                    .clickable { paymentMethod = method }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = method,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                val isFormFilled = customerName.trim().isNotEmpty() && customerPhone.trim().isNotEmpty()

                                // Checkout Primary Button matching surrounding yellow and state
                                Button(
                                    enabled = isFormFilled,
                                    onClick = {
                                        coroutineScope.launch {
                                            try {
                                                val invoiceId = "AS-${System.currentTimeMillis().toString().takeLast(6)}"
                                                var successCount = 0

                                                // Update Inventory Loop
                                                cartItems.forEach { cartItem ->
                                                    val res = onCheckout(cartItem.item.id, cartItem.quantity)
                                                    if (res.isSuccess) {
                                                        successCount++
                                                    }
                                                }

                                                if (successCount > 0) {
                                                    triggerSuccessHaptic()
                                                    // Prepare success receipt
                                                    val receipt = InvoiceReceipt(
                                                        invoiceId = invoiceId,
                                                        customerName = customerName.ifBlank { "Walk-in Retail Customer" },
                                                        customerPhone = customerPhone.ifBlank { "N/A" },
                                                        items = cartItems.toList(),
                                                        subtotal = totalBillAmount - gstAmount,
                                                        gst = gstAmount,
                                                        total = finalWithGst,
                                                        paymentMethod = paymentMethod,
                                                        timestamp = System.currentTimeMillis()
                                                    )
                                                    showSuccessReceipt = receipt
                                                    onInvoiceFinalized(receipt)
                                                } else {
                                                    snackbarHostState.showSnackbar("Failed to update inventory!")
                                                }
                                            } catch (e: Exception) {
                                                snackbarHostState.showSnackbar("Checkout Error: ${e.localizedMessage}")
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("submit_bill_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    border = if (isFormFilled) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        disabledContainerColor = Color.Transparent,
                                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (isFormFilled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("PROCEED & DEDUCT STOCK", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

    // Success Invoice Receipt Dialogue Overlay
    if (showSuccessReceipt != null) {
        ReceiptSuccessDialog(
            receipt = showSuccessReceipt!!,
            onDismiss = {
                cartItems.clear()
                showSuccessReceipt = null
                onDismissRequest?.invoke()
            }
        )
    }
}

@Composable
fun SaffronPrimaryGreen(): Color {
    return if (MaterialTheme.colorScheme.surface == DarkSurface) {
        Color(0xFF10B981) // Crisp Emerald green for Dark Theme
    } else {
        MaterialTheme.colorScheme.primary // standard primary
    }
}

@Composable
fun CartItemRow(
    cartItem: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1.2f)) {
                Text(
                    text = cartItem.item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "SKU: ${cartItem.item.sku} • Unit: ${IndianFormatUtils.formatInr(cartItem.item.sellingPrice)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Available Stock: ${cartItem.item.quantity} ${cartItem.item.unit}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (cartItem.item.quantity < cartItem.item.minStockThreshold) Red500 else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Quantity adjust panel
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(0.8f)
            ) {
                IconButton(
                    onClick = onDecrease,
                    modifier = Modifier
                        .size(28.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(14.dp))
                }

                Text(
                    text = "${cartItem.quantity}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(28.dp)
                )

                IconButton(
                    onClick = onIncrease,
                    modifier = Modifier
                        .size(28.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(14.dp))
                }
            }

            // Total price row item
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(0.7f)
            ) {
                Text(
                    text = IndianFormatUtils.formatInr(cartItem.item.sellingPrice * cartItem.quantity),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove Item",
                        tint = Red500.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptSuccessDialog(
    receipt: InvoiceReceipt,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sdf = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault())
    val dateString = sdf.format(Date(receipt.timestamp))

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 520.dp)
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Done check icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFE6F4EA), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF137333),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Stock Adjusted & Billed!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF137333)
                )
                Text(
                    text = "Invoice Generated Successfully",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Simulated paper receipt design
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (MaterialTheme.colorScheme.surface == DarkSurface) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Logo title
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "AUTOSTOCK",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "INV: #${receipt.invoiceId}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        // Customer metadata
                        Text("Customer: ${receipt.customerName}", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        if (receipt.customerPhone != "N/A" && receipt.customerPhone.isNotBlank()) {
                            Text("Contact: +91 ${receipt.customerPhone}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("Date: $dateString", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Payment Type: ${receipt.paymentMethod}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)

                        Divider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        // Invoice products table
                        Text("ITEMS SOLD:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))

                        receipt.items.forEach { cartItem ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${cartItem.item.name} x${cartItem.quantity}",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1.5f)
                                )
                                Text(
                                    text = IndianFormatUtils.formatInr(cartItem.item.sellingPrice * cartItem.quantity),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(0.5f)
                                )
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        // Summary prices
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(IndianFormatUtils.formatInr(receipt.subtotal), fontSize = 11.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("GST (18% inclusive)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(IndianFormatUtils.formatInr(receipt.gst), fontSize = 11.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("NET TOTAL PAID", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                            Text(
                                text = IndianFormatUtils.formatInr(receipt.total),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Text("Close", fontSize = 11.sp, maxLines = 1)
                    }

                    Button(
                        onClick = {
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
                        modifier = Modifier.weight(1.1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Preview", fontSize = 11.sp, maxLines = 1)
                    }

                    Button(
                        onClick = {
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
                        },
                        modifier = Modifier.weight(1.1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print PDF", fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

// Compact camera scanner analyzer implementation built on CameraX
@ExperimentalGetImage
@Composable
fun CameraScannerPreview(
    onBarcodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            try {
                if (cameraProviderFuture.isDone) {
                    cameraProviderFuture.get().unbindAll()
                }
            } catch (_: Exception) {}
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }

                // Config Barcode options inside camera
                val options = BarcodeScannerOptions.Builder().build()
                val scanner = BarcodeScanning.getClient(options)

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                for (barcode in barcodes) {
                                    barcode.rawValue?.let { code ->
                                        if (code.isNotBlank()) {
                                            onBarcodeDetected(code)
                                        }
                                    }
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (_: Exception) {}
            }, ContextCompat.getMainExecutor(context))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}
