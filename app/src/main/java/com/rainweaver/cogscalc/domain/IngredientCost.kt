package com.rainweaver.cogscalc.domain

import java.math.BigDecimal
import java.math.RoundingMode

/** Derives an ingredient's cost per base unit from its purchase price & quantity. */
object IngredientCost {

    /**
     * costPerBase = purchasePrice / (purchaseQuantity converted to baseUnit).
     * e.g. Rp16,000 for 1 kg flour → Rp16 per gram.
     */
    fun costPerBase(purchasePrice: Money, purchaseQuantity: BigDecimal, purchaseUnit: MeasureUnit, baseUnit: MeasureUnit): Money {
        require(purchaseQuantity > BigDecimal.ZERO) { "Purchase quantity must be greater than zero" }
        val qtyInBase = UnitConverter.convert(purchaseQuantity, purchaseUnit, baseUnit)
        require(qtyInBase > BigDecimal.ZERO) { "Converted purchase quantity must be greater than zero" }
        return Money(purchasePrice.value.divide(qtyInBase, 12, RoundingMode.HALF_UP))
    }
}
