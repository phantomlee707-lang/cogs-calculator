package com.rainweaver.cogscalc.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rainweaver.cogscalc.data.local.*
import com.rainweaver.cogscalc.domain.*
import com.rainweaver.cogscalc.ui.*
import java.math.BigDecimal

private data class DraftLine(val key: Long, val ingredientId: Long, val qty: String, val unit: String, val factor: String)
private data class DraftCost(val label: String, val amount: String)
private var lineKeySeq = 0L

/**
 * Parse a user-typed decimal tolerantly (strips thousands separators, spaces,
 * currency letters) into a canonical BigDecimal string, so raw text like
 * "10,000" or "Rp 5000" can never be stored verbatim and corrupt the DB on read.
 */
private fun cleanDecimal(text: String, fallback: String): String {
    val cleaned = text.filter { it.isDigit() || it == '.' || it == '-' }
    return cleaned.toBigDecimalOrNull()?.toPlainString() ?: fallback
}

@Composable
fun RecipesScreen(vm: CogsViewModel, onOpen: (Long) -> Unit, onEdit: (Long) -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val cur = s.settings.currency; val dec = s.settings.decimals
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
        item {
            Button(onClick = { onEdit(0L) }, modifier = Modifier.fillMaxWidth().padding(16.dp)) { Text("+ New recipe") }
        }
        if (s.recipes.isEmpty()) item { EmptyState("No recipes yet. Add ingredients first, then build a recipe.") }
        items(s.recipes) { r ->
            val cost = s.costs[r.recipe.id]
            ListItem(
                headlineContent = { Text(r.recipe.name) },
                supportingContent = {
                    val per = cost?.costPerPortion
                    Text("${r.recipe.portions} portions" + (per?.let { "  ·  ${it.format(cur, dec)} / portion" } ?: ""))
                },
                modifier = Modifier.clickable { onOpen(r.recipe.id) },
                trailingContent = { TextButton(onClick = { vm.deleteRecipe(r.recipe.id) }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun RecipeEditorScreen(vm: CogsViewModel, existingId: Long, onDone: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val existing = s.recipes.firstOrNull { it.recipe.id == existingId }
    var name by remember(existingId) { mutableStateOf(existing?.recipe?.name ?: "") }
    var category by remember(existingId) { mutableStateOf(existing?.recipe?.category ?: "") }
    var portions by remember(existingId) { mutableStateOf((existing?.recipe?.portions ?: 1).toString()) }
    var wastage by remember(existingId) { mutableStateOf(existing?.recipe?.wastagePercent ?: "0") }
    var notes by remember(existingId) { mutableStateOf(existing?.recipe?.notes ?: "") }
    var lines by remember(existingId) {
        mutableStateOf(existing?.lines?.map { DraftLine(lineKeySeq++, it.ingredientId, it.quantity, it.unit, it.conversionFactor ?: "") }
            ?: listOf(DraftLine(lineKeySeq++, s.ingredients.firstOrNull()?.id ?: 0, "1", "G", "")))
    }
    var costs by remember(existingId) {
        mutableStateOf(existing?.additionalCosts?.map { DraftCost(it.label, it.amount) } ?: emptyList<DraftCost>())
    }

    fun save() {
        val lineEntities = lines.filter { it.ingredientId != 0L }.map { dl ->
            val ing = s.ingredients.firstOrNull { it.id == dl.ingredientId }
            RecipeLineEntity(0, existingId, dl.ingredientId, ing?.name ?: "",
                cleanDecimal(dl.qty, "1"), dl.unit, dl.factor.takeIf { it.isNotBlank() }?.let { cleanDecimal(it, "1") })
        }
        val costEntities = costs.filter { it.label.isNotBlank() }.map { AdditionalCostEntity(0, existingId, it.label, cleanDecimal(it.amount, "0")) }
        vm.saveRecipe(
            RecipeEntity(existingId, name, category, portions.toIntOrNull() ?: 1, cleanDecimal(wastage, "0"), notes, "",
                existing?.recipe?.archived ?: false, existing?.recipe?.createdAt ?: 0, 0),
            lineEntities, costEntities,
        )
        onDone()
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp)) {
        item {
            SectionCard("Recipe") {
                Field("Name *", name) { name = it }
                Spacer(Modifier.height(8.dp))
                Field("Category (optional)", category) { category = it }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field("Sellable portions *", portions, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f)) { portions = it }
                    Field("Wastage % (optional)", wastage, keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f)) { wastage = it }
                }
                Spacer(Modifier.height(8.dp))
                Field("Notes (optional)", notes) { notes = it }
            }
        }
        item { SectionCard("Ingredients") {} }
        itemsIndexed(lines) { i, dl ->
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                Column(Modifier.padding(12.dp)) {
                    IngredientPicker(s.ingredients, dl.ingredientId) { sel -> lines = lines.mapIndexed { j, l -> if (j == i) l.copy(ingredientId = sel) else l } }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Field("Qty", dl.qty, keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f)) { v -> lines = lines.mapIndexed { j, l -> if (j == i) l.copy(qty = v) else l } }
                        UnitDropdown("Unit", MeasureUnit.valueOf(dl.unit), Modifier.weight(1f)) { u -> lines = lines.mapIndexed { j, l -> if (j == i) l.copy(unit = u.name) else l } }
                    }
                    Spacer(Modifier.height(6.dp))
                    Field("Conversion factor (base per unit, if units differ)", dl.factor, keyboardType = KeyboardType.Decimal) { v -> lines = lines.mapIndexed { j, l -> if (j == i) l.copy(factor = v) else l } }
                    TextButton(onClick = { lines = lines.filterIndexed { j, _ -> j != i } }) { Text("Remove line", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
        item {
            TextButton(onClick = { lines = lines + DraftLine(lineKeySeq++, s.ingredients.firstOrNull()?.id ?: 0, "1", "G", "") },
                modifier = Modifier.padding(horizontal = 16.dp)) { Text("+ Add ingredient line") }
        }
        item { SectionCard("Additional costs (optional)") {} }
        itemsIndexed(costs) { i, dc ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Field("Label", dc.label, modifier = Modifier.weight(1.4f)) { v -> costs = costs.mapIndexed { j, c -> if (j == i) c.copy(label = v) else c } }
                Field("Amount", dc.amount, keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f)) { v -> costs = costs.mapIndexed { j, c -> if (j == i) c.copy(amount = v) else c } }
                TextButton(onClick = { costs = costs.filterIndexed { j, _ -> j != i } }) { Text("✕") }
            }
        }
        item {
            TextButton(onClick = { costs = costs + DraftCost("", "") }, modifier = Modifier.padding(horizontal = 16.dp)) { Text("+ Add cost") }
        }
        item {
            Button(onClick = { save() }, modifier = Modifier.fillMaxWidth().padding(16.dp), enabled = name.isNotBlank()) { Text("Save recipe") }
        }
    }
}

@Composable
private fun IngredientPicker(ingredients: List<com.rainweaver.cogscalc.data.Ingredient>, selectedId: Long, onSelect: (Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val name = ingredients.firstOrNull { it.id == selectedId }?.name ?: "Select ingredient"
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(name) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ingredients.forEach { ing -> DropdownMenuItem(text = { Text(ing.name) }, onClick = { onSelect(ing.id); expanded = false }) }
        }
    }
}

@Composable
fun RecipeDetailScreen(vm: CogsViewModel, id: Long, onBack: () -> Unit, onEdit: (Long) -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val recipe = s.recipes.firstOrNull { it.recipe.id == id }
    val cost = s.costs[id]
    val snaps by vm.snapshots(id).collectAsStateWithLifecycle(initialValue = emptyList())
    val cur = s.settings.currency; val dec = s.settings.decimals

    if (recipe == null || cost == null) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp)) {
            item {
                SectionCard(recipe.recipe.name) {
                    Text("${recipe.recipe.portions} portions", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    cost.lines.forEach { l ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${l.ingredientName} (${l.quantity.stripTrailingZeros().toPlainString()} ${l.unit.label})", Modifier.weight(1f))
                            Text(l.lineCost.format(cur, dec))
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                    TotalRow("Ingredient subtotal", cost.ingredientSubtotal.format(cur, dec))
                    if (cost.wastageAmount.value.signum() != 0) TotalRow("Wastage", cost.wastageAmount.format(cur, dec))
                    if (cost.additionalTotal.value.signum() != 0) TotalRow("Additional", cost.additionalTotal.format(cur, dec))
                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                    TotalRow("Total batch cost", cost.batchCost.format(cur, dec), bold = true)
                    TotalRow("Cost per portion", cost.costPerPortion?.format(cur, dec) ?: "—", bold = true)
                }
            }
            item {
                val base = cost.costPerPortion ?: cost.batchCost
                val markup = s.settings.defaultMarkup.toBigDecimalOrNull()
                val margin = s.settings.defaultMargin.toBigDecimalOrNull()
                val step = s.settings.priceStep.toBigDecimalOrNull() ?: BigDecimal.ZERO
                SectionCard("Suggested selling price") {
                    markup?.let { TotalRow("Markup ${it.stripTrailingZeros().toPlainString()}%", Pricing.roundToStep(Pricing.markupPrice(base, it), step).format(cur, dec)) }
                    margin?.let { m -> Pricing.marginPrice(base, m)?.let { TotalRow("Gross margin ${m.stripTrailingZeros().toPlainString()}%", Pricing.roundToStep(it, step).format(cur, dec)) } }
                    Text("Suggestion only — operating expenses may still apply.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
            if (snaps.isNotEmpty()) {
                item { SectionCard("Saved snapshots") {} }
                items(snaps) { sw ->
                    val d = java.util.Date(sw.snapshot.createdAt)
                    Text("${java.text.SimpleDateFormat("MMM d, yyyy HH:mm", java.util.Locale.US).format(d)}  ·  batch ${Money.of(BigDecimal(sw.snapshot.batchCost)).format(sw.snapshot.currency, dec)}",
                        Modifier.padding(horizontal = 16.dp, vertical = 4.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Surface(tonalElevation = 3.dp) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.recipePdf(id) { sharePdf(context, it) } }, modifier = Modifier.weight(1f)) { Text("Export PDF") }
                    OutlinedButton(onClick = { vm.saveSnapshot(id) }, modifier = Modifier.weight(1f)) { Text("Save snapshot") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onEdit(id) }, modifier = Modifier.weight(1f)) { Text("Edit") }
                    TextButton(onClick = { vm.deleteRecipe(id); onBack() }, modifier = Modifier.weight(1f)) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}
