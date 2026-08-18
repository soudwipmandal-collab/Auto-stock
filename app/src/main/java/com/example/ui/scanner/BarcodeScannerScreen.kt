package com.example.ui.scanner

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
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
import com.example.ui.theme.DarkSurface
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.InventoryItem
import com.example.data.model.RecentScanRecord
import com.example.data.model.StockStatus
import com.example.data.model.TransactionType
import com.example.ui.theme.Amber400
import com.example.ui.theme.Amber500
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Cyan600
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.Red500
import com.example.ui.theme.Red600
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
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class, ExperimentalGetImage::class)
@Composable
fun BarcodeScannerScreen(
    allItems: List<InventoryItem>,
    recentScans: List<RecentScanRecord> = emptyList(),
    onLookupItem: suspend (String) -> InventoryItem?,
    onAdjustStock: suspend (Long, Int, TransactionType, String) -> Result<InventoryItem>,
    onRecordRecentScan: (String, InventoryItem?) -> Unit = { _, _ -> },
    onClearRecentScans: () -> Unit = {},
    onViewItemDetails: (InventoryItem) -> Unit,
    onAddNewItemWithBarcode: ((String) -> Unit)? = null,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val strings = currentStrings()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    var stockDeltaQuantity by remember { mutableIntStateOf(1) }
    var quantityInputText by remember { mutableStateOf("1") }
    var scannedItem by remember { mutableStateOf<InventoryItem?>(null) }
    var lastScannedCode by remember { mutableStateOf("") }
    var manualInputCode by remember { mutableStateOf("") }
    var isManualInputVisible by remember { mutableStateOf(false) }
    var isFlashOn by remember { mutableStateOf(false) }
    var cameraLensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var isSearching by remember { mutableStateOf(false) }
    var actionSuccessMessage by remember { mutableStateOf<String?>(null) }
    var unregisteredBarcodeToCreate by remember { mutableStateOf<String?>(null) }
    var isRecentScansSheetVisible by remember { mutableStateOf(false) }

    // Dedicated ToneGenerator for barcode scan beep
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

    fun triggerScanHapticAndBeep() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        } catch (_: Exception) {}
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(70)
            }
        } catch (_: Exception) {}
    }

    fun handleCodeDetected(code: String) {
        if (code.isBlank() || isSearching) return
        lastScannedCode = code.trim()
        isSearching = true
        triggerScanHapticAndBeep()

        coroutineScope.launch {
            val found = onLookupItem(lastScannedCode)
            onRecordRecentScan(lastScannedCode, found)
            isSearching = false
            scannedItem = found
            stockDeltaQuantity = 1
            quantityInputText = "1"
            actionSuccessMessage = null

            if (found != null) {
                unregisteredBarcodeToCreate = null
            } else {
                unregisteredBarcodeToCreate = lastScannedCode
                snackbarHostState.showSnackbar("Barcode not registered: $lastScannedCode")
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("barcode_scanner_screen")
    ) {
        // 1. Camera Live View or Permission Request View
        if (cameraPermissionState.status.isGranted) {
            CameraPreviewWithMlKit(
                lensFacing = cameraLensFacing,
                isFlashOn = isFlashOn,
                onBarcodeScanned = { detectedCode ->
                    handleCodeDetected(detectedCode)
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Camera Permission Placeholder
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(AmberContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Camera Icon",
                            tint = Saffron500,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = strings.scanTitle,
                        color = Slate50,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.scanSubtitle,
                        color = Slate400,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    OutlinedButton(
                        onClick = { cameraPermissionState.launchPermissionRequest() },
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black),
                        modifier = Modifier.testTag("enable_camera_permission_button")
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.grantPermission, color = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Optical Reticle Overlay with Laser Animation
        ScannerOverlayView(
            isSearching = isSearching,
            modifier = Modifier.fillMaxSize()
        )

        // 3. Header Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            // Action & Camera Utility Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .background(
                                if (isSearching) Saffron500 else Emerald500,
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = strings.scanTitle,
                            color = Slate50,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isSearching) "Searching..." else "Ready to scan",
                            color = if (isSearching) Saffron400 else Slate400,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Recent Scans Toggle Button
                    IconButton(
                        onClick = { isRecentScansSheetVisible = true },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("recent_scans_toggle_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (recentScans.isNotEmpty()) {
                                    Badge(
                                        containerColor = Saffron500,
                                        contentColor = Slate950
                                    ) {
                                        Text("${recentScans.size}", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = strings.recentScansTitle,
                                tint = if (isRecentScansSheetVisible) Saffron500 else Slate300,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Manual Keyboard Entry Toggle
                    IconButton(
                        onClick = { isManualInputVisible = !isManualInputVisible },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = strings.manualCodeEntry,
                            tint = if (isManualInputVisible) Saffron500 else Slate400,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Flashlight Toggle
                    IconButton(
                        onClick = { isFlashOn = !isFlashOn },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Torch",
                            tint = if (isFlashOn) Amber400 else Slate400,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Camera Switch (Front/Back)
                    IconButton(
                        onClick = {
                            cameraLensFacing = if (cameraLensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlipCameraAndroid,
                            contentDescription = "Switch Camera",
                            tint = Slate400,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Manual Code Input Box (Collapsible)
            AnimatedVisibility(
                visible = isManualInputVisible,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualInputCode,
                            onValueChange = { manualInputCode = it },
                            placeholder = { Text(strings.enterBarcode, color = Slate400, fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Slate50,
                                unfocusedTextColor = Slate50,
                                focusedBorderColor = Saffron500,
                                unfocusedBorderColor = Slate700
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                keyboardController?.hide()
                                handleCodeDetected(manualInputCode)
                            }),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manual_barcode_input")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = {
                                keyboardController?.hide()
                                handleCodeDetected(manualInputCode)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black),
                            modifier = Modifier.testTag("manual_barcode_submit_button")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = strings.search, tint = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black)
                        }
                    }
                }
            }
        }

        // 4. Quick Test Barcodes Carousel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = if (scannedItem != null) 360.dp else 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.quickTestBarcodes.uppercase(),
                    color = Slate400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isSearching) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(11.dp), strokeWidth = 2.dp, color = Saffron500)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Looking up...", color = Saffron400, fontSize = 10.sp)
                    }
                }
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allItems) { item ->
                    Surface(
                        onClick = { handleCodeDetected(item.barcode) },
                        shape = RoundedCornerShape(10.dp),
                        color = Slate900.copy(alpha = 0.9f),
                        border = BorderStroke(1.dp, if (lastScannedCode == item.barcode) Saffron500 else Slate700),
                        modifier = Modifier.testTag("sample_scan_${item.sku}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                when (item.category) {
                                    "Cars" -> Icons.Default.DirectionsCar
                                    "Bikes" -> Icons.Default.TwoWheeler
                                    else -> Icons.Default.Build
                                },
                                contentDescription = null,
                                tint = if (item.quantity <= item.minStockThreshold) Red500 else Saffron500,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = item.name,
                                    color = Slate50,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 130.dp)
                                )
                                Text(
                                    text = "${item.sku} • ${item.quantity} ${item.unit}",
                                    color = Slate400,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Scanned Product Result Sheet
        AnimatedVisibility(
            visible = scannedItem != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            scannedItem?.let { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("scanned_result_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (item.quantity <= item.minStockThreshold) Red500 else Saffron500),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Header: Status badge & Close
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            when (item.stockStatus) {
                                                StockStatus.OUT_OF_STOCK -> Red500
                                                StockStatus.LOW_STOCK -> Amber500
                                                StockStatus.IN_STOCK -> Emerald500
                                            },
                                            CircleShape
                                        )
                                    )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when (item.stockStatus) {
                                        StockStatus.OUT_OF_STOCK -> strings.outOfStock
                                        StockStatus.LOW_STOCK -> strings.lowStock
                                        StockStatus.IN_STOCK -> strings.inStock
                                    }.uppercase(),
                                    color = when (item.stockStatus) {
                                        StockStatus.OUT_OF_STOCK -> Red500
                                        StockStatus.LOW_STOCK -> Amber400
                                        StockStatus.IN_STOCK -> Emerald500
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(
                                onClick = {
                                    scannedItem = null
                                    actionSuccessMessage = null
                                },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = strings.cancel, tint = Slate400, modifier = Modifier.size(16.dp))
                            }
                        }

                        // Success action toast banner inside card
                        actionSuccessMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(EmeraldContainer, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald500, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = msg, color = Emerald500, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Product Title & Specs
                        Text(
                            text = item.name,
                            color = Slate50,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${item.category} • ${item.subcategory} • ${strings.sku}: ${item.sku}",
                            color = Saffron400,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        if (item.fitment.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${strings.fitment}: ${item.fitment}",
                                color = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Stock & Pricing Summary Grid
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Slate850, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(strings.inStock, color = Slate400, fontSize = 11.sp)
                                Text(
                                    text = "${item.quantity} ${item.unit}",
                                    color = if (item.quantity <= item.minStockThreshold) Red500 else Emerald500,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text(strings.location, color = Slate400, fontSize = 11.sp)
                                Text(
                                    text = item.locationRack.ifBlank { "Unassigned" },
                                    color = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(strings.sellingPrice, color = Slate400, fontSize = 11.sp)
                                Text(
                                    IndianFormatUtils.formatInr(item.sellingPrice),
                                    color = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quantity Selector for Action ([-] [ Qty Units Input ] [+] & quick increment chips)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "QUANTITY & TRANSACTION",
                                color = Slate400,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Tap box to type number",
                                color = Saffron400,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Decrement button
                            Surface(
                                onClick = {
                                    if (stockDeltaQuantity > 1) {
                                        stockDeltaQuantity--
                                        quantityInputText = stockDeltaQuantity.toString()
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = Slate800,
                                border = BorderStroke(1.dp, Slate700),
                                enabled = stockDeltaQuantity > 1,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Decrease Quantity",
                                        tint = if (stockDeltaQuantity > 1) Slate50 else Slate600,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Manual Number Input Quantity Box
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Slate900,
                                border = BorderStroke(1.dp, Saffron500.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    BasicTextField(
                                        value = quantityInputText,
                                        onValueChange = { input ->
                                            val filtered = input.filter { it.isDigit() }.take(5)
                                            quantityInputText = filtered
                                            val parsed = filtered.toIntOrNull()
                                            if (parsed != null && parsed > 0) {
                                                stockDeltaQuantity = parsed
                                            } else if (filtered.isEmpty()) {
                                                stockDeltaQuantity = 1
                                            }
                                        },
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            color = Slate50,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        ),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = {
                                                keyboardController?.hide()
                                                if (quantityInputText.isEmpty() || (quantityInputText.toIntOrNull() ?: 0) <= 0) {
                                                    quantityInputText = "1"
                                                    stockDeltaQuantity = 1
                                                }
                                            }
                                        ),
                                        singleLine = true,
                                        cursorBrush = SolidColor(Saffron400),
                                        modifier = Modifier
                                            .widthIn(min = 28.dp, max = 56.dp)
                                            .testTag("scanner_quantity_input")
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = item.unit,
                                        color = Slate400,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Increment button
                            Surface(
                                onClick = {
                                    stockDeltaQuantity++
                                    quantityInputText = stockDeltaQuantity.toString()
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = Slate800,
                                border = BorderStroke(1.dp, Slate700),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Increase Quantity",
                                        tint = Slate50,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Quick Presets: 1, 5, 10, 20
                            listOf(1, 5, 10, 20).forEach { qty ->
                                Surface(
                                    onClick = {
                                        stockDeltaQuantity = qty
                                        quantityInputText = qty.toString()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (stockDeltaQuantity == qty) Saffron500 else Slate800,
                                    border = BorderStroke(1.dp, if (stockDeltaQuantity == qty) Saffron500 else Slate700),
                                    modifier = Modifier
                                        .height(38.dp)
                                        .width(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "$qty",
                                            color = if (stockDeltaQuantity == qty) Slate950 else Slate300,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Stock In / Stock Out Action Choice Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Stock In Action Button
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val res = onAdjustStock(
                                            item.id,
                                            stockDeltaQuantity,
                                            TransactionType.STOCK_IN,
                                            "Stock In from scanner (+$stockDeltaQuantity)"
                                        )
                                        if (res.isSuccess) {
                                            scannedItem = res.getOrNull()
                                            actionSuccessMessage = "+$stockDeltaQuantity ${item.unit} Stocked In • New Total: ${res.getOrNull()?.quantity ?: (item.quantity + stockDeltaQuantity)}"
                                            snackbarHostState.showSnackbar("Restocked +$stockDeltaQuantity ${item.name}")
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Emerald600,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .testTag("action_stock_in_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Stock In (+$stockDeltaQuantity)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Stock Out Action Button
                            val canStockOut = item.quantity >= stockDeltaQuantity
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        if (!canStockOut) {
                                            snackbarHostState.showSnackbar("Cannot dispatch $stockDeltaQuantity. Available: ${item.quantity} ${item.unit}")
                                            return@launch
                                        }
                                        val res = onAdjustStock(
                                            item.id,
                                            -stockDeltaQuantity,
                                            TransactionType.STOCK_OUT,
                                            "Stock Out from scanner (-$stockDeltaQuantity)"
                                        )
                                        if (res.isSuccess) {
                                            scannedItem = res.getOrNull()
                                            actionSuccessMessage = "-$stockDeltaQuantity ${item.unit} Stocked Out • Remaining: ${res.getOrNull()?.quantity ?: (item.quantity - stockDeltaQuantity)}"
                                            snackbarHostState.showSnackbar("Dispatched -$stockDeltaQuantity ${item.name}")
                                        }
                                    }
                                },
                                enabled = item.quantity > 0,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Red600,
                                    disabledContainerColor = Slate800,
                                    contentColor = Color.White,
                                    disabledContentColor = Slate600
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .testTag("action_stock_out_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = null,
                                    tint = if (item.quantity > 0) Color.White else Slate600,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Stock Out (-$stockDeltaQuantity)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Bottom Actions: Details & Scan Next
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onViewItemDetails(item) },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, tint = Cyan600, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("View Details", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }

                            OutlinedButton(
                                onClick = {
                                    scannedItem = null
                                    actionSuccessMessage = null
                                    stockDeltaQuantity = 1
                                    quantityInputText = "1"
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Saffron500, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Scan Next", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }

        // 6. Unregistered Barcode Dialog
        unregisteredBarcodeToCreate?.let { unrecognizedCode ->
            AlertDialog(
                onDismissRequest = { unregisteredBarcodeToCreate = null },
                shape = RoundedCornerShape(14.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                icon = {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(AmberContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, tint = Saffron500, modifier = Modifier.size(22.dp))
                    }
                },
                title = {
                    Text(
                        text = strings.unregisteredBarcode,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${strings.enterBarcode}:",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = unrecognizedCode,
                            color = Saffron500,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = strings.unregisteredBarcodeDesc,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val code = unrecognizedCode
                            unregisteredBarcodeToCreate = null
                            onAddNewItemWithBarcode?.invoke(code)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black,
                            contentColor = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.Black else Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.registerProduct, color = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { unregisteredBarcodeToCreate = null }) {
                        Text(strings.cancel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }

        if (isRecentScansSheetVisible) {
            RecentScansSheet(
                recentScans = recentScans,
                onSelectBarcode = { code ->
                    lastScannedCode = code
                    handleCodeDetected(code)
                },
                onClearHistory = onClearRecentScans,
                onDismiss = { isRecentScansSheetVisible = false },
                onViewItemDetails = onViewItemDetails,
                allItems = allItems,
                snackbarHostState = snackbarHostState
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecentScansSheet(
    recentScans: List<RecentScanRecord>,
    onSelectBarcode: (String) -> Unit,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit,
    onViewItemDetails: (InventoryItem) -> Unit,
    allItems: List<InventoryItem>,
    snackbarHostState: SnackbarHostState
) {
    val strings = currentStrings()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp),
                color = MaterialTheme.colorScheme.outline,
                shape = CircleShape
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Saffron500.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = Saffron500,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = strings.recentScansTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = strings.recentScansSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                if (recentScans.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            onClearHistory()
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Scan history cleared")
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = strings.clearHistory,
                            tint = Red500,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strings.clearHistory,
                            color = Red500,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (recentScans.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = strings.noRecentScans,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    items(recentScans, key = { it.barcode + "_" + it.timestamp }) { scan ->
                        val matchingItem = allItems.find {
                            it.barcode.equals(scan.barcode, ignoreCase = true) || it.sku.equals(scan.barcode, ignoreCase = true)
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (matchingItem != null) Saffron500.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
                                    ) {
                                        Text(
                                            text = scan.barcode,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Saffron500,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    Text(
                                        text = formatTimeAgo(scan.timestamp),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (matchingItem != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = matchingItem.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "SKU: ${matchingItem.sku} • ${matchingItem.category}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Surface(
                                            color = when {
                                                matchingItem.quantity <= 0 -> RedContainer
                                                matchingItem.quantity <= matchingItem.minStockThreshold -> AmberContainer
                                                else -> EmeraldContainer
                                            },
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "${matchingItem.quantity} ${matchingItem.unit}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when {
                                                    matchingItem.quantity <= 0 -> Red500
                                                    matchingItem.quantity <= matchingItem.minStockThreshold -> Amber400
                                                    else -> Emerald500
                                                },
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        text = scan.itemName ?: strings.unregisteredBarcode,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = Amber400
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            val clip = ClipData.newPlainText("Barcode", scan.barcode)
                                            clipboard?.setPrimaryClip(clip)
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(strings.barcodeCopied)
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = strings.copyBarcode,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = strings.copyBarcode, fontSize = 11.sp)
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Button(
                                        onClick = {
                                            onSelectBarcode(scan.barcode)
                                            onDismiss()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Saffron500,
                                            contentColor = Color.Black
                                        )
                                    ) {
                                        Text(text = "Scan Again", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimeAgo(timestamp: Long): String {
    val diffSec = (System.currentTimeMillis() - timestamp) / 1000
    return when {
        diffSec < 60 -> "Just now"
        diffSec < 3600 -> "${diffSec / 60}m ago"
        diffSec < 86400 -> "${diffSec / 3600}h ago"
        else -> {
            val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

/**
 * CameraX Live Preview with real ML Kit Barcode Scanning Engine
 */
@Composable
fun CameraPreviewWithMlKit(
    lensFacing: Int,
    isFlashOn: Boolean,
    onBarcodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember { PreviewView(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(lensFacing, isFlashOn) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val barcodeScanner = BarcodeScanning.getClient(
                    BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                        .build()
                )

                var lastScannedTimestamp = 0L

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImageProxy(barcodeScanner, imageProxy) { scannedValue ->
                        val now = System.currentTimeMillis()
                        if (now - lastScannedTimestamp > 1400L) {
                            lastScannedTimestamp = now
                            ContextCompat.getMainExecutor(context).execute {
                                onBarcodeScanned(scannedValue)
                            }
                        }
                    }
                }

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
                camera.cameraControl.enableTorch(isFlashOn)
            } catch (_: Exception) {}
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()
            } catch (_: Exception) {}
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    imageProxy: ImageProxy,
    onSuccess: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val raw = barcode.rawValue ?: barcode.displayValue
                    if (!raw.isNullOrBlank()) {
                        onSuccess(raw)
                        break
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

/**
 * Viewfinder HUD with Animated Laser Barcode Scanner Line
 */
@Composable
fun ScannerOverlayView(
    isSearching: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "laser_transition")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_pos"
    )

    val laserColor = Emerald500

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Reticle box dimensions
        val boxWidth = w * 0.76f
        val boxHeight = h * 0.32f
        val left = (w - boxWidth) / 2f
        val top = (h - boxHeight) / 2.3f
        val right = left + boxWidth
        val bottom = top + boxHeight
        val cornerLen = 28f

        // Draw translucent dark vignette around the reticle box
        drawRect(
            color = Color(0x77000000),
            size = size
        )

        // Clear the viewfinder box
        drawRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(boxWidth, boxHeight)
        )

        // Viewfinder borders
        drawRect(
            color = Color(0x22FFFFFF),
            topLeft = Offset(left, top),
            size = Size(boxWidth, boxHeight),
            style = Stroke(width = 1.2f)
        )

        // Draw 4 Corner Brackets
        val cornerColor = if (isSearching) Saffron500 else laserColor
        val cornerStroke = 4f

        // Top-Left
        drawLine(cornerColor, Offset(left, top), Offset(left + cornerLen, top), cornerStroke, StrokeCap.Round)
        drawLine(cornerColor, Offset(left, top), Offset(left, top + cornerLen), cornerStroke, StrokeCap.Round)

        // Top-Right
        drawLine(cornerColor, Offset(right, top), Offset(right - cornerLen, top), cornerStroke, StrokeCap.Round)
        drawLine(cornerColor, Offset(right, top), Offset(right, top + cornerLen), cornerStroke, StrokeCap.Round)

        // Bottom-Left
        drawLine(cornerColor, Offset(left, bottom), Offset(left + cornerLen, bottom), cornerStroke, StrokeCap.Round)
        drawLine(cornerColor, Offset(left, bottom), Offset(left, bottom - cornerLen), cornerStroke, StrokeCap.Round)

        // Bottom-Right
        drawLine(cornerColor, Offset(right, bottom), Offset(right - cornerLen, bottom), cornerStroke, StrokeCap.Round)
        drawLine(cornerColor, Offset(right, bottom), Offset(right, bottom - cornerLen), cornerStroke, StrokeCap.Round)

        // Draw Laser Line across the box
        val laserY = top + (boxHeight * laserPosition)
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, cornerColor, Color.White, cornerColor, Color.Transparent),
                startX = left,
                endX = right
            ),
            start = Offset(left + 8f, laserY),
            end = Offset(right - 8f, laserY),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )

        // Soft laser glow rect
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(cornerColor.copy(alpha = 0.2f), Color.Transparent),
                startY = laserY,
                endY = laserY + 20f
            ),
            topLeft = Offset(left + 8f, laserY),
            size = Size(boxWidth - 16f, 20f)
        )
    }
}
