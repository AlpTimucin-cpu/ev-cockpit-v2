package app.evcockpit.planner.model

import org.junit.Assert.*
import org.junit.Test

class ChargingCurveConsistencyTest {
    @Test fun defaultCurveIsExplicitlyUnverifiedButConsistent() {
        val findings = ChargingCurveConsistency.warnings(VehicleProfile.defaults())
        assertTrue(findings.any { "TAHMİN" in it })
        assertFalse(findings.any { "aşıyor" in it })
    }

    @Test fun nominalPeakMismatchIsFlaggedWithoutMutatingInputs() {
        val base = VehicleProfile.defaults()
        val originalCurve = base.chargingCurve.points
        val highPeak = base.chargingCurve.copy(
            points = base.chargingCurve.points.mapIndexed { index, point ->
                if (index == 1) point.copy(y = 115.0) else point
            }
        )
        val profile = base.copy(chargingCurve = highPeak)
        val findings = ChargingCurveConsistency.warnings(profile)

        assertTrue(findings.any { "aşıyor" in it && "DOĞRULANMADI" in it })
        assertEquals(115.0, profile.chargingCurve.points.maxOf { it.y }, 0.00001)
        assertEquals(originalCurve, base.chargingCurve.points)
        assertTrue(profile.validate().isEmpty())
    }

    @Test fun userObservedDcPowerCannotTurnEstimateIntoVerifiedData() {
        val profile = VehicleProfile.defaults().copy(
            observedDcKw = SourcedValue(74.0, "araç ekranı", false)
        )
        val findings = ChargingCurveConsistency.warnings(profile)

        assertTrue(findings.any { "tek ölçüm" in it })
        assertFalse(profile.effectiveChargingCurve.verified)
        assertEquals(74.0, profile.effectiveChargingCurve.points.maxOf { it.y }, 0.001)
    }

    @Test fun modelNeverImportsOrRequiresThirdPartyVehicleDatabase() {
        // Only the project's own VehicleProfile/SourcedCurve contract is needed.
        val p = VehicleProfile.defaults().copy(
            chargingCurve = SourcedCurve(
                listOf(CurvePoint(0.0, 35.0), CurvePoint(50.0, 35.0),
                    CurvePoint(100.0, 5.0)),
                "synthetic-test-fixture", true
            )
        )
        assertTrue(ChargingCurveConsistency.warnings(p).isEmpty())
    }
}
