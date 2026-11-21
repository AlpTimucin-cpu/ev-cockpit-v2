package app.evcockpit.ui.tesla

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TeslaDesignSystemTest {
    @Test fun exactSourceLabelsNeverPretendToBeVerified() {
        assertEquals("OBD", SourceKind.OBD.label)
        assertEquals("MANUEL", SourceKind.MANUEL.label)
        assertEquals("DEMO", SourceKind.DEMO.label)
        assertEquals("YOK", SourceKind.YOK.label)
        assertEquals("ESKİ", SourceKind.ESKI.label)
    }
    @Test fun speedDoesNotInventUnknownOrNegativeValues() {
        assertEquals("--", TeslaDisplayPolicy.speed(null))
        assertEquals("--", TeslaDisplayPolicy.speed(Double.NaN))
        assertEquals("--", TeslaDisplayPolicy.speed(Double.POSITIVE_INFINITY))
        assertEquals("--", TeslaDisplayPolicy.speed(-8.0))
        assertEquals("90", TeslaDisplayPolicy.speed(89.7))
        assertEquals("0", TeslaDisplayPolicy.speed(0.0))
    }
    @Test fun socCannotSilentlyClampOutOfRangeReadings() {
        assertEquals("--", TeslaDisplayPolicy.soc(null))
        assertEquals("--", TeslaDisplayPolicy.soc(-1.0))
        assertEquals("--", TeslaDisplayPolicy.soc(101.0))
        assertEquals("--", TeslaDisplayPolicy.soc(Double.NaN))
        assertEquals("64", TeslaDisplayPolicy.soc(64.1))
    }
    @Test fun energyAndRangeAreAlwaysExplicitEstimates() {
        assertEquals("Enerji verisi yok", TeslaDisplayPolicy.estimatedEnergy(null))
        assertEquals("Menzil hesaplanamadı", TeslaDisplayPolicy.estimatedRange(null))
        assertEquals("Enerji verisi yok", TeslaDisplayPolicy.estimatedEnergy(-10.0))
        assertEquals("Menzil hesaplanamadı", TeslaDisplayPolicy.estimatedRange(Double.NaN))
        assertTrue(TeslaDisplayPolicy.estimatedEnergy(33.28).contains("TAHMİN"))
        assertTrue(TeslaDisplayPolicy.estimatedRange(245.2).contains("TAHMİN"))
    }
    @Test fun powerAxisClampsAndRejectsMissingReadings() {
        assertEquals(0f, TeslaDisplayPolicy.powerFraction(null), 0f)
        assertEquals(0f, TeslaDisplayPolicy.powerFraction(Double.NaN), 0f)
        assertEquals(0f, TeslaDisplayPolicy.powerFraction(5.0, 0.0), 0f)
        assertEquals(0.5f, TeslaDisplayPolicy.powerFraction(-62.5), 1e-6f)
        assertEquals(0.5f, TeslaDisplayPolicy.powerFraction(62.5), 1e-6f)
        assertEquals(1f, TeslaDisplayPolicy.powerFraction(1000.0), 0f)
    }
    @Test fun layoutWeightsAndDefaultsAreCentralized() {
        val total = TeslaTokens.leftPaneWeight + TeslaTokens.mapWeight +
            TeslaTokens.actionWeight
        assertEquals(1f, total, 1e-6f)
        assertEquals(5.0, TeslaTokens.drivingThresholdKmh, 0.0)
        assertEquals(3000L, TeslaTokens.staleObdAfterMs)
        assertEquals(150, TeslaTokens.animationMillis)
        assertEquals(56f, TeslaTokens.touchTarget.value, 0f)
        assertFalse(PanelStatus.EMPTY == PanelStatus.NORMAL)
    }
}
