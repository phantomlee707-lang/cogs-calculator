package com.rainweaver.cogscalc.domain

import java.math.BigDecimal

/** One line of a cost breakdown (current or snapshot). */
data class CostLine(
    val ingredientName: String,
    val quantity: BigDecimal,
    val unit: MeasureUnit,
    val costPerBase: Money,
    val lineCost: Money,
)

/** A fully computed recipe cost. costPerPortion is null when portions is invalid. */
data class RecipeCost(
    val lines: List<CostLine>,
    val ingredientSubtotal: Money,
    val wastageAmount: Money,
    val additionalTotal: Money,
    val batchCost: Money,
    val portions: Int,
    val costPerPortion: Money?,
)
