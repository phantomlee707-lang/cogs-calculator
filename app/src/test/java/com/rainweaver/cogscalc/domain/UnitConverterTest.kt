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
        assertEquals(0, bd("1000").compareTo(UnitConverter.convert(bd("1"), MeasureUnit.KG, MeasureUnit.G)))
        assertEquals(0, bd("1").compareTo(UnitConverter.convert(bd("1000"), MeasureUnit.G, MeasureUnit.KG)))
        assertEquals(0, bd("250").compareTo(UnitConverter.convert(bd("0.25"), MeasureUnit.KG, MeasureUnit.G)))
    }

    @Test fun volumeConverts() {
        assertEquals(0, bd("1000").compareTo(UnitConverter.convert(bd("1"), MeasureUnit.L, MeasureUnit.ML)))
        assertEquals(0, bd("2.5").compareTo(UnitConverter.convert(bd("2500"), MeasureUnit.ML, MeasureUnit.L)))
    }

    @Test fun countIsIdentity() {
        assertEquals(0, bd("12").compareTo(UnitConverter.convert(bd("12"), MeasureUnit.PIECE, MeasureUnit.PIECE)))
    }

    @Test fun incompatibleThrows() {
        assertThrows(IllegalArgumentException::class.java) { UnitConverter.convert(bd("1"), MeasureUnit.KG, MeasureUnit.ML) }
        assertThrows(IllegalArgumentException::class.java) { UnitConverter.convert(bd("1"), MeasureUnit.G, MeasureUnit.PIECE) }
    }

    @Test fun compatible() {
        assertTrue(UnitConverter.compatible(MeasureUnit.KG, MeasureUnit.G))
        assertFalse(UnitConverter.compatible(MeasureUnit.KG, MeasureUnit.L))
        assertFalse(UnitConverter.compatible(MeasureUnit.G, MeasureUnit.PIECE))
    }
}
