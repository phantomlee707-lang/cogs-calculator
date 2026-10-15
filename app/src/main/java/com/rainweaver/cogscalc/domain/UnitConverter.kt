package com.rainweaver.cogscalc.domain

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Converts between compatible units (same [UnitType]). Weight, volume and count
 * are NOT interchangeable — cross-type conversion requires an explicit,
 * ingredient-specific factor (handled by the cost engine, not here).
 */
object UnitConverter {

    fun compatible(a: MeasureUnit, b: MeasureUnit): Boolean = a.type == b.type

    /** Convert [quantity] from [from] to [to]. Throws if the units are incompatible. */
    fun convert(quantity: BigDecimal, from: MeasureUnit, to: MeasureUnit): BigDecimal {
        require(from.type == to.type) {
            "Incompatible units: ${from.label} (${from.type}) cannot convert to ${to.label} (${to.type})"
        }
        val inBase = quantity.multiply(from.baseFactor)
        return inBase.divide(to.baseFactor, 12, RoundingMode.HALF_UP).stripTrailingZeros()
    }
}
