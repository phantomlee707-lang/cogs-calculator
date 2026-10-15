package com.rainweaver.cogscalc.domain

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Money backed by [BigDecimal] (exact decimal arithmetic — never binary float).
 * Internal precision is kept high; rounding to the currency's display scale
 * happens only at presentation time via [rounded].
 */
@JvmInline
value class Money(val value: BigDecimal) : Comparable<Money> {

    operator fun plus(other: Money): Money = Money(value.add(other.value))

    operator fun minus(other: Money): Money = Money(value.subtract(other.value))

    operator fun times(quantity: BigDecimal): Money = Money(value.multiply(quantity))

    fun divide(divisor: BigDecimal, scale: Int = 6, mode: RoundingMode = RoundingMode.HALF_UP): Money =
        Money(value.divide(divisor, scale, mode))

    fun rounded(scale: Int, mode: RoundingMode = RoundingMode.HALF_UP): Money =
        Money(value.setScale(scale.coerceAtLeast(0), mode))

    val isNegative: Boolean get() = value.signum() < 0

    override fun compareTo(other: Money): Int = value.compareTo(other.value)

    companion object {
        val ZERO: Money = Money(BigDecimal.ZERO)
        fun of(value: BigDecimal): Money = Money(value)
        fun of(value: Long): Money = Money(BigDecimal.valueOf(value))
    }
}
