package com.rainweaver.cogscalc.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rainweaver.cogscalc.data.CogsRepository
import com.rainweaver.cogscalc.data.Ingredient
import com.rainweaver.cogscalc.data.local.*
import com.rainweaver.cogscalc.data.toDomain
import com.rainweaver.cogscalc.domain.RecipeCost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.math.BigDecimal

data class UiState(
    val loaded: Boolean = false,
    val ingredients: List<Ingredient> = emptyList(),
    val recipes: List<RecipeWithDetails> = emptyList(),
    val costs: Map<Long, RecipeCost> = emptyMap(),
    val settings: AppSettingsEntity = AppSettingsEntity(),
    val message: String? = null,
)

class CogsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = CogsRepository(CogsDatabase.get(app))
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init { reload() }

    fun reload() = viewModelScope.launch {
        val ingEntities = repo.allIngredients()
        val ingMap = ingEntities.associateBy { it.id }
        val recipes = repo.observeRecipes().first()
        val costs = recipes.associate { it.recipe.id to repo.computeCostWith(it, ingMap) }
        val settings = repo.observeSettings().first() ?: AppSettingsEntity()
        _state.update {
            it.copy(loaded = true, ingredients = ingEntities.mapNotNull { e -> runCatching { e.toDomain() }.getOrNull() },
                recipes = recipes, costs = costs, settings = settings)
        }
    }

    // ---- ingredients ----
    fun saveIngredient(e: IngredientEntity) = viewModelScope.launch {
        val qty = e.purchaseQty.toBigDecimalOrNull()
        if (e.name.isBlank()) { msg("Ingredient name is required"); return@launch }
        if (qty == null || qty <= BigDecimal.ZERO) { msg("Purchase quantity must be greater than zero"); return@launch }
        if (e.purchasePrice.toBigDecimalOrNull()?.let { it < BigDecimal.ZERO } != false) { msg("Purchase price is invalid"); return@launch }
        repo.saveIngredient(e); reload(); msg("Ingredient saved")
    }
    fun deleteIngredient(id: Long) = viewModelScope.launch { repo.deleteIngredient(id); reload(); msg("Ingredient deleted") }

    // ---- recipes ----
    fun saveRecipe(r: RecipeEntity, lines: List<RecipeLineEntity>, additional: List<AdditionalCostEntity>) = viewModelScope.launch {
        if (r.name.isBlank()) { msg("Recipe name is required"); return@launch }
        if (r.portions <= 0) { msg("Portions must be greater than zero"); return@launch }
        repo.saveRecipe(r, lines, additional); reload(); msg("Recipe saved")
    }
    fun deleteRecipe(id: Long) = viewModelScope.launch { repo.deleteRecipe(id); reload(); msg("Recipe deleted") }

    // ---- snapshot ----
    fun saveSnapshot(recipeId: Long) = viewModelScope.launch {
        val cost = _state.value.costs[recipeId]
            ?: repo.recipe(recipeId)?.let { repo.computeCost(it) }
            ?: run { msg("Recipe not found"); return@launch }
        repo.saveSnapshot(recipeId, cost, _state.value.settings.currency)
        msg("Cost snapshot saved")
    }

    // ---- settings ----
    fun saveSettings(s: AppSettingsEntity) = viewModelScope.launch { repo.saveSettings(s); reload(); msg("Settings saved") }

    fun snapshots(recipeId: Long) = repo.observeSnapshots(recipeId)

    // ---- PDF ----
    fun recipePdf(recipeId: Long, onFile: (File) -> Unit) = viewModelScope.launch {
        val r = repo.recipe(recipeId) ?: return@launch
        val cost = _state.value.costs[recipeId] ?: repo.computeCost(r)
        val settings = _state.value.settings
        val file = CostReportPdf.writePdf(getApplication(), r.recipe, cost, settings)
        onFile(file)
    }

    fun msg(m: String) = _state.update { it.copy(message = m) }
    fun consumeMessage() = _state.update { it.copy(message = null) }
}
