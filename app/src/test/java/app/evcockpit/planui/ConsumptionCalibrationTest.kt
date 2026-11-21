package app.evcockpit.planui

import app.evcockpit.planner.model.*
import app.evcockpit.planner.route.*
import app.evcockpit.storage.*
import org.junit.Assert.*
import org.junit.Test

class ConsumptionCalibrationTest {
    private fun request(): PlanRequest {
        val profile = VehicleProfile.defaults()
        val points = listOf(
            RoutePoint(38.0,35.0,0.0,0.0,90.0),
            RoutePoint(38.1,35.1,10_000.0,0.0,90.0),
        )
        return PlanRequest(
            route = RouteProfile(points,false,"SENTETİK / DOĞRULANMADI",
                0.0,0.0,10.0/90.0),
            car = profile,
            candidates = emptyList(),
            startSocPct = 80.0,
        )
    }

    @Test fun modelConsumptionExistsButIsNotVerifiedVehicleMeasurement() {
        val model = ConsumptionCalibration.modelKwhPer100Km(request())
        assertNotNull(model)
        assertTrue(model!! > 1.0 && model < 100.0)
    }

    @Test fun suggestedCorrectionDoesNotAutomaticallyChangeOptions() {
        val options = PlannerOptions(consumptionAdjust = 0.05)
        val suggested = ConsumptionCalibration.suggest(
            modelKwhPer100Km=20.0, observedKwhPer100Km=22.0,
            plannedArrivalSoc=40.0, observedArrivalSoc=34.0,
            existingAdjustment=options.consumptionAdjust,
        )
        assertEquals(0.15,suggested.suggestedConsumptionAdjust,1e-9)
        assertFalse(suggested.verified)
        assertEquals(0.05,options.consumptionAdjust,0.0)
        assertTrue(suggested.confidenceNote.contains("DOĞRULANMADI"))
        assertTrue(suggested.confidenceNote.contains("SoC farkı"))
    }

    @Test fun modelGuardFailsClosedForExtremeInputs() {
        assertThrows(IllegalArgumentException::class.java) {
            ConsumptionCalibration.suggest(0.0,20.0,40.0,35.0,0.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ConsumptionCalibration.suggest(20.0,Double.NaN,40.0,35.0,0.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ConsumptionCalibration.suggest(20.0,22.0,40.0,101.0,0.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ConsumptionCalibration.suggest(20.0,22.0,40.0,35.0,0.8)
        }
        val clamped = ConsumptionCalibration.suggest(10.0,90.0,50.0,30.0,0.0)
        assertEquals(0.3,clamped.suggestedConsumptionAdjust,1e-9)
    }

    @Test fun diaryStoresOnlyBoundedNumbersNeverRouteOrIdentifiers() {
        val r = CalibrationRecord(20.0,22.0,40.0,34.0,15.0)
        val json = CalibrationJournalCodec.encode(List(12){r})
        assertEquals(10,CalibrationJournalCodec.decode(json).size)
        assertFalse(json.contains("lat"))
        assertFalse(json.contains("lon"))
        assertFalse(json.contains("VIN"))
        assertFalse(json.contains("token"))
        assertEquals(r,CalibrationJournalCodec.decode(json).first())
    }

    @Test fun malformedJournalNeverInventsVerifiedEvidence() {
        assertTrue(CalibrationJournalCodec.decode("{").isEmpty())
        assertTrue(CalibrationJournalCodec.decode(
            """[{"modelKwh":10,"actualKwh":12,"planSoc":20,"actualSoc":999,"adjustPct":10}]"""
        ).isEmpty())
        assertTrue(CalibrationJournalCodec.decode(
            "x".repeat(4097)
        ).isEmpty())
    }
}
