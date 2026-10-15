package com.rainweaver.cogscalc.domain

import java.math.BigDecimal

/** Input validation returning a human-readable message, or null when valid. */
object Validator {

    fun quantity(q: BigDecimal): String? = if (q <= BigDecimal.ZERO) "Quantity must be greater than zero" else null

    fun portions(p: Int): String? = if (p <= 0) "Portions must be greater than zero" else null

    fun price(p: Money): String? = if (p.isNegative) "Price cannot be negative" else null

    fun percent(p: BigDecimal): String? = if (p < BigDecimal.ZERO) "Percentage cannot be negative" else null

    fun margin(p: BigDecimal): String? = when {
        p < BigDecimal.ZERO -> "Margin cannot be negative"
        p >= BigDecimal(100) -> "Margin must be below 100%"
        else -> null
    }
}
