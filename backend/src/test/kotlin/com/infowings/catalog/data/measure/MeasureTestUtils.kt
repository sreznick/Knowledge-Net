package com.infowings.catalog.data.measure

import com.infowings.catalog.data.Measure
import org.junit.Assert
import java.math.BigDecimal
import java.math.RoundingMode

internal fun assertEquals(message: String, actual: BigDecimal, expected: BigDecimal) {
    val scaledActual = actual.setScale(4, RoundingMode.HALF_UP)
    val scaledExpected = expected.setScale(4, RoundingMode.HALF_UP)
    if (scaledActual != scaledExpected) {
        Assert.fail("$message, actual:$scaledActual, expected: $scaledExpected")
    }
}

internal fun measureTest(source: Double, sourceInBase: Double, measure: Measure<BigDecimal>, baseMeasure: Measure<BigDecimal>) {
    val sourceBigDecimal = BigDecimal(source)
    val baseValue = measure.toBase(sourceBigDecimal)
    assertEquals("Converting ${measure.symbol} to ${baseMeasure.symbol} fails", baseValue, BigDecimal(sourceInBase))
    assertEquals("Converting ${baseMeasure.symbol} to ${measure.symbol} fails", sourceBigDecimal, measure.fromBase(baseValue))
}