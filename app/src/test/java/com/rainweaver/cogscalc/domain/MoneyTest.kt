package com.rainweaver.cogscalc.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class MoneyTest {
    private fun m(s: String) = Money.of(BigDecimal(s))
    private fun eq(expected: String, actual: Money) =
        assertTrue("expected $expected got ${actual.value}", BigDecimal(expected).compareTo(actual.value) == 0)

    @Test fun arithmetic() {
        eq("150", m("100") + m("50"))
        eq("50", m("100") - m("50"))
        eq("300", m("100") * BigDecimal("3"))
    }

    @Test fun divideRoundsToSixDp() {
        eq("3.333333", m("10").divide(BigDecimal("3")))
    }

    @Test fun rounded() {
        eq("3.33", m("3.333333").rounded(2))
        eq("4", m("3.5").rounded(0))
    }

    @Test fun zeroAndNegative() {
        assertTrue(Money.ZERO.value.signum() == 0)
        assertTrue(m("-5").isNegative)
        assertFalse(m("5").isNegative)
    }
}
