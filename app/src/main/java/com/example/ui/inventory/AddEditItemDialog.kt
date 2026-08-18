package com.example.ui.inventory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InventoryItem
import com.example.ui.theme.Cyan400
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.Red500
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
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditItemDialog(
    itemToEdit: InventoryItem? = null,
    initialBarcode: String? = null,
    initialCategory: String? = null,
    onDismiss: () -> Unit,
    onSave: (InventoryItem) -> Unit,
    onDelete: ((InventoryItem) -> Unit)? = null,
    showCars: Boolean = true,
    showBikes: Boolean = true,
    showSpareParts: Boolean = true
) {
    val strings = currentStrings()
    val isEditing = itemToEdit != null

    val categories = remember(showCars, showBikes, showSpareParts) {
        buildList {
            if (showSpareParts) add("Spare Parts")
            if (showCars) add("Cars")
            if (showBikes) add("Bikes")
            if (isEmpty()) add("Spare Parts") // fallback
        }
    }

    val initialCat = remember(categories, itemToEdit, initialCategory) {
        val preferred = itemToEdit?.category ?: initialCategory ?: "Spare Parts"
        if (preferred in categories) preferred else categories.first()
    }

    var name by remember { mutableStateOf(itemToEdit?.name ?: "") }
    var category by remember { mutableStateOf(initialCat) }
    var subcategory by remember {
        mutableStateOf(
            itemToEdit?.subcategory ?: when (initialCat) {
                "Cars" -> "SUV"
                "Bikes" -> "Commuter 100-125cc"
                else -> "Engine"
            }
        )
    }
    var sku by remember { mutableStateOf(itemToEdit?.sku ?: generateRandomSku(initialCat)) }
    var barcode by remember { mutableStateOf(itemToEdit?.barcode ?: initialBarcode ?: generateRandomBarcode()) }
    var fitment by remember { mutableStateOf(itemToEdit?.fitment ?: "") }
    var quantityText by remember { mutableStateOf(itemToEdit?.quantity?.toString() ?: "10") }
    var minStockThresholdText by remember { mutableStateOf(itemToEdit?.minStockThreshold?.toString() ?: "5") }
    var costPriceText by remember {
        mutableStateOf(
            itemToEdit?.costPrice?.toString() ?: when (initialCat) {
                "Cars" -> "950000"
                "Bikes" -> "72000"
                else -> "350"
            }
        )
    }
    var sellingPriceText by remember {
        mutableStateOf(
            itemToEdit?.sellingPrice?.toString() ?: when (initialCat) {
                "Cars" -> "1180000"
                "Bikes" -> "88000"
                else -> "499"
            }
        )
    }
    var locationRack by remember { mutableStateOf(itemToEdit?.locationRack ?: "Rack A-01 / Bin 12") }
    var supplier by remember { mutableStateOf(itemToEdit?.supplier ?: "OEM Genuine Spares") }
    var unit by remember { mutableStateOf(itemToEdit?.unit ?: "Units") }
    var description by remember { mutableStateOf(itemToEdit?.description ?: "") }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(categories) {
        if (category !in categories) {
            category = categories.first()
        }
    }

    val subcategoriesForSelected = when (category) {
        "Cars" -> listOf("SUV", "Hatchback", "Sedan", "EV / Electric", "4x4 Off-Road", "Other")
        "Bikes" -> listOf("Commuter 100-125cc", "Cruiser 350cc", "Scooter 110-125cc", "Street Naked", "Adventure", "Other")
        else -> listOf("Engine", "Brakes", "Lubricants / 4T Oil", "Tyres & Tubes", "Electrical", "Chain & Sprocket", "Filters & Body", "Suspension", "Other")
    }

    val categoryLabelMap = mapOf(
        "Spare Parts" to strings.catSpareParts,
        "Cars" to strings.catCars,
        "Bikes" to strings.catBikes
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("add_edit_item_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) strings.editProduct else strings.addNewProduct,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = strings.cancel, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorText != null) {
                    Text(
                        text = errorText ?: "",
                        color = Red500,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // 1. Category Selector
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = categoryLabelMap[category] ?: category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(strings.selectCategory) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = Saffron500,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(categoryLabelMap[cat] ?: cat, color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    category = cat
                                    subcategory = when (cat) {
                                        "Cars" -> "SUV"
                                        "Bikes" -> "Commuter 100-125cc"
                                        else -> "Engine"
                                    }
                                    if (!isEditing) {
                                        sku = generateRandomSku(cat)
                                        if (cat == "Cars") {
                                            costPriceText = "950000"
                                            sellingPriceText = "1180000"
                                        } else if (cat == "Bikes") {
                                            costPriceText = "72000"
                                            sellingPriceText = "88000"
                                        } else {
                                            costPriceText = "350"
                                            sellingPriceText = "499"
                                        }
                                    }
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Subcategory selector
                Text(strings.subcategory, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    subcategoriesForSelected.forEach { sub ->
                        val isSelected = subcategory == sub || (sub == "Other" && !subcategoriesForSelected.dropLast(1).contains(subcategory))
                        Box(
                            modifier = Modifier
                                .background(if (isSelected) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .border(1.dp, if (isSelected) (if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black) else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { subcategory = sub }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = sub,
                                color = if (isSelected) (if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black) else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                if (subcategory == "Other" || !subcategoriesForSelected.dropLast(1).contains(subcategory)) {
                    OutlinedTextField(
                        value = if (subcategory == "Other") "" else subcategory,
                        onValueChange = { subcategory = it.ifBlank { "Other" } },
                        label = { Text("Custom Subcategory Name") },
                        placeholder = { Text("e.g., Accessories, Lighting, Transmission") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = Saffron500,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }

                // GST / HSN preview badge
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${strings.gstRate}: ${IndianFormatUtils.getGstRateForCategory(category)}%",
                            color = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${strings.hsnCode}: ${IndianFormatUtils.getHsnCodeForCategory(category)}",
                            color = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // 2. Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.enterName) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = Saffron500,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // 3. Vehicle Fitment / Compatibility
                OutlinedTextField(
                    value = fitment,
                    onValueChange = { fitment = it },
                    label = { Text(strings.enterFitment) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = Saffron500,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // 4. SKU & Barcode Generation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text(strings.enterSku) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = Saffron500,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text(strings.enterBarcode) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = Saffron500,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }

                // 5. Quantity & Threshold
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it.filter { c -> c.isDigit() } },
                        label = { Text(strings.enterQty) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = Saffron500,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    OutlinedTextField(
                        value = minStockThresholdText,
                        onValueChange = { minStockThresholdText = it.filter { c -> c.isDigit() } },
                        label = { Text(strings.enterThreshold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = Saffron500,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }

                // 6. Pricing in INR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = costPriceText,
                        onValueChange = { costPriceText = it },
                        label = { Text(strings.enterCostPrice) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = Saffron500,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    OutlinedTextField(
                        value = sellingPriceText,
                        onValueChange = { sellingPriceText = it },
                        label = { Text(strings.enterSellingPrice) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = Saffron500,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }

                // 7. Location Rack & Supplier
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = locationRack,
                        onValueChange = { locationRack = it },
                        label = { Text(strings.enterLocation) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = Saffron500,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    OutlinedTextField(
                        value = supplier,
                        onValueChange = { supplier = it },
                        label = { Text("Supplier") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = Saffron500,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }

                // 8. Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(strings.enterNotes) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = Saffron500,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                if (isEditing && onDelete != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Red500),
                        border = BorderStroke(1.dp, Red500),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.deleteItem)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorText = "Please enter item name."
                        return@Button
                    }
                    val qty = quantityText.toIntOrNull() ?: 0
                    val minThreshold = minStockThresholdText.toIntOrNull() ?: 5
                    val costPrice = costPriceText.toDoubleOrNull() ?: 0.0
                    val sellingPrice = sellingPriceText.toDoubleOrNull() ?: 0.0

                    val item = InventoryItem(
                        id = itemToEdit?.id ?: 0L,
                        sku = sku.ifBlank { generateRandomSku(category) },
                        barcode = barcode.ifBlank { generateRandomBarcode() },
                        name = name.trim(),
                        category = category,
                        subcategory = subcategory,
                        fitment = fitment.trim(),
                        quantity = qty,
                        minStockThreshold = minThreshold,
                        costPrice = costPrice,
                        sellingPrice = sellingPrice,
                        locationRack = locationRack.ifBlank { "Unassigned" },
                        supplier = supplier.ifBlank { "Direct" },
                        description = description.trim(),
                        unit = unit,
                        lastRestockedTimestamp = System.currentTimeMillis()
                    )
                    onSave(item)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.White else Color.Black,
                    contentColor = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.Black else Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (isEditing) strings.save else strings.add,
                    color = if (MaterialTheme.colorScheme.surface == DarkSurface) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )

    if (showDeleteConfirm && itemToEdit != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(strings.deleteConfirmTitle, color = MaterialTheme.colorScheme.onSurface) },
            text = { Text(strings.deleteConfirmMessage, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(itemToEdit)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red500)
                ) {
                    Text(strings.confirmDelete, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(strings.cancel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

private fun generateRandomSku(category: String): String {
    val prefix = when (category) {
        "Cars" -> "CAR-IND"
        "Bikes" -> "BIK-IND"
        else -> "SKU-IND"
    }
    val randomNum = Random.nextInt(1000, 9999)
    return "$prefix-$randomNum"
}

private fun generateRandomBarcode(): String {
    val randomSuffix = Random.nextLong(1000000000L, 9999999999L)
    return "890$randomSuffix"
}
