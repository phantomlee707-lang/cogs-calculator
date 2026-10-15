package com.rainweaver.cogscalc.data

import com.rainweaver.cogscalc.data.local.*
import com.rainweaver.cogscalc.domain.*
import java.math.BigDecimal

/** Domain view of an ingredient with its derived cost per base unit. */
data class Ingredient(
    val id: Long,
    val name: String,
    val category: String,
    val purchaseQty: BigDecimal,
    val purchaseUnit: MeasureUnit,
    val purchasePrice: Money,
    val baseUnit: MeasureUnit,
    val costPerBase: Money,
    val supplier: String,
    val notes: String,
    val archived: Boolean,
)

class CogsRepository(private val db: CogsDatabase) {

    private val ingredientDao = db.ingredientDao()
    private val recipeDao = db.recipeDao()
    private val snapshotDao = db.snapshotDao()
    private val settingsDao = db.settingsDao()

    // ---- ingredients ----
    fun observeIngredients() = ingredientDao.observeActive()
    fun searchIngredients(q: String) = ingredientDao.search(q)
    suspend fun allIngredients() = ingredientDao.getAll()
    suspend fun saveIngredient(e: IngredientEntity) =
        if (e.id == 0L) ingredientDao.insert(e.copy(updatedAt = now())) else { ingredientDao.update(e.copy(updatedAt = now())); e.id }
    suspend fun deleteIngredient(id: Long) = ingredientDao.deleteById(id)
    suspend fun archiveIngredient(id: Long, archived: Boolean) = ingredientDao.setArchived(id, archived)

    // ---- recipes ----
    fun observeRecipes() = recipeDao.observeAll()
    fun observeRecipe(id: Long) = recipeDao.observeById(id)
    suspend fun recipe(id: Long) = recipeDao.getById(id)
    suspend fun saveRecipe(r: RecipeEntity, lines: List<RecipeLineEntity>, additional: List<AdditionalCostEntity>): Long {
        val stamped = r.copy(updatedAt = now(), createdAt = if (r.id == 0L) now() else r.createdAt)
        return recipeDao.saveRecipe(stamped, lines, additional)
    }
    suspend fun deleteRecipe(id: Long) = recipeDao.deleteById(id)

    // ---- snapshots ----
    fun observeSnapshots(recipeId: Long) = snapshotDao.observeByRecipe(recipeId)
    suspend fun snapshot(id: Long) = snapshotDao.getById(id)

    // ---- settings ----
    fun observeSettings() = settingsDao.observe()
    suspend fun settings() = settingsDao.get() ?: AppSettingsEntity().also { settingsDao.upsert(it) }
    suspend fun saveSettings(s: AppSettingsEntity) = settingsDao.upsert(s)

    // ---- cost computation ----
    /** Current cost of a recipe using CURRENT ingredient prices. */
    suspend fun computeCost(recipe: RecipeWithDetails): RecipeCost {
        val ingredients = allIngredients().associateBy { it.id }
        return computeCostWith(recipe, ingredients)
    }

    fun computeCostWith(recipe: RecipeWithDetails, ingredients: Map<Long, IngredientEntity>): RecipeCost {
        val lines = recipe.lines.mapNotNull { rl ->
            val ing = ingredients[rl.ingredientId] ?: return@mapNotNull null
            val base = MeasureUnit.valueOf(ing.baseUnit)
            val costPerBase = costPerBase(ing)
            val input = RecipeLineInput(
                ingredientId = ing.id, ingredientName = ing.name, ingredientBaseUnit = base,
                ingredientCostPerBase = costPerBase, quantity = BigDecimal(rl.quantity),
                unit = MeasureUnit.valueOf(rl.unit), conversionFactor = rl.conversionFactor?.let { BigDecimal(it) },
            )
            // Skip lines that can't be costed (missing cross-type factor) rather than crash.
            val lineCost = runCatching { CostEngine.lineCost(input) }.getOrNull() ?: return@mapNotNull null
            CostLine(ing.name, BigDecimal(rl.quantity), MeasureUnit.valueOf(rl.unit), costPerBase, lineCost)
        }
        val subtotal = lines.fold(Money.ZERO) { acc, l -> acc + l.lineCost }
        val wastageAmount = subtotal * (BigDecimal(recipe.recipe.wastagePercent).divide(BigDecimal(100)))
        val additionalTotal = recipe.additionalCosts.fold(Money.ZERO) { acc, ac -> acc + Money(BigDecimal(ac.amount)) }
        val batch = subtotal + wastageAmount + additionalTotal
        val perPortion = CostEngine.costPerPortion(batch, recipe.recipe.portions)
        return RecipeCost(lines, subtotal, wastageAmount, additionalTotal, batch, recipe.recipe.portions, perPortion)
    }

    /** Freeze the current cost as an immutable historical snapshot. */
    suspend fun saveSnapshot(recipeId: Long, cost: RecipeCost, currency: String): Long {
        val s = CostSnapshotEntity(
            recipeId = recipeId, createdAt = now(), portions = cost.portions,
            ingredientSubtotal = cost.ingredientSubtotal.value.toPlainString(),
            additionalTotal = cost.additionalTotal.value.toPlainString(),
            wastageAmount = cost.wastageAmount.value.toPlainString(),
            batchCost = cost.batchCost.value.toPlainString(),
            costPerPortion = cost.costPerPortion?.value?.toPlainString(),
            currency = currency,
        )
        val lines = cost.lines.map {
            SnapshotLineEntity(0, 0, it.ingredientName, it.quantity.toPlainString(), it.unit.name,
                it.costPerBase.value.toPlainString(), it.lineCost.value.toPlainString())
        }
        return snapshotDao.save(s, lines)
    }

    companion object {
        fun costPerBase(ing: IngredientEntity): Money =
            IngredientCost.costPerBase(
                Money(BigDecimal(ing.purchasePrice)), BigDecimal(ing.purchaseQty),
                MeasureUnit.valueOf(ing.purchaseUnit), MeasureUnit.valueOf(ing.baseUnit),
            )
        fun now() = System.currentTimeMillis()
    }
}

fun IngredientEntity.toDomain(): Ingredient = Ingredient(
    id, name, category, BigDecimal(purchaseQty), MeasureUnit.valueOf(purchaseUnit),
    Money(BigDecimal(purchasePrice)), MeasureUnit.valueOf(baseUnit),
    CogsRepository.costPerBase(this), supplier, notes, archived,
)
