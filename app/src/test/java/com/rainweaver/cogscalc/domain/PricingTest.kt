package com.rainweaver.cogscalc.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class PricingTest {
    private fun bd(s: String) = BigDecimal(s)
    private fun money(s: String) = Money.of(bd(s))

    @Test fun markupOnCost() {
        // 10,000 × 1.30 = 13,000
        assertEquals(0, bd("13000").compareTo(Pricing.markupPrice(money("10000"), bd("30")).value))
    }

    @Test fun targetGrossMargin() {
        // 10,000 ÷ 0.70 = 14,285.714286
        val p = Pricing.marginPrice(money("10000"), bd("30"))
        assertEquals(0, bd("14285.714286").compareTo(p!!.value))
    }

    @Test fun marginAtOrAbove100IsNull() {
        assertNull(Pricing.marginPrice(money("10000"), bd("100")))
        assertNull(Pricing.marginPrice(money("10000"), bd("120")))
    }

    @Test fun roundToStep() {
        assertEquals(0, bd("13500").compareTo(Pricing.roundToStep(money("13333"), bd("500")).value))
        assertEquals(0, bd("14500").compareTo(Pricing.roundToStep(money("14286"), bd("500")).value))
    }

    @Test fun markupAndMarginDiffer() {
        val cost = money("10000")
        val markup = Pricing.markupPrice(cost, bd("30"))
        val margin = Pricing.marginPrice(cost, bd("30"))!!
        assertTrue(markup.value < margin.value) // 13,000 < 14,285.71
    }

    private fun assertTrue(cond: Boolean) = org.junit.Assert.assertTrue(cond)
}
