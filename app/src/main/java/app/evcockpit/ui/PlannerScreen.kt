package app.evcockpit.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.evcockpit.navigation.NavigationRoute
import app.evcockpit.obd.SocReading
import app.evcockpit.planui.*
import app.evcockpit.planner.energy.HvacMode
import app.evcockpit.planner.model.*
import app.evcockpit.storage.PlannerOptionsStore
import app.evcockpit.storage.VehicleProfileStore
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun PlannerScreen(
    vm:PlannerViewModel,
    navigation:NavigationRoute?,
    soc:SocReading,
    onOpenRoute:()->Unit,
    onOpenSettings:()->Unit,
) {
    val context=LocalContext.current
    val optionsStore=remember(context){PlannerOptionsStore(context.applicationContext)}
    val profileStore=remember(context){VehicleProfileStore(context.applicationContext)}
    val options by optionsStore.options.collectAsState(initial=PlannerOptions())
    val profile by profileStore.profile.collectAsState(initial=VehicleProfile.defaults())
    val state by vm.state.collectAsStateWithLifecycle()
    val scope=rememberCoroutineScope()
    var parameters by remember{mutableStateOf(false)}
    var dataFlow by remember{mutableStateOf(false)}
    var optionsError by remember{mutableStateOf<String?>(null)}
    val busy=state.stage in setOf(PlannerStage.ELEVATION,PlannerStage.STATIONS,
        PlannerStage.OPTIMIZING,PlannerStage.VERIFYING)
    fun persist(value:PlannerOptions) {
        val errors=value.errors()
        if(errors.isNotEmpty())optionsError=errors.first()
        else {
            optionsError=null
            scope.launch{optionsStore.save(value)}
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("EV Rota Planlayıcı",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
        Text("TAHMİN • DOĞRULANMADI",color=MaterialTheme.colorScheme.error,
            style=MaterialTheme.typography.titleMedium)
        val success=state.plan as? PlanResult.Success
        Text(if(success==null)"Belirsizlik: henüz hesaplanmadı — TAHMİN"
            else PlannerDisplay.uncertainty(success))
        Text("SoC kaynağı: "+soc.source+" • "+
            (soc.pct?.let{"%"+("%.1f".format(it))} ?:"değer yok"))
        Text("Kaynak rota: "+(navigation?.provider ?: "yok")+
            " • trafik dahil değil, sahada DOĞRULANMADI")
        Text(state.message)
        Text(state.warning,style=MaterialTheme.typography.bodySmall)
        Text("İstasyonlar: "+state.stationStatus,style=MaterialTheme.typography.bodySmall)
        Text("Çözücü hesap süresi: "+
            (state.solverElapsedMs?.let{it.toString()+" ms"} ?:"ölçülmedi")+
            " • POCO F5 değeri için ekranı test edin")
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Button(onClick={vm.plan(navigation,soc,options,profile)},enabled=!busy,
                modifier=Modifier.weight(1f).heightIn(min=48.dp)) {
                Text("Şarj planı hesapla")
            }
            if(busy) OutlinedButton(onClick=vm::cancel,
                modifier=Modifier.heightIn(min=48.dp)){Text("İptal")}
        }
        if(navigation==null) {
            Text("Önce GPS'i açın, hedef belirleyin ve kendi HeiGIT anahtarınızla rota oluşturun.")
            OutlinedButton(onClick=onOpenRoute){Text("Rota ekranına git")}
            OutlinedButton(onClick=onOpenSettings){Text("GPS / HeiGIT anahtar ayarları")}
        }
        if(soc.pct==null || soc.source.startsWith("DEMO")) {
            OutlinedButton(onClick=onOpenSettings){Text("Manuel SoC gir / kontrol et")}
        }
        OutlinedButton(onClick={dataFlow=true}) {Text("Veri akışı • hangi hizmete ne gönderiliyor?")}
        OutlinedButton(onClick={parameters=!parameters}) {
            Text(if(parameters)"Parametre panelini kapat" else "Parametreleri düzenle")
        }
        if(parameters) {
            Text("Tüm ayarlar cihazda kalıcı; değerler model varsayımıdır.")
            PlannerNumber("Varışta asgari SoC (%)",options.minArrivalSoc){
                persist(options.copy(minArrivalSoc=it))
            }
            PlannerNumber("Durakta asgari SoC (%)",options.minStopSoc){
                persist(options.copy(minStopSoc=it))
            }
            PlannerNumber("Azami şarj SoC (%)",options.maxChargeSoc){
                persist(options.copy(maxChargeSoc=it))
            }
            PlannerNumber("Azami seyir hızı (km/sa)",options.maxCruiseKmh){
                persist(options.copy(maxCruiseKmh=it))
            }
            PlannerNumber("Ortam sıcaklığı (°C)",options.temperatureC){
                persist(options.copy(temperatureC=it))
            }
            PlannerNumber("Ek yük (kg)",options.extraLoadKg){
                persist(options.copy(extraLoadKg=it))
            }
            PlannerNumber("Tüketim düzeltmesi (%)",options.consumptionAdjust*100.0){
                persist(options.copy(consumptionAdjust=it/100.0))
            }
            PlannerNumber("Asgari istasyon gücü (kW)",options.minStationKw){
                persist(options.copy(minStationKw=it))
            }
            PlannerNumber("İstasyon yarıçapı (km)",options.searchRadiusMeters/1000.0){
                persist(options.copy(searchRadiusMeters=(it*1000.0).roundToInt()))
            }
            PlannerNumber("En çok durak",options.maxStops.toDouble()){
                persist(options.copy(maxStops=it.toInt()))
            }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("HVAC: "+options.hvac.name,modifier=Modifier.weight(1f))
                OutlinedButton(onClick={
                    persist(options.copy(hvac=when(options.hvac){
                        HvacMode.OFF->HvacMode.AUTO
                        HvacMode.AUTO->HvacMode.HEATING
                        HvacMode.HEATING->HvacMode.OFF
                    }))
                }) {Text("Değiştir")}
            }
            Row {
                Text("Yalnız OSM CCS2 etiketli (cihaz durumu yine bilinmiyor)",
                    modifier=Modifier.weight(1f))
                Switch(checked=options.onlyTaggedCcs2,
                    onCheckedChange={persist(options.copy(onlyTaggedCcs2=it))})
            }
            Row {
                Text("Yavaş AC adaylarına izin ver",modifier=Modifier.weight(1f))
                Switch(checked=options.allowSlowCharging,
                    onCheckedChange={persist(options.copy(allowSlowCharging=it))})
            }
            var excluded by remember(options.excludedNetworks){
                mutableStateOf(options.excludedNetworks)
            }
            OutlinedTextField(value=excluded,onValueChange={excluded=it},
                label={Text("Dışlanan ağlar (virgülle)")},modifier=Modifier.fillMaxWidth())
            OutlinedButton(onClick={persist(options.copy(excludedNetworks=excluded))}){
                Text("Ağ filtresini kaydet")
            }
            Text("Hedefte şarj bilgisi v1'de optimizasyon maliyetine dahil değil.")
            PlannerNumber("Hedefte şarj varsa kW (0 = yok)",
                options.destinationChargeKw ?: 0.0) {
                persist(options.copy(destinationChargeKw=it.takeIf{value->value>0}))
            }
            OutlinedButton(onClick={scope.launch{optionsStore.reset()}}) {
                Text("Plan ayarlarını varsayılana döndür")
            }
            optionsError?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        }
        if(state.plan is PlanResult.Failure) {
            val failed=state.plan as PlanResult.Failure
            Text("PLAN YOK: "+failed.reason, color=MaterialTheme.colorScheme.error)
            Text("En yakın tahmini ulaşılabilir nokta: "+
                ("%.1f km".format(failed.nearestReachableKm)))
        }
        if(success!=null) {
            Text(PlannerDisplay.overview(success),style=MaterialTheme.typography.titleMedium)
            Text("Tahmini varış enerji: "+
                ("%.1f kWh".format(success.arrivalSocPct/100.0*
                    (state.request?.car?.capacityAtSohKwh ?: 0.0))))
            Text("Fiziksel süre ve risk cezası ayrı: "+
                ("%.0f + %.0f dk".format(success.physicalMinutes,success.riskPenaltyMinutes)))
            Text("Canlı trafik, fiyat ve doluluk yok. Şarj eğrisi TAHMİN.")
            SocProfileChart(success,state.request?.startSocPct ?: 0.0,
                state.request?.minArrivalSoc ?: 10.0)
            if(success.stops.isEmpty())Text("Menzil tahminine göre şarj durağı gerekmiyor.")
            success.stops.forEachIndexed { index,stop ->
                ElevatedCard(modifier=Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp),
                        verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        Text("Durak "+(index+1)+" • "+stop.charger.name,
                            fontWeight=FontWeight.Bold)
                        Text("Operatör: "+stop.charger.operator)
                        Text(PlannerDisplay.stop(stop))
                        Text("Kaynak: "+stop.charger.powerKw.source)
                        Text("Güven: "+stop.charger.confidence.name+
                            " • doluluk bilinmiyor • TAHMİN")
                        Text("Sapma ~"+("%.1f km".format(stop.charger.detourKm))+
                            " • "+("%.0f dk".format(stop.extraMinutes))+
                            " (gerçek yol DOĞRULANMADI)")
                        val backups=state.alternatives?.backups
                            ?.filter{it.originalStopId==stop.charger.id}.orEmpty()
                        if(backups.isEmpty())Text("Yedek istasyon: veri yok / DOĞRULANMADI")
                        else backups.forEach {
                            Text("Yedek istasyon (TAHMİN): "+it.candidate.name+
                                " • "+it.note)
                        }
                    }
                }
            }
            state.alternatives?.fewerStops?.let {
                Text("Daha az durak alternatifi: "+it.stops.size+" durak (TAHMİN)")
            }
            state.alternatives?.safer?.let {
                Text("Güvenli CCS2/+%5 SoC alternatifi: "+it.stops.size+" durak (TAHMİN)")
            }
            success.warnings.forEach { Text(it,style=MaterialTheme.typography.bodySmall) }
            Text("Harita işaretleri, navigasyona aktarma ve kayıt: M8.7 ile bağlanacak.")
        }
        Text("© OpenStreetMap katkıcıları / OpenFreeMap • © openrouteservice • Open-Meteo")
        Text("Sürüş öncesi kullanım. Sürüşte ekrana yoğun dokunmayın.",
            style=MaterialTheme.typography.bodySmall)
    }
    if(dataFlow) {
        AlertDialog(onDismissRequest={dataFlow=false},
            title={Text("Veri akışı — kullanıcı isteğiyle")},
            text={
                Column(Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text("HeiGIT ORS: GPS başlangıç, hedef ve seçilmiş durak koordinatları; anahtar yalnız HTTPS Authorization başlığında.")
                    Text("Open-Meteo Elevation: örneklenmiş rota enlem/boylamları; plan isteğinde en çok 3 grup × 100 nokta.")
                    Text("OSM/Overpass: sadeleştirilmiş rota koridoru, 3 km yarıçap (ayar değişebilir); 7 gün önbellek.")
                    Text("Open-Meteo Forecast: rota isteğinde başlangıç/orta/hedef hava koordinatları.")
                    Text("Photon: yalnız Ara düğmesine basılan adres metni. En az 10 saniye aralık.")
                    Text("Kamu sunucularının kota/erişim ve güncellik garantisi yok. Gerçek ağ/telefon DOĞRULANMADI.")
                    Text("Veri akışı kaynağı: docs/M8_VERI_AKISI.md; OSM/OpenFreeMap/Open-Meteo/ORS atıfları.")
                }
            },
            confirmButton={TextButton(onClick={dataFlow=false}){Text("Kapat")}})
    }
}

@Composable
private fun PlannerNumber(label:String,value:Double,onSave:(Double)->Unit) {
    var entered by remember(value){mutableStateOf(value.toString())}
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp),
        modifier=Modifier.fillMaxWidth()) {
        OutlinedTextField(value=entered,onValueChange={entered=it},label={Text(label)},
            singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),
            modifier=Modifier.weight(1f))
        OutlinedButton(onClick={
            val parsed=entered.trim().replace(',','.').toDoubleOrNull()
            if(parsed!=null && parsed.isFinite())onSave(parsed)
        },modifier=Modifier.heightIn(min=48.dp)){
            Text("Kaydet")
        }
    }
}

@Composable
private fun SocProfileChart(plan:PlanResult.Success,startSoc:Double,minSoc:Double) {
    val distance=plan.distanceKm.coerceAtLeast(0.001)
    val p=mutableListOf(0.0 to startSoc)
    plan.stops.forEach { stop->
        val x=stop.charger.distanceAlongMeters/1000.0
        p+=x to stop.arrivalSocPct.toDouble()
        p+=x to stop.departureSocPct.toDouble()
    }
    p+=distance to plan.arrivalSocPct.toDouble()
    var selected by remember {mutableStateOf("")}
    Text("SoC profili — çizgi ve alt sınır (TAHMİN)")
    Canvas(Modifier.fillMaxWidth().height(164.dp)
        .semantics { contentDescription="Mesafeye göre tahmini SoC grafiği" }
        .pointerInput(p) {
            detectTapGestures { offset ->
                val x=(offset.x/size.width.toFloat()).coerceIn(0f,1f)*distance
                val closest=p.minByOrNull {kotlin.math.abs(it.first-x)}
                selected="Yaklaşık "+("%.1f km".format(x))+" • SoC "+
                    (closest?.second?.roundToInt()?.toString() ?:"--")+"% (TAHMİN)"
            }
        }) {
        fun point(item:Pair<Double,Double>)=Offset(
            (item.first/distance*size.width).toFloat().coerceIn(0f,size.width),
            (size.height*(1-item.second.coerceIn(0.0,100.0)/100.0)).toFloat())
        val refY=(size.height*(1-minSoc.coerceIn(0.0,100.0)/100.0)).toFloat()
        drawLine(Color.Red.copy(alpha=0.75f),
            Offset(0f,refY),Offset(size.width,refY),strokeWidth=2.dp.toPx())
        for((a,b) in p.zipWithNext()) {
            drawLine(Color(0xFF30CFAA),point(a),point(b),strokeWidth=3.dp.toPx())
        }
        val last=p.last()
        val optimistic=last.first to plan.optimisticArrivalSocPct
        val pessimistic=last.first to plan.pessimisticArrivalSocPct
        drawLine(Color(0xFF83B8FF),point(pessimistic),point(optimistic),
            strokeWidth=5.dp.toPx())
    }
    if(selected.isNotBlank())Text(selected)
    Text("Mavi varış aralığı: model ±%10/+%15 varsayımı; istatistiksel güven aralığı değildir.",
        style=MaterialTheme.typography.bodySmall)
}
