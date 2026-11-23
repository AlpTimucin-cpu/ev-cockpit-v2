package app.evcockpit.planner.charging

import app.evcockpit.planner.model.*
import app.evcockpit.storage.ManualTariffCodec
import org.junit.Assert.*
import org.junit.Test

class TripChargeCostTest {
    private fun charger(id: String, operator: String) = ChargerCandidate(
        id = id, name = id, operator = operator,
        latitude = 38.0, longitude = 35.0,
        powerKw = SourcedValue(120.0, "TEST / DOĞRULANMADI"),
        confidence = ChargerConfidence.UNKNOWN,
    )

    private fun success(stops: List<PlannedStop>) = PlanResult.Success(
        stops = stops, arrivalSocPct = 18,
        optimisticArrivalSocPct = 25.0, pessimisticArrivalSocPct = 10.0,
        distanceKm = 450.0, drivingMinutes = 310.0,
        chargingMinutes = 46.0, stopAndDetourMinutes = 15.0,
        riskPenaltyMinutes = 5.0, physicalMinutes = 371.0,
        riskAdjustedMinutes = 376.0, warnings = emptyList(),
    )

    @Test fun costUsesBatterySideSocDeltaWithoutInventingGridLosses() {
        val stop = PlannedStop(charger("A", "Eşarj"), 20, 80, 30.0, 5.0, 0.0)
        val manual = ManualChargeTariff(12.90, 1_800_000_000_000L)
        val result = TripChargeCost.calculate(success(listOf(stop)), 52.0,
            mapOf(TripChargeCost.normalizedOperator("Esarj") to manual),
            routeEnergyKwh = 76.5)
        assertEquals(31.2, result.totalBatteryAddedKwh, 1e-9)
        assertEquals(31.2, result.stops[0].billedKwhEstimate, 1e-9)
        assertEquals(402.48, result.totalCostTl!!, 0.0001)
        assertEquals(76.5, result.routeEnergyKwh!!, 0.0)
        assertEquals(0, result.unknownPriceStops)
        assertTrue(TripChargeCost.formatTl(result.totalCostTl).contains("TAHMİN"))
        assertFalse(manual.verified)
    }

    @Test fun missingSecondOperatorCannotBePricedAsZeroTl() {
        val first = PlannedStop(charger("A", "ZES"), 20, 70, 30.0, 5.0, 0.0)
        val second = PlannedStop(charger("B", "Bilgi yok"), 15, 65, 20.0, 3.0, 0.0)
        val result = TripChargeCost.calculate(success(listOf(first, second)), 52.0,
            mapOf("zes" to ManualChargeTariff(14.0, 1_800_000_000_000L)))
        assertNull(result.totalCostTl)
        assertEquals(1, result.unknownPriceStops)
        assertEquals(364.0, result.totalKnownCostTl, 0.001)
        assertTrue(TripChargeCost.formatTl(result.totalCostTl).contains("FİYAT YOK"))
    }

    @Test fun emptyChargingPlanIsZeroOnlyForOnRoutePurchases() {
        val result = TripChargeCost.calculate(success(emptyList()), 52.0, emptyMap())
        assertEquals(0.0, result.totalCostTl!!, 0.0)
        assertEquals(0.0, result.totalBatteryAddedKwh, 0.0)
        assertTrue(result.fullyPriced)
    }

    @Test fun outOfRangeAndStaleTariffsNeverClaimCurrentPrice() {
        assertFalse(ManualChargeTariff(0.0, 1_800_000_000_000L).isValid())
        assertFalse(ManualChargeTariff(151.0, 1_800_000_000_000L).isValid())
        assertFalse(ManualChargeTariff(Double.NaN, 1_800_000_000_000L).isValid())
        assertFalse(ManualChargeTariff(10.0, 1_800_000_000_000L,
            verified = true).isValid())
        val tariff = ManualChargeTariff(10.0, 1_800_000_000_000L)
        assertFalse(TripChargeCost.isStale(tariff, 1_800_000_000_000L))
        assertTrue(TripChargeCost.isStale(tariff, 1_800_000_000_000L +
            15L * 86_400_000L))
        assertTrue(TripChargeCost.isStale(tariff, 1_799_000_000_000L))
    }

    @Test fun persistenceStoresOnlyOperatorPricesAndNoTripCoordinates() {
        val price = ManualChargeTariff(12.5, 1_800_000_000_000L)
        val payload = ManualTariffCodec.encode(mapOf("Eşarj" to price))
        assertFalse(payload.contains("38.0"))
        assertFalse(payload.contains("latitude"))
        assertFalse(payload.contains("VIN"))
        assertFalse(payload.contains("apiKey"))
        val restored = ManualTariffCodec.decode(payload)
        assertEquals(price, restored["esarj"])
        assertTrue(ManualTariffCodec.decode("{broken").isEmpty())
        assertTrue(ManualTariffCodec.decode("x".repeat(8193)).isEmpty())
    }

    @Test fun batteryCapacityIsValidatedRatherThanAssumed() {
        assertThrows(IllegalArgumentException::class.java) {
            TripChargeCost.calculate(success(emptyList()), Double.NaN, emptyMap())
        }
        assertThrows(IllegalArgumentException::class.java) {
            TripChargeCost.calculate(success(emptyList()), 0.0, emptyMap())
        }
    }
}
