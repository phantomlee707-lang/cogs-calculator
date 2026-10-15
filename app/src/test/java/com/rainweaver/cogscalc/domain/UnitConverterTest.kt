package com.rainweaver.cogscalc.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class UnitConverterTest {
    private fun bd(s: String) = BigDecimal(s)

    @Test fun weightConverts() {
        assertEquals(0, bd("1000").compareTo(UnitConverter.convert(bd("1"), Unit.KG, Unit.G)))
        assertEquals(0, bd("1").compareTo(UnitConverter.convert(bd("1000"), Unit.G, Unit.KG)))
        assertEquals(0, bd("250").compareTo(UnitConverter.convert(bd("0.25"), Unit.KG, Unit.G)))
    }

    @Test fun volumeConverts() {
        assertEquals(0, bd("1000").compareTo(UnitConverter.convert(bd("1"), Unit.L, Unit.ML)))
        assertEquals(0, bd("2.5").compareTo(UnitConverter.convert(bd("2500"), Unit.ML, Unit.L)))
    }

    @Test fun countIsIdentity() {
        assertEquals(0, bd("12").compareTo(UnitConverter.convert(bd("12"), Unit.PIECE, Unit.PIECE)))
    }

    @Test fun incompatibleThrows() {
        assertThrows(IllegalArgumentException::class.java) { UnitConverter.convert(bd("1"), Unit.KG, Unit.ML) }
        assertThrows(IllegalArgumentException::class.java) { UnitConverter.convert(bd("1"), Unit.G, Unit.PIECE) }
    }

    @Test fun compatible() {
        assertTrue(UnitConverter.compatible(Unit.KG, Unit.G))
        assertFalse(UnitConverter.compatible(Unit.KG, Unit.L))
        assertFalse(UnitConverter.compatible(Unit.G, Unit.PIECE))
    }
}
