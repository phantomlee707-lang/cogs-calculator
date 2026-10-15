package com.rainweaver.cogscalc.domain

import java.math.BigDecimal

/** The three physical dimensions a unit can belong to. */
enum class UnitType { WEIGHT, VOLUME, COUNT }

/**
 * A measurement unit. [baseFactor] is how many BASE units make one of this unit
 * (weight base = gram, volume base = ml, count base = piece). Weight, volume and
 * count are never auto-converted between each other.
 */
enum class MeasureUnit(val type: UnitType, val baseFactor: BigDecimal, val label: String) {
    G(UnitType.WEIGHT, BigDecimal.ONE, "g"),
    KG(UnitType.WEIGHT, BigDecimal.valueOf(1000), "kg"),
    ML(UnitType.VOLUME, BigDecimal.ONE, "ml"),
    L(UnitType.VOLUME, BigDecimal.valueOf(1000), "L"),
    PIECE(UnitType.COUNT, BigDecimal.ONE, "pcs");

    companion object {
        fun fromLabel(label: String): MeasureUnit? = entries.firstOrNull { it.label.equals(label.trim(), ignoreCase = true) }
    }
}
