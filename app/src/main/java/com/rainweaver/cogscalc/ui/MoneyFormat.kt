package com.rainweaver.cogscalc.ui

import com.rainweaver.cogscalc.domain.Money
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/** Format a Money value as a localized currency string with the given decimals. */
fun Money.format(currency: String, decimals: Int): String {
    val d = decimals.coerceIn(0, 6)
    val v = value.setScale(d, RoundingMode.HALF_UP)
    val nf = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = d; maximumFractionDigits = d
    }
    val sym = if (currency.isBlank()) "" else currency.trim() + " "
    return sym + nf.format(v)
}
