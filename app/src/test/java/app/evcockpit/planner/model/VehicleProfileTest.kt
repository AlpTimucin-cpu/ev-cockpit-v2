package app.evcockpit.planner.model

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class VehicleProfileTest {
    @Test fun assumptionsRemainUnverifiedAndSourced() {
        val p=VehicleProfile.defaults()
        val list=listOf(p.usableKwh,p.sohPercent,p.massKg,p.extraLoadKg,p.cdA,p.crr,
            p.drivetrainEfficiency,p.regenEfficiency,p.maxRegenKw,p.maxDcKw,p.maxAcKw,
            p.auxBaseKw,p.hvacAutoKw,p.hvacHeatingKw,p.consumptionAdjust)
        assertTrue(list.all { !it.verified && it.source.isNotBlank() })
        assertFalse(p.chargingCurve.verified);assertFalse(p.coldDerateTable.verified)
        assertTrue(p.validate().isEmpty())
    }
    @Test fun profileRejectsInvalidValues() {
        val p=VehicleProfile.defaults()
        assertTrue(p.copy(usableKwh=p.usableKwh.copy(value=0.0)).validate().isNotEmpty())
        assertTrue(p.copy(consumptionAdjust=p.consumptionAdjust.copy(value=0.31)).validate().isNotEmpty())
        assertEquals(46.8,p.copy(sohPercent=p.sohPercent.copy(value=90.0)).capacityAtSohKwh,0.0001)
    }
    @Test fun chargingCurveIsStable() {
        val p=VehicleProfile.defaults()
        assertEquals(p.chargingCurve.at(60.0),p.chargingCurve.at(60.0),0.0)
        assertTrue(p.chargingCurve.at(60.0)>p.chargingCurve.at(90.0))
    }
    @Test fun plannerHasNoAndroidImports() {
        val root=File("src/main/java/app/evcockpit/planner")
        assertTrue(root.isDirectory)
        root.walkTopDown().filter { it.isFile && it.extension=="kt" }.forEach { file ->
            assertFalse(file.path,Regex("""(?m)^\s*import\s+android\.""")
                .containsMatchIn(file.readText()))
        }
    }
}
