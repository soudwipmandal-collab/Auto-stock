package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import com.example.ui.theme.Emerald500
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.InventoryItem
import com.example.ui.MainViewModel
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.TextStyle
import com.example.ui.alerts.StockAlertsScreen
import com.example.ui.analytics.AnalyticsScreen
import com.example.ui.inventory.AddEditItemDialog
import com.example.ui.inventory.InventoryScreen
import com.example.ui.inventory.ItemDetailDialog
import com.example.ui.billing.BillingDialog
import com.example.ui.billing.BillingHistoryDialog
import com.example.ui.scanner.BarcodeScannerScreen
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Red500
import com.example.ui.theme.Saffron500
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.ui.util.AppLanguage
import com.example.ui.util.AppStrings
import com.example.ui.util.LocalAppLanguage
import com.example.ui.util.LocalAppStrings

sealed class Screen(
    val getTitle: (AppStrings) -> String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    data object Inventory : Screen(
        getTitle = { it.navInventory },
        selectedIcon = Icons.Filled.Inventory2,
        unselectedIcon = Icons.Outlined.Inventory2,
        testTag = "nav_inventory"
    )

    data object Scanner : Screen(
        getTitle = { it.navScanner },
        selectedIcon = Icons.Filled.QrCodeScanner,
        unselectedIcon = Icons.Outlined.QrCodeScanner,
        testTag = "nav_scanner"
    )

    data object Alerts : Screen(
        getTitle = { it.navAlerts },
        selectedIcon = Icons.Filled.NotificationsActive,
        unselectedIcon = Icons.Outlined.Notifications,
        testTag = "nav_alerts"
    )

    data object Billing : Screen(
        getTitle = { if (it.navInventory == "Inventory") "Billing" else if (it.navInventory == "इन्वेंट्री") "बिलिंग" else "বিলিং" },
        selectedIcon = Icons.Filled.ShoppingCart,
        unselectedIcon = Icons.Outlined.ShoppingCart,
        testTag = "nav_billing"
    )

    data object Analytics : Screen(
        getTitle = { it.navAnalytics },
        selectedIcon = Icons.Filled.ShowChart,
        unselectedIcon = Icons.Outlined.ShowChart,
        testTag = "nav_analytics"
    )
}

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
            MyApplicationTheme(darkTheme = isDarkTheme) {
                AutoStockApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoStockApp(viewModel: MainViewModel) {
    val allItems by viewModel.allItems.collectAsStateWithLifecycle()
    val alertItems by viewModel.alertItems.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val recentScans by viewModel.recentScans.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()

    val strings = remember(selectedLanguage) { AppStrings.getStrings(selectedLanguage) }

    var currentScreenIndex by remember { mutableIntStateOf(0) }
    val screens = listOf(Screen.Inventory, Screen.Scanner, Screen.Billing, Screen.Alerts, Screen.Analytics)

    val snackbarHostState = remember { SnackbarHostState() }

    var selectedItemForDetail by remember { mutableStateOf<InventoryItem?>(null) }
    var itemToEditFromDetail by remember { mutableStateOf<InventoryItem?>(null) }
    var barcodeForNewItem by remember { mutableStateOf<String?>(null) }
    var isBillingOpen by remember { mutableStateOf(false) }
    var isHamburgerOpen by remember { mutableStateOf(false) }
    var isHistoryOpen by remember { mutableStateOf(false) }
    var isSettingsExpanded by remember { mutableStateOf(false) }
    val billingHistory by viewModel.billingHistory.collectAsStateWithLifecycle()
    val globalThreshold by viewModel.globalSafetyThreshold.collectAsStateWithLifecycle()
    val showCars by viewModel.showCarsTab.collectAsStateWithLifecycle()
    val showBikes by viewModel.showBikesTab.collectAsStateWithLifecycle()
    val showSpareParts by viewModel.showSparePartsTab.collectAsStateWithLifecycle()

    CompositionLocalProvider(
        LocalAppLanguage provides selectedLanguage,
        LocalAppStrings provides strings
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("main_app_scaffold"),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = { isHamburgerOpen = true },
                            modifier = Modifier.testTag("hamburger_billing_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Navigation Menu",
                                tint = if (isDarkTheme) Color.White else Color.Black
                            )
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color.Transparent, RoundedCornerShape(7.dp))
                                    .border(1.dp, if (isDarkTheme) Color.White else Color.Black, RoundedCornerShape(7.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = if (isDarkTheme) Color.White else Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.appTitle,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = 0.3.sp
                            )
                        }
                    },
                    actions = {},
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 2.dp
                ) {
                    screens.forEachIndexed { index, screen ->
                        val isSelected = currentScreenIndex == index
                        val screenTitle = screen.getTitle(strings)
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentScreenIndex = index },
                            icon = {
                                if (screen == Screen.Alerts && alertItems.isNotEmpty()) {
                                    BadgedBox(
                                        badge = {
                                            Badge(
                                                containerColor = Red500,
                                                contentColor = Color.White
                                            ) {
                                                Text("${alertItems.size}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                            contentDescription = screenTitle,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                } else {
                                    Icon(
                                        if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screenTitle,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = screenTitle,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = if (isDarkTheme) Color.White else Color.Black,
                                selectedTextColor = if (isDarkTheme) Color.White else Color.Black,
                                indicatorColor = if (isDarkTheme) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.12f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag(screen.testTag)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (screens[currentScreenIndex]) {
                    Screen.Inventory -> {
                        InventoryScreen(
                            items = allItems,
                            onAddItem = { viewModel.addItem(it) },
                            onUpdateItem = { viewModel.updateItem(it) },
                            onDeleteItem = { viewModel.deleteItem(it) },
                            onAdjustStock = { id, delta, type, note -> viewModel.adjustStock(id, delta, type, note) },
                            onNavigateToScanner = { currentScreenIndex = 1 },
                            onViewItemDetails = { selectedItemForDetail = it },
                            snackbarHostState = snackbarHostState,
                            showCars = showCars,
                            showBikes = showBikes,
                            showSpareParts = showSpareParts
                        )
                    }
                    Screen.Scanner -> {
                        BarcodeScannerScreen(
                            allItems = allItems,
                            recentScans = recentScans,
                            onLookupItem = { viewModel.lookupByBarcodeOrSku(it) },
                            onAdjustStock = { id, delta, type, note -> viewModel.adjustStock(id, delta, type, note) },
                            onRecordRecentScan = { code, item -> viewModel.addRecentScan(code, item) },
                            onClearRecentScans = { viewModel.clearRecentScans() },
                            onViewItemDetails = { selectedItemForDetail = it },
                            onAddNewItemWithBarcode = { scannedCode ->
                                barcodeForNewItem = scannedCode
                            },
                            snackbarHostState = snackbarHostState
                        )
                    }
                    Screen.Billing -> {
                        com.example.ui.billing.BillingScreenContent(
                            allItems = allItems,
                            onCheckout = { itemId, qty ->
                                viewModel.adjustStock(itemId, -qty, com.example.data.model.TransactionType.STOCK_OUT, "Retail Billing / Invoice")
                            },
                            onInvoiceFinalized = { viewModel.addInvoiceReceipt(it) },
                            snackbarHostState = snackbarHostState,
                            onDismissRequest = null
                        )
                    }
                    Screen.Alerts -> {
                        StockAlertsScreen(
                            alertItems = alertItems,
                            onAdjustStock = { id, delta, type, note -> viewModel.adjustStock(id, delta, type, note) },
                            onNavigateToScanner = { currentScreenIndex = 1 },
                            onViewItemDetails = { selectedItemForDetail = it },
                            snackbarHostState = snackbarHostState,
                            showCars = showCars,
                            showBikes = showBikes,
                            showSpareParts = showSpareParts
                        )
                    }
                    Screen.Analytics -> {
                        AnalyticsScreen(
                            items = allItems,
                            transactions = transactions,
                            showCars = showCars,
                            showBikes = showBikes,
                            showSpareParts = showSpareParts
                        )
                    }
                }
            }
        }

        // Item Detail View Dialog
        selectedItemForDetail?.let { item ->
            val itemTransactions = transactions.filter { it.itemId == item.id }
            ItemDetailDialog(
                item = item,
                transactions = itemTransactions,
                onDismiss = { selectedItemForDetail = null },
                onEdit = {
                    itemToEditFromDetail = item
                    selectedItemForDetail = null
                },
                onAdjustStock = { id, delta, type, note ->
                    viewModel.adjustStock(id, delta, type, note)
                }
            )
        }

        // Edit Item Dialog from Details
        itemToEditFromDetail?.let { item ->
            AddEditItemDialog(
                itemToEdit = item,
                onDismiss = { itemToEditFromDetail = null },
                onSave = { updated ->
                    viewModel.updateItem(updated)
                    itemToEditFromDetail = null
                },
                onDelete = { deleted ->
                    viewModel.deleteItem(deleted)
                    itemToEditFromDetail = null
                },
                showCars = showCars,
                showBikes = showBikes,
                showSpareParts = showSpareParts
            )
        }

        // Add New Item Dialog from Barcode Scanner
        barcodeForNewItem?.let { code ->
            AddEditItemDialog(
                initialBarcode = code,
                onDismiss = { barcodeForNewItem = null },
                onSave = { newItem ->
                    viewModel.addItem(newItem)
                    barcodeForNewItem = null
                },
                showCars = showCars,
                showBikes = showBikes,
                showSpareParts = showSpareParts
            )
        }

        // Quick Billing Dialog
        if (isBillingOpen) {
            BillingDialog(
                allItems = allItems,
                onCheckout = { itemId, qty ->
                    viewModel.adjustStock(itemId, -qty, com.example.data.model.TransactionType.STOCK_OUT, "Retail Billing / Invoice")
                },
                onDismissRequest = { isBillingOpen = false },
                snackbarHostState = snackbarHostState,
                onInvoiceFinalized = { viewModel.addInvoiceReceipt(it) }
            )
        }

        // Navigation Hamburger Side Panel Overlay (1/2.5 of screen)
        if (isHamburgerOpen) {
            // Semi-transparent overlay to dim the rest of the screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { isHamburgerOpen = false }
            )

            // Side Drawer Panel itself (takes 75% of screen width)
            Surface(
                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.75f)
                    .widthIn(max = 290.dp)
                    .align(Alignment.CenterStart)
                    .clickable(enabled = false) { } // Prevent clicks through the panel
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "AutoStock",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )

                        IconButton(
                            onClick = { isHamburgerOpen = false },
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.CenterEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Menu",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Column containing safety settings and billing history, scrollable if height is limited
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // 1. BILLING HISTORY SHORTCUT (Super compacted 1-line layout)
                        Card(
                            onClick = {
                                isHamburgerOpen = false
                                isHistoryOpen = true
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Billing History",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 4. SETTINGS (Collapsible Options)
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                // Clickable header row to expand/collapse
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isSettingsExpanded = !isSettingsExpanded }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "Settings",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Settings",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Icon(
                                        imageVector = if (isSettingsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (isSettingsExpanded) "Collapse" else "Expand",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                if (isSettingsExpanded) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Theme toggle item
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.toggleTheme() }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isDarkTheme) Icons.Default.WbSunny else Icons.Default.DarkMode,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isDarkTheme) "Light Mode" else "Dark Mode",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Switch(
                                            checked = isDarkTheme,
                                            onCheckedChange = { viewModel.toggleTheme() },
                                            modifier = Modifier.scale(0.8f),
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Language Selector item
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Language,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Language",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        LanguageSelectorPill(
                                            currentLanguage = selectedLanguage,
                                            onLanguageSelected = { viewModel.setLanguage(it) }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = "Tab Visibility",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Cars toggle
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.setShowCarsTab(!showCars) }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.DirectionsCar,
                                                contentDescription = null,
                                                tint = if (showCars) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Cars",
                                                fontSize = 11.sp,
                                                color = if (showCars) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Checkbox(
                                            checked = showCars,
                                            onCheckedChange = { viewModel.setShowCarsTab(it) },
                                            modifier = Modifier.scale(0.7f).size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Bikes toggle
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.setShowBikesTab(!showBikes) }
                                            .padding(vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.TwoWheeler,
                                            contentDescription = null,
                                            tint = if (showBikes) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Bikes",
                                            fontSize = 11.sp,
                                            color = if (showBikes) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Checkbox(
                                        checked = showBikes,
                                        onCheckedChange = { viewModel.setShowBikesTab(it) },
                                        modifier = Modifier.scale(0.7f).size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Spare Parts toggle
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.setShowSparePartsTab(!showSpareParts) }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Build,
                                            contentDescription = null,
                                            tint = if (showSpareParts) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Spare Parts",
                                            fontSize = 11.sp,
                                            color = if (showSpareParts) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Checkbox(
                                        checked = showSpareParts,
                                        onCheckedChange = { viewModel.setShowSparePartsTab(it) },
                                        modifier = Modifier.scale(0.7f).size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                Spacer(modifier = Modifier.height(10.dp))

                                // Migrated Threshold Setting (the one originally below billing history)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Threshold Setting",
                                        tint = Saffron500,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Low Stock Threshold",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Units:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    var thresholdText by remember(globalThreshold) { mutableStateOf(globalThreshold.toString()) }
                                    val focusManager = LocalFocusManager.current
                                    val scope = rememberCoroutineScope()
                                    var showSuccessColor by remember { mutableStateOf(false) }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        BasicTextField(
                                            value = thresholdText,
                                            onValueChange = { newValue ->
                                                val cleanText = newValue.filter { it.isDigit() }
                                                if (cleanText.length <= 4) {
                                                    thresholdText = cleanText
                                                }
                                            },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Number,
                                                imeAction = ImeAction.Done
                                            ),
                                            keyboardActions = KeyboardActions(
                                                onDone = {
                                                    thresholdText.toIntOrNull()?.let { num ->
                                                        if (num in 1..9999) {
                                                            viewModel.setGlobalThreshold(num)
                                                            focusManager.clearFocus()
                                                            scope.launch {
                                                                showSuccessColor = true
                                                                delay(2000)
                                                                showSuccessColor = false
                                                            }
                                                        }
                                                    }
                                                }
                                            ),
                                            textStyle = TextStyle(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                            decorationBox = { innerTextField ->
                                                Row(
                                                    modifier = Modifier
                                                        .width(55.dp)
                                                        .height(28.dp)
                                                        .background(
                                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                            shape = RoundedCornerShape(4.dp)
                                                        )
                                                        .border(
                                                            width = 1.dp,
                                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                                            shape = RoundedCornerShape(4.dp)
                                                        )
                                                        .padding(horizontal = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Start
                                                ) {
                                                    innerTextField()
                                                }
                                            }
                                        )

                                        // Presets
                                        listOf(5, 10, 20).forEach { option ->
                                            val isSelected = globalThreshold == option
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(
                                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                    )
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) 
                                                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                        shape = RoundedCornerShape(4.dp)
                                                    )
                                                    .clickable { 
                                                        viewModel.setGlobalThreshold(option)
                                                        thresholdText = option.toString()
                                                        focusManager.clearFocus()
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "$option",
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer 
                                                            else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }

                                        // OK Button
                                        val okBgColor by animateColorAsState(
                                            targetValue = if (showSuccessColor) Emerald500 else MaterialTheme.colorScheme.primary,
                                            label = "ok_button_color"
                                        )
                                        
                                        Box(
                                            modifier = Modifier
                                                .height(28.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(okBgColor)
                                                .clickable { 
                                                    thresholdText.toIntOrNull()?.let { num ->
                                                        if (num in 1..9999) {
                                                            viewModel.setGlobalThreshold(num)
                                                            focusManager.clearFocus()
                                                            scope.launch {
                                                                showSuccessColor = true
                                                                delay(2000)
                                                                showSuccessColor = false
                                                            }
                                                        }
                                                    }
                                                }
                                                .padding(horizontal = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Ok",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (showSuccessColor) Color.White else MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                        // 5. ACCOUNT (Super compacted)
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Account",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Account",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "soudwipmandal@gmail.com",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Billing History View
        if (isHistoryOpen) {
            BillingHistoryDialog(
                historyList = billingHistory,
                onDismissRequest = { isHistoryOpen = false }
            )
        }
        }
    }
}

@Composable
fun LanguageSelectorPill(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = modifier
            .height(30.dp)
            .testTag("language_selector_pill")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp)
        ) {
            AppLanguage.entries.forEachIndexed { index, lang ->
                if (index > 0) {
                    Spacer(modifier = Modifier.width(2.dp))
                }
                LanguagePillItem(
                    label = lang.shortLabel,
                    isSelected = currentLanguage == lang,
                    onClick = { onLanguageSelected(lang) }
                )
            }
        }
    }
}

@Composable
private fun LanguagePillItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        label = "pill_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "pill_text"
    )

    Box(
        modifier = Modifier
            .height(26.dp)
            .widthIn(min = 28.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}
