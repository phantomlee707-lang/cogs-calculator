package com.rainweaver.cogscalc.domain

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Selling-price estimators. Markup and target gross margin are NOT equivalent and
 * are kept clearly separate.
 *   markup on cost : price = cost × (1 + markup%)
 *   target margin  : price = cost ÷ (1 − margin%)
 */
object Pricing {

    fun markupPrice(cost: Money, markupPercent: BigDecimal): Money =
        cost * (BigDecimal.ONE + markupPercent.divide(BigDecimal(100)))

    /** Target gross margin price, or null when margin ≥ 100% (undefined). */
    fun marginPrice(cost: Money, marginPercent: BigDecimal): Money? {
        val denom = BigDecimal.ONE - marginPercent.divide(BigDecimal(100))
        if (denom <= BigDecimal.ZERO) return null
        return cost.divide(denom, 6, RoundingMode.HALF_UP)
    }

    /** Round a price to the nearest [step] (e.g. 500). No-op when step ≤ 0. */
    fun roundToStep(value: Money, step: BigDecimal): Money {
        if (step <= BigDecimal.ZERO) return value
        return Money(value.value.divide(step, 0, RoundingMode.HALF_UP).multiply(step))
    }
}
