package app.evcockpit.planner.model

data class ModelParameters(
    val gravity: SourcedValue = SourcedValue(9.80665,
        "https://physics.nist.gov/cgi-bin/cuu/Value?gn",true,"Standard gravity"),
    val seaLevelDensity: SourcedValue = SourcedValue(1.225,"ISA sea-level",false),
    val referenceTempC: SourcedValue = SourcedValue(15.0,"ISA reference",false),
    val kelvinOffset: SourcedValue = SourcedValue(273.15,"SI Celsius-Kelvin definition",true),
    val optimisticFactor: SourcedValue = SourcedValue(0.90,"M8 master uncertainty",false),
    val pessimisticFactor: SourcedValue = SourcedValue(1.15,"M8 master uncertainty",false),
    val coldDrivingFactor: SourcedCurve = SourcedCurve(
        listOf(CurvePoint(-20.0,1.25),CurvePoint(-5.0,1.15),
            CurvePoint(20.0,1.0),CurvePoint(40.0,1.0)),
        "M8 low temperature battery loss scenario", false, "TAHMİN"),
    val metersPerKm: SourcedValue = SourcedValue(1000.0,"SI units",true),
    val secondsPerHour: SourcedValue = SourcedValue(3600.0,"SI units",true),
    val wattsPerKw: SourcedValue = SourcedValue(1000.0,"SI units",true),
    val detourSpeedKmh: SourcedValue = SourcedValue(40.0,
        "M8 candidate detour speed assumption", false),
    val minSpeedKmh: SourcedValue = SourcedValue(5.0,"M8 master low speed",false),
    val initialSoc: SourcedValue = SourcedValue(70.0,"User default",false),
    val minArrivalSoc: SourcedValue = SourcedValue(10.0,"M8 master",false),
    val minStopSoc: SourcedValue = SourcedValue(10.0,"M8 master",false),
    val maxChargeSoc: SourcedValue = SourcedValue(80.0,"M8 master",false),
    val stopOverheadMinutes: SourcedValue = SourcedValue(3.0,"M8 master",false),
    val socGridStep: SourcedValue = SourcedValue(5.0,"M8 solver grid",false),
    val unknownStationPenaltyMinutes: SourcedValue = SourcedValue(25.0,"M8 risk default",false),
    val probableStationPenaltyMinutes: SourcedValue = SourcedValue(10.0,"M8 risk default",false),
)
