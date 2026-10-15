package com.rainweaver.cogscalc.domain

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Core costing engine.
 *   ingredient cost = quantity used × cost per compatible base unit
 *   batch cost      = Σ ingredient costs + additional costs
 *   cost per portion= batch cost ÷ sellable portions (null if portions invalid)
 */
object CostEngine {

    /** The line's quantity expressed in the ingredient's base unit. */
    fun quantityInBase(line: RecipeLineInput): BigDecimal {
        return if (UnitConverter.compatible(line.unit, line.ingredientBaseUnit)) {
            UnitConverter.convert(line.quantity, line.unit, line.ingredientBaseUnit)
        } else {
            val factor = line.conversionFactor
                ?: throw IllegalArgumentException(
                    "A conversion factor is required to use ${line.unit.label} for \"${line.ingredientName}\" " +
                        "(priced per ${line.ingredientBaseUnit.label})."
                )
            require(factor > BigDecimal.ZERO) { "Conversion factor must be greater than zero" }
            line.quantity.multiply(factor)
        }
    }

    fun lineCost(line: RecipeLineInput): Money {
        val qtyInBase = quantityInBase(line)
        return Money(qtyInBase.multiply(line.ingredientCostPerBase.value))
    }

    fun batchCost(lines: List<RecipeLineInput>, additionalCosts: List<Money> = emptyList()): Money {
        var total = Money.ZERO
        lines.forEach { total += lineCost(it) }
        additionalCosts.forEach { total += it }
        return total
    }

    /** Cost per sellable portion, or null when portions is zero/invalid (never misleading). */
    fun costPerPortion(batch: Money, portions: Int): Money? {
        if (portions <= 0) return null
        return batch.divide(BigDecimal.valueOf(portions.toLong()), 6, RoundingMode.HALF_UP)
    }
}
