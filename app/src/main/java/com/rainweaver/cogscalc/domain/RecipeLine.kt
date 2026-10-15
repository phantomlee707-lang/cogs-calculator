package com.rainweaver.cogscalc.domain

import java.math.BigDecimal

/**
 * One ingredient line in a recipe. The ingredient's cost is captured per base
 * unit; [unit] is what the recipe uses. If [unit] is a different [UnitType] than
 * [ingredientBaseUnit] (e.g. cups of flour priced per gram), [conversionFactor]
 * (base units per one [unit]) must be supplied explicitly.
 */
data class RecipeLineInput(
    val ingredientId: Long,
    val ingredientName: String,
    val ingredientBaseUnit: MeasureUnit,
    val ingredientCostPerBase: Money,
    val quantity: BigDecimal,
    val unit: MeasureUnit,
    val conversionFactor: BigDecimal? = null,
)
