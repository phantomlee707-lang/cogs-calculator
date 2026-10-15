package com.rainweaver.cogscalc.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal

class IngredientCostTest {
    private fun bd(s: String) = BigDecimal(s)
    private fun money(s: String) = Money.of(bd(s))

    @Test fun flourPerGram() {
        // Rp16,000 for 1 kg → Rp16/g
        val c = IngredientCost.costPerBase(money("16000"), bd("1"), MeasureUnit.KG, MeasureUnit.G)
        assertEquals(0, bd("16").compareTo(c.value))
    }

    @Test fun oilPerMl() {
        // Rp50,000 for 2 L → Rp25/ml
        val c = IngredientCost.costPerBase(money("50000"), bd("2"), MeasureUnit.L, MeasureUnit.ML)
        assertEquals(0, bd("25").compareTo(c.value))
    }

    @Test fun zeroPurchaseThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            IngredientCost.costPerBase(money("100"), bd("0"), MeasureUnit.KG, MeasureUnit.G)
        }
    }
}
