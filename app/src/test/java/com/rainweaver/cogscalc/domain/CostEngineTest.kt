package com.rainweaver.cogscalc.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal

class CostEngineTest {
    private fun bd(s: String) = BigDecimal(s)
    private fun money(s: String) = Money.of(bd(s))

    private fun flourLine(qty: String, unit: Unit, factor: BigDecimal? = null) =
        RecipeLineInput(1, "Flour", Unit.G, money("16"), bd(qty), unit, factor)

    @Test fun lineCostGrams() {
        assertEquals(0, bd("4000").compareTo(CostEngine.lineCost(flourLine("250", Unit.G)).value))
    }

    @Test fun lineCostKgAutoConverts() {
        // 0.25 kg = 250 g × Rp16 = 4000
        assertEquals(0, bd("4000").compareTo(CostEngine.lineCost(flourLine("0.25", Unit.KG)).value))
    }

    @Test fun lineCostCrossTypeWithFactor() {
        // 1 cup, explicit factor 125 g/cup × Rp16 = 2000
        assertEquals(0, bd("2000").compareTo(CostEngine.lineCost(flourLine("1", Unit.PIECE, bd("125"))).value))
    }

    @Test fun crossTypeWithoutFactorThrows() {
        assertThrows(IllegalArgumentException::class.java) { CostEngine.lineCost(flourLine("1", Unit.PIECE)) }
    }

    @Test fun batchCostWithAdditional() {
        val lines = listOf(flourLine("250", Unit.G), flourLine("500", Unit.G)) // 4000 + 8000
        val total = CostEngine.batchCost(lines, listOf(money("1000")))
        assertEquals(0, bd("13000").compareTo(total.value))
    }

    @Test fun costPerPortion() {
        val cp = CostEngine.costPerPortion(money("12000"), 10)
        assertEquals(0, bd("1200").compareTo(cp!!.value))
    }

    @Test fun costPerPortionZeroIsNull() {
        assertNull(CostEngine.costPerPortion(money("12000"), 0))
    }

    @Test fun costPerPortionRepeatingRounds() {
        val cp = CostEngine.costPerPortion(money("10000"), 3) // 3333.333333
        assertEquals(0, bd("3333.333333").compareTo(cp!!.value))
    }
}
