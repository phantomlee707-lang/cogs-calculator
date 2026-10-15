package com.rainweaver.cogscalc.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rainweaver.cogscalc.data.local.AppSettingsEntity
import com.rainweaver.cogscalc.data.local.IngredientEntity
import com.rainweaver.cogscalc.domain.MeasureUnit
import com.rainweaver.cogscalc.ui.*

@Composable
fun IngredientsScreen(vm: CogsViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<IngredientEntity?>(null) }
    var show by remember { mutableStateOf(false) }
    val dec = s.settings.decimals; val cur = s.settings.currency

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
        item {
            Button(onClick = { editing = IngredientEntity(name = "", purchaseQty = "1", purchaseUnit = "KG", purchasePrice = "0", baseUnit = "G"); show = true },
                modifier = Modifier.fillMaxWidth().padding(16.dp)) { Text("+ Add ingredient") }
        }
        if (s.ingredients.isEmpty()) item { EmptyState("No ingredients yet.") }
        items(s.ingredients) { ing ->
            ListItem(
                headlineContent = { Text(ing.name) },
                supportingContent = { Text("${ing.costPerBase.format(cur, dec)} per ${ing.baseUnit.label}  ·  buy ${ing.purchaseQty} ${ing.purchaseUnit.label}") },
                trailingContent = {
                    Row {
                        TextButton(onClick = { editing = ing.toEntity(); show = true }) { Text("Edit") }
                        TextButton(onClick = { vm.deleteIngredient(ing.id) }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    }
                },
            )
            HorizontalDivider()
        }
    }
    if (show && editing != null) {
        IngredientDialog(editing!!, onDismiss = { show = false }, onSave = { vm.saveIngredient(it); show = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IngredientDialog(e0: IngredientEntity, onDismiss: () -> Unit, onSave: (IngredientEntity) -> Unit) {
    var e by remember { mutableStateOf(e0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (e0.id == 0L) "New ingredient" else "Edit ingredient") },
        text = {
            Column {
                Field("Name *", e.name) { e = e.copy(name = it) }
                Spacer(Modifier.height(8.dp))
                Field("Category (optional)", e.category) { e = e.copy(category = it) }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field("Purchase qty", e.purchaseQty, keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f)) { e = e.copy(purchaseQty = it) }
                    UnitDropdown("Purchase unit", MeasureUnit.valueOf(e.purchaseUnit), Modifier.weight(1f)) { e = e.copy(purchaseUnit = it.name) }
                }
                Spacer(Modifier.height(8.dp))
                Field("Purchase price", e.purchasePrice, keyboardType = KeyboardType.Decimal) { e = e.copy(purchasePrice = it) }
                Spacer(Modifier.height(8.dp))
                UnitDropdown("Priced per (base unit)", MeasureUnit.valueOf(e.baseUnit)) { e = e.copy(baseUnit = it.name) }
                Spacer(Modifier.height(8.dp))
                Field("Supplier (optional)", e.supplier) { e = e.copy(supplier = it) }
            }
        },
        confirmButton = { Button(onClick = { onSave(e) }, enabled = e.name.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitDropdown(label: String, selected: MeasureUnit, modifier: Modifier = Modifier, onSelect: (MeasureUnit) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected.label, onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MeasureUnit.entries.forEach { u -> DropdownMenuItem(text = { Text("${u.label}  (${u.type})") }, onClick = { onSelect(u); expanded = false }) }
        }
    }
}

@Composable
fun SettingsScreen(vm: CogsViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var st by remember(s.settings) { mutableStateOf(s.settings) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let { vm.msg(if (Backup.export(context, it)) "Backup saved" else "Backup failed") }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { if (Backup.restore(context, it)) { vm.reload(); vm.msg("Restored — reopen app if data looks stale") } else vm.msg("Restore failed") }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            SectionCard("Business") {
                Field("Business name", st.businessName) { st = st.copy(businessName = it) }
                Spacer(Modifier.height(8.dp))
                Field("Currency symbol", st.currency) { st = st.copy(currency = it) }
                Spacer(Modifier.height(8.dp))
                Field("Decimals", st.decimals.toString(), keyboardType = KeyboardType.Number) { st = st.copy(decimals = it.toIntOrNull() ?: 0) }
            }
        }
        item {
            SectionCard("Pricing defaults") {
                Field("Default markup %", st.defaultMarkup, keyboardType = KeyboardType.Decimal) { st = st.copy(defaultMarkup = it) }
                Spacer(Modifier.height(8.dp))
                Field("Default gross margin %", st.defaultMargin, keyboardType = KeyboardType.Decimal) { st = st.copy(defaultMargin = it) }
                Spacer(Modifier.height(8.dp))
                Field("Round price to step", st.priceStep, keyboardType = KeyboardType.Decimal) { st = st.copy(priceStep = it) }
                Spacer(Modifier.height(12.dp))
                Button(onClick = { vm.saveSettings(st) }, modifier = Modifier.fillMaxWidth()) { Text("Save settings") }
            }
        }
        item {
            SectionCard("Backup & restore") {
                Text("Export or restore all data locally. Backups are never uploaded.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { exportLauncher.launch("cogs-backup.db") }, modifier = Modifier.weight(1f)) { Text("Backup") }
                    Button(onClick = { importLauncher.launch(arrayOf("application/octet-stream", "*/*")) }, modifier = Modifier.weight(1f)) { Text("Restore") }
                }
            }
        }
    }
}

private fun com.rainweaver.cogscalc.data.Ingredient.toEntity(): IngredientEntity = IngredientEntity(
    id, name, category, purchaseQty.toPlainString(), purchaseUnit.name, purchasePrice.value.toPlainString(),
    baseUnit.name, supplier, notes, archived, 0,
)
