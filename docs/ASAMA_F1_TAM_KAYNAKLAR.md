# Aşama F1 — TAM kaynak kodlar ve telefon kurulum rehberi

**Depo:** AlpTimucin-cpu/ev-cockpit-v2
**Dal:** `feature/stage-f1-map-first-shell-20261123`
**Temel:** `docs/stage-f-final-hmi-spec-20261123` (nihai HMI PR #113)
**Durum:** GitHub Actions CI kaydı ile ayrıca doğrulanmalıdır. Bu belge kodu ve test kaynaklarını tam sunar; gerçek telefon/araç testi değildir.

## Kapsam ve çakışma kuralları
- Yalnız F1, yani veri kaynağı olmadan Compose/MapLibre telefon Activity iskeleti, açık DEMO ve boş `—` modları.
- `App` / `MainActivity` ve A–C servisleri değiştirilmedi; yeni `MapFirstDashboardActivity` yalnız Ayarlar düğmesiyle açılır.
- Android Auto şablonu ve Mola değişmedi. Gerçek hız, hava, OBD, istasyon/rota bağlaması F2–F5'in işidir.
- Kullanıcının ZIP'inde derlenmiş Flutter SBF APK bulundu; araç veri protokolü doğrulanmadı. SBF kodları, görselleri veya şarj verileri alınmadı.
- Çevrim içi MapLibre sadece temel harita karolarını çekebilir. Önizleme (IDE @Preview) native harita çizmez ve hiçbir sahte yol/şarj noktası oluşturmaz.

## Telefonda GitHub akışı
1. [F1 taslak PR](https://github.com/AlpTimucin-cpu/ev-cockpit-v2/pull/114) bağlantısını aç; bu dalın dosyaları **zaten commit edildi**. Tek tek kopyalama gerekmez.
2. [F1 dalından Actions](https://github.com/AlpTimucin-cpu/ev-cockpit-v2/actions) sayfasında başlatılan son başarılı **Android CI** çalışmasını aç.
3. Alttaki **Artifacts → ev-cockpit-v2-debug** ZIP dosyasını indir; ZIP içindeki `app-debug.apk`'yi aç ve yükle.
4. Kurulan uygulamada mevcut **Ayarlar** sekmesine gir, **F1 — yeni harita ağırlıklı ekranı önizle (DEMO)** düğmesini seç.
5. DEMO bandını, yeni 5 sekmeyi, yeni MapLibre harita ile kartları kontrol et. Üstte **Boş veri** seçildiğinde hiçbir telemetri değeri uydurulmadan `—` görünmeli.
6. Geri düğmesiyle eski arayüze dönersin; önceki OBD/rota/şarj ekranları aynı yerde durur.

## Değişen 16 dosyanın TAM kaynağı
Aşağıdaki bloklarda **hiçbir satır atlanmadı**. Aynı isimli dosyaların son kaynak sürümleri yazılıdır.


---

### 1. `app/build.gradle.kts`

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "app.evcockpit"
    compileSdk = 36
    defaultConfig {
        applicationId = "app.evcockpit"
        minSdk = 28
        targetSdk = 35
        versionCode = 47
        versionName = "0.6.0-F1-preview"
        val ciSha = (System.getenv("GITHUB_SHA") ?: "unknown")
            .take(40).replace(Regex("[^0-9a-f]"), "")
        buildConfigField("String", "CI_COMMIT", "\"$ciSha\"")
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    // Public TEST certificate only. Never use it for production/release signing.
    System.getenv("EV_COCKPIT_PUBLIC_DEBUG_P12")?.takeIf { it.isNotBlank() }?.let { keyPath ->
        val shared = signingConfigs.create("persistentDebug") {
            storeFile = file(keyPath)
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        buildTypes.getByName("debug").signingConfig = shared
    }
}

dependencies {
    // Hilt, uygulama ve servisler arasında aynı konum deposunu paylaşır.
    implementation("com.google.dagger:hilt-android:2.57.1")
    kapt("com.google.dagger:hilt-compiler:2.57.1")
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("org.maplibre.gl:android-sdk:11.13.0")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.media3:media3-exoplayer:1.10.0")
    implementation("androidx.media3:media3-ui:1.10.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
```

---

### 2. `app/src/main/AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <queries>
        <!-- Package visibility only: no additional runtime or privileged permission. -->
        <package android:name="com.google.android.gms" />
        <package android:name="com.google.android.projection.gearhead" />
        <!-- Only public package visibility; Mola binary, components and permissions untouched. -->
        <package android:name="me.app.fermataX.auto.mola" />
        <package android:name="me.app.fermataX.auto.mola.debug" />
        <intent>
            <action android:name="android.intent.action.TTS_SERVICE" />
        </intent>
    </queries>

    <uses-feature android:name="android.hardware.bluetooth" android:required="false" />

    <uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />
    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:name=".CockpitApplication"
        android:allowBackup="false"
        android:label="ID.4 Yol Arkadaşı"
        android:theme="@android:style/Theme.Material.NoActionBar">
        <!-- F1: ayrı telefon önizlemesi; dışarıya açık değildir. -->
        <activity
            android:name=".ui.finalhmi.MapFirstDashboardActivity"
            android:exported="false"
            android:screenOrientation="fullSensor" />
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTop"
            android:screenOrientation="fullSensor"
            android:configChanges="orientation|screenSize|keyboardHidden">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <!-- External launchers can select existing pages; no Mola code/UI is modified. -->
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="evcockpit" android:host="open" />
            </intent-filter>
        </activity>

        <service
            android:name=".obd.ObdConnectionService"
            android:exported="false"
            android:foregroundServiceType="connectedDevice" />

        <service
            android:name=".location.LocationTrackingService"
            android:exported="false"
            android:foregroundServiceType="location" />

        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>
    </application>
</manifest>
```

---

### 3. `app/src/main/java/app/evcockpit/ui/RouteSettingsScreen.kt`

```kotlin
package app.evcockpit.ui

import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import app.evcockpit.ui.NightlyUpdatePanel
import app.evcockpit.ui.finalhmi.MapFirstDashboardActivity
import android.os.Build
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.evcockpit.navigation.EncryptedRouteKeyStore
import app.evcockpit.obd.ManualSocInput
import app.evcockpit.storage.VehicleProfileStore
import app.evcockpit.planner.model.VehicleProfile
import app.evcockpit.planner.model.SourcedValue
import app.evcockpit.location.GpsSnapshot
import kotlinx.coroutines.launch

@Composable
fun RouteSettingsScreen(
    gps: GpsSnapshot,
    darkTheme: Boolean,
    keepScreenOn: Boolean,
    onDarkTheme: (Boolean) -> Unit,
    onKeepScreenOn: (Boolean) -> Unit,
    manualSocPct: Double?,
    onSaveManualSoc: (Double) -> Unit,
    onClearManualSoc: () -> Unit,
    onStartGps: () -> Unit,
    onStopGps: () -> Unit,
) {
    val context = LocalContext.current
    val store = remember(context) { EncryptedRouteKeyStore(context) }
    val configured by store.isConfigured.collectAsState(initial = false)
    val mapTilerStore = remember(context) { MapTilerKeyStore(context) }
    val mapTilerKey by mapTilerStore.key.collectAsState(initial = null)
    var mapTilerInput by remember { mutableStateOf("") }
    var mapTilerStatus by remember { mutableStateOf<String?>(null) }
    val profileStore = remember(context) { VehicleProfileStore(context.applicationContext) }
    val vehicleProfile by profileStore.profile.collectAsState(initial = VehicleProfile.defaults())
    val scope = rememberCoroutineScope()
    var observedDcText by remember { mutableStateOf("") }
    var observedDcError by remember { mutableStateOf<String?>(null) }
    var input by remember { mutableStateOf("") }
    var manualInput by remember { mutableStateOf("") }
    var manualError by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var compatibility by remember { mutableStateOf(DeviceCompatibilityDiagnostics.capture(context)) }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Araç profili — VW ID.4 Pure 52 kWh (TAHMİN / DOĞRULANMADI)")
        OutlinedButton(
            onClick = {
                context.startActivity(Intent(context, MapFirstDashboardActivity::class.java))
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("F1 — yeni harita ağırlıklı ekranı önizle (DEMO)") }
        Text("Önizleme ayrı ekranda açılır. Eski rota, OBD ve şarj menüleri korunur.")
        Text("İhtiyatlı varsayılan DC: " + vehicleProfile.maxDcKw.value.toInt() +
            " kW • model yılı, batarya sıcaklığı ve SoC sonucu değiştirir.")
        Text("Kayıtlı ekran gözlemi: " +
            (vehicleProfile.observedDcKw?.value?.let { "%.1f kW".format(it) } ?: "yok"))
        OutlinedTextField(value = observedDcText,
            onValueChange = { observedDcText = it },
            label = { Text("Araç ekranında gördüğüm gerçek azami şarj gücü (kW)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            val v = observedDcText.trim().replace(',', '.').toDoubleOrNull()
            if (v == null || !v.isFinite() || v !in 1.0..350.0) {
                observedDcError = "1–350 kW arası bir araç ekranı gözlemi girin."
            } else {
                observedDcError = null
                scope.launch {
                    profileStore.save(vehicleProfile.copy(observedDcKw =
                        SourcedValue(v, "KULLANICI ARAÇ EKRANI GÖZLEMİ", false,
                            "Tek oturum değeri; gerçek fiziksel azami DC DOĞRULANMADI")))
                }
                observedDcText = ""
            }
        }) { Text("Gözlemi kaydet (tahmini eğriyi ölçekle)") }
        OutlinedButton(onClick = {
            scope.launch { profileStore.save(vehicleProfile.copy(observedDcKw = null)) }
        }, enabled = vehicleProfile.observedDcKw != null) { Text("Gözlemi sil") }
        OutlinedButton(onClick = {
            scope.launch { profileStore.reset() }
        }) { Text("Araç profili varsayılanına dön") }
        observedDcError?.let { Text(it) }
        Text("Gözlem şarj eğrisini oransal ölçekler; sürekli/garanti edilen güç değildir. " +
            "Model ve telefon üzerinde şarj tahmini DOĞRULANMADI.")
        Text("Manuel batarya SoC (OBD yanıt vermezse)")
        Text("Kayıtlı manuel SoC: " +
            (manualSocPct?.let { "%.1f %%".format(it) } ?: "yok"))
        OutlinedTextField(
            value = manualInput, onValueChange = { manualInput = it },
            label = { Text("Gösterge SoC (%) — 0-100") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = {
            val soc = ManualSocInput.parse(manualInput)
            if (soc == null) {
                manualError = "0 ile 100 arasında geçerli SoC girin."
            } else {
                onSaveManualSoc(soc)
                manualError = null
                manualInput = ""
            }
        }) { Text("Manuel SoC kaydet") }
        OutlinedButton(onClick = onClearManualSoc, enabled = manualSocPct != null) {
            Text("Manuel SoC sil")
        }
        manualError?.let { Text(it) }
        Text("OBD SoC yanıt verirse öncelik OBD'dedir; PID ölçeği doğrulanmadı etiketi korunur.")
        Text("Harita katmanları: sokak / açık / koyu ücretsiz; uydu ve topo için MapTiler anahtarı.")
        Text("MapTiler anahtarı: " +
            if (mapTilerKey.isNullOrBlank()) "kayıtlı değil" else "cihaza şifreli kaydedildi")
        OutlinedTextField(
            value = mapTilerInput,
            onValueChange = { mapTilerInput = it.take(256) },
            label = { Text("Kendi MapTiler API anahtarın (uydu ve topo)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(enabled = mapTilerInput.isNotBlank(), onClick = {
            scope.launch {
                try {
                    mapTilerStore.save(mapTilerInput)
                    mapTilerInput = ""
                    mapTilerStatus = "Anahtar kaydedildi; uydu/topo artık harita katmanından seçilebilir."
                } catch (_: Exception) {
                    mapTilerStatus = "Anahtar kaydedilemedi; biçimini veya cihaz güvenliğini kontrol et."
                }
            }
        }) { Text("MapTiler anahtarını şifreli kaydet") }
        if (!mapTilerKey.isNullOrBlank()) {
            OutlinedButton(onClick = {
                scope.launch {
                    try {
                        mapTilerStore.clear()
                        mapTilerInput = ""
                        mapTilerStatus = "MapTiler anahtarı silindi."
                    } catch (_: Exception) {
                        mapTilerStatus = "Anahtar silinemedi."
                    }
                }
            }) { Text("MapTiler anahtarını sil") }
        }
        mapTilerStatus?.let { Text(it) }
        Text("Anahtar MapTiler karo isteklerinde sağlayıcıya gönderilir. " +
            "Ücretsiz kullanım kotası sağlayıcı hesabına bağlıdır; karoların yüklenmesi garanti değil.")
        Text("Görünüm: sabit koyu tema • dinamik renk kullanılmıyor.")
        Row { Text("Ekranı açık tut"); Switch(checked = keepScreenOn, onCheckedChange = onKeepScreenOn) }
        Text("Ekranı açık tut seçimi yalnız uygulama açıkken geçerlidir; telefon üzerinde DOĞRULANMADI.")
        NightlyUpdatePanel()
        Text("Konum: ${gps.status}")
        Text("Hassasiyet: ${gps.accuracyMeters?.toInt()?.toString() ?: "--"} m • "
            + (gps.provider ?: "Konum sağlayıcı yok"))
        Button(onClick = onStartGps) { Text("GPS takibini başlat") }
        OutlinedButton(onClick = onStopGps) { Text("GPS takibini durdur") }
        Text("GPS izni bu düğmeyle istenir. Konum işlenmesi için görünür bildirimli konum servisi kullanılır.")
        Text("Arka plan servisleri için pil / otomatik başlatma ayarları")
        Text(BackgroundSettingsGuide.explanation(
            Build.MANUFACTURER, Build.DISPLAY, Build.FINGERPRINT))
        Text("Telefon / ROM uyumluluk durumu (yerel; hassas kimlik verisi yok)")
        Text(compatibility.safeReport())
        OutlinedButton(onClick = {
            compatibility = DeviceCompatibilityDiagnostics.capture(context)
        }) { Text("İzin ve sistem durumunu yenile") }
        OutlinedButton(onClick = {
            val clipboard = context.getSystemService(ClipboardManager::class.java)
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText(
                    "ID.4 uyumluluk özeti", compatibility.safeReport()))
                status = "Kimlik ve konum içermeyen uyumluluk özeti panoya kopyalandı."
            } else {
                status = "Pano kullanılamıyor; rapor ekranda görüntüleniyor."
            }
        }) { Text("Güvenli uyumluluk özetini kopyala") }
        if (!compatibility.preciseLocationGranted) {
            Text("Hassas konum izni yok: GPS başlangıçlı rota ve yeniden planlama " +
                "kullanılamayabilir. Sistem konum izninde 'Kesin' seçimini kontrol edin.")
        }
        OutlinedButton(onClick = {
            status = if (BackgroundSettingsGuide.openBatteryOptimizationSettings(context)) {
                "Pil optimizasyonu sistem sayfası istendi; ayarı kendiniz kontrol edin."
            } else "Pil optimizasyonu ayarı açılamadı; telefon Ayarlar'dan açın."
        }) { Text("Pil optimizasyonu ayarlarını aç") }
        OutlinedButton(onClick = {
            status = if (BackgroundSettingsGuide.openAutostartOrAppDetails(context)) {
                "Otomatik başlatma / uygulama bilgileri sayfası istendi."
            } else "Otomatik başlatma ayarı açılamadı; telefon Ayarlar'dan açın."
        }) { Text("Otomatik başlatma / uygulama ayarları") }
        Text("Bu düğmeler izni değiştirmez, yalnızca sistem sayfasını açar. " +
            "Arka planda sorunsuz çalışma DOĞRULANMADI.")
        Text("Rota servisi: openrouteservice (HeiGIT)")
        Text("API anahtarı: " + if (configured) "kayıtlı (doğrulanmadı)" else "kayıtlı değil")
        Text("Anahtarını kendi HeiGIT hesabından oluştur. Bu uygulama anahtar üretmez.")
        OutlinedButton(onClick = {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://account.heigit.org/")),
                )
            } catch (_: Exception) {
                status = "Hesap sayfası açılamadı."
            }
        }) { Text("HeiGIT hesap sayfasını aç") }
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Kendi API anahtarın") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                scope.launch {
                    try {
                        store.save(input)
                        input = "" // Remove plaintext from Compose state immediately.
                        status = "Anahtar Android Keystore + DataStore ile şifreli kaydedildi."
                    } catch (_: Exception) {
                        status = "Anahtar kaydedilemedi; geçerliliği ve cihaz güvenlik ayarlarını kontrol et."
                    }
                }
            },
            enabled = input.isNotBlank(),
        ) { Text("Anahtarı şifreli kaydet") }
        if (configured) {
            OutlinedButton(onClick = {
                scope.launch {
                    try {
                        store.clear()
                        input = ""
                        status = "Kaydedilmiş anahtar silindi."
                    } catch (_: Exception) {
                        status = "Anahtar silinemedi."
                    }
                }
            }) { Text("Kaydedilmiş anahtarı sil") }
        }
        status?.let { Text(it) }
        Text("Gizlilik: veri dosyasına yalnızca AES-GCM şifreli içerik yazılır. "
            + "Android Keystore anahtarı dışarı aktarılmaz. "
            + "Telefonun ele geçirilmesine karşı mutlak güvenlik garantisi yoktur.")
        Text("Anahtarın doğruluğu ancak HeiGIT isteğiyle test edilebilir; "
            + "API kotası ve servis erişilebilirliği sağlayıcıya bağlıdır.")
    }
}
```

---

### 4. `app/src/main/java/app/evcockpit/ui/finalhmi/CockpitDesignTokens.kt`

```kotlin
package app.evcockpit.ui.finalhmi

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * F1 ve sonraki F ekranları için TEK tasarım token kaynağı.
 * Buradaki renk ve ölçüler fiziksel araçtan okunan veri değildir.
 */
object CockpitDesignTokens {
    val background = Color(0xFF080E19)
    val panel = Color(0xFF121E30)
    val panelRaised = Color(0xFF1C2B41)
    val mapFallback = Color(0xFF0D1C2B)
    val outline = Color(0xFF30425A)
    val text = Color(0xFFF4F8FF)
    val secondary = Color(0xFFACC0D4)
    val accent = Color(0xFF1677FF)
    val accentSoft = Color(0xFF14345C)
    val regenerate = Color(0xFF32CF99)
    val traction = Color(0xFF1677FF)
    val caution = Color(0xFFFFC878)
    val error = Color(0xFFFF7B84)
    val neutralTrack = Color(0xFF42536A)
    val demoBanner = Color(0xFF4C2A06)
    val mutedScrim = Color(0xCC0A1423)

    val corner = RoundedCornerShape(18.dp)
    val cornerSmall = RoundedCornerShape(16.dp)
    val cornerTiny = RoundedCornerShape(10.dp)
    val cardOutlineWidth = 1.dp
    val spaceXXS = 3.dp
    val spaceXS = 6.dp
    val spaceS = 9.dp
    val spaceM = 12.dp
    val spaceL = 16.dp
    val spaceXL = 20.dp
    val spaceXXL = 28.dp
    val target = 56.dp
    val topBarMinHeight = 56.dp
    val tabBarHeight = 70.dp
    val bannerMinHeight = 36.dp
    val phoneMapHeight = 400.dp
    val compactMapHeight = 330.dp
    val wideWeatherHeight = 112.dp
    val speedCardMinHeight = 116.dp
    val metricCardMinHeight = 112.dp
    val chargingCardMinHeight = 140.dp
    val mapSummaryHeight = 72.dp
    val mapBorderRadius = 18.dp
    val mapAttributionPadding = 4.dp

    const val mapWeight = 0.65f
    const val dataWeight = 0.35f
    val expandedMinWidth = 840.dp
    val compactMaxWidth = 600.dp

    val speedText = 48.sp
    val metricNumber = 25.sp
    val headText = 21.sp
    val titleText = 17.sp
    val bodyText = 14.sp
    val labelText = 12.sp
    val tinyText = 11.sp
    val tabText = 13.sp
    val fontWeightStrong = FontWeight.SemiBold
}

private val finalColors = darkColorScheme(
    primary = CockpitDesignTokens.accent,
    background = CockpitDesignTokens.background,
    surface = CockpitDesignTokens.panel,
    onBackground = CockpitDesignTokens.text,
    onSurface = CockpitDesignTokens.text,
    onPrimary = CockpitDesignTokens.text,
    secondary = CockpitDesignTokens.secondary,
    outline = CockpitDesignTokens.outline,
    error = CockpitDesignTokens.error,
)

private val finalType = Typography(
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = CockpitDesignTokens.bodyText,
        fontWeight = FontWeight.Normal,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = CockpitDesignTokens.bodyText,
        fontWeight = FontWeight.Normal,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = CockpitDesignTokens.titleText,
        fontWeight = FontWeight.Medium,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = CockpitDesignTokens.titleText,
        fontWeight = FontWeight.SemiBold,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = CockpitDesignTokens.headText,
        fontWeight = FontWeight.SemiBold,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = CockpitDesignTokens.labelText,
        fontWeight = FontWeight.Normal,
    ),
)

@Composable
fun FinalCockpitTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = finalColors, typography = finalType, content = content)
}
```

---

### 5. `app/src/main/java/app/evcockpit/ui/finalhmi/DashboardUiState.kt`

```kotlin
package app.evcockpit.ui.finalhmi

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** DEĞER + KAYNAK + ZAMAN. Null asla sıfır sayılmaz. */
enum class DashboardOrigin {
    NONE, SYSTEM, GPS, OPEN_METEO, OBD_VERIFIED, MANUAL, MODEL, DEMO,
}

data class StampedValue<T>(
    val value: T? = null,
    val source: DashboardOrigin = DashboardOrigin.NONE,
    val timestampEpochMs: Long? = null,
) {
    fun freshValue(nowEpochMs: Long, maxAgeMs: Long): T? {
        val time = timestampEpochMs ?: return null
        if (source == DashboardOrigin.NONE || value == null || maxAgeMs < 0L) return null
        val age = nowEpochMs - time
        return if (age in 0L..maxAgeMs) value else null
    }
}

data class RouteWeatherSample(
    val distanceAlongKm: Double,
    val temperatureC: StampedValue<Double> = StampedValue(),
    val rainExpected: StampedValue<Boolean> = StampedValue(),
)

data class NextChargeUiState(
    val stationName: StampedValue<String> = StampedValue(),
    val distanceKm: StampedValue<Double> = StampedValue(),
    val chargingMinutes: StampedValue<Double> = StampedValue(),
    val targetSocPercent: StampedValue<Double> = StampedValue(),
    val costLabel: StampedValue<String> = StampedValue(),
)

/** Bütün alanlar bilinmiyor olabilir; boş durum normal ve güvenlidir. */
data class DashboardUiState(
    val demo: Boolean = false,
    val clock: StampedValue<String> = StampedValue(),
    val outdoorTemperatureC: StampedValue<Double> = StampedValue(),
    val speedKmh: StampedValue<Double> = StampedValue(),
    val socPercent: StampedValue<Double> = StampedValue(),
    val estimatedRangeKm: StampedValue<Double> = StampedValue(),
    val consumptionKwhPer100Km: StampedValue<Double> = StampedValue(),
    val powerKw: StampedValue<Double> = StampedValue(),
    val arrivalMinutes: StampedValue<Double> = StampedValue(),
    val remainingDistanceKm: StampedValue<Double> = StampedValue(),
    val arrivalSocPercent: StampedValue<Double> = StampedValue(),
    val chargeTotalLabel: StampedValue<String> = StampedValue(),
    val maneuverText: StampedValue<String> = StampedValue(),
    val maneuverDistanceKm: StampedValue<Double> = StampedValue(),
    val weatherSamples: List<RouteWeatherSample> = emptyList(),
    val nextCharge: NextChargeUiState = NextChargeUiState(),
)

/**
 * Saf hesaplar: kapasite parametre olarak gelir; 52 kWh burada KESİNLİKLE sabitlenmez.
 * Hesap olumsuz/gerçekdışı sonuç verirse gösterilecek değer null olur.
 */
object DashboardMath {
    fun estimatedSocPercent(
        startSocPercent: Double?,
        modelEnergySpentKwh: Double?,
        usableCapacityKwh: Double?,
    ): Double? {
        val start = startSocPercent ?: return null
        val spent = modelEnergySpentKwh ?: return null
        val capacity = usableCapacityKwh ?: return null
        if (!start.isFinite() || start !in 0.0..100.0 ||
            !spent.isFinite() || spent < 0.0 ||
            !capacity.isFinite() || capacity <= 0.0) return null
        val result = start - 100.0 * spent / capacity
        return result.takeIf { it.isFinite() && it in 0.0..100.0 }
    }

    fun estimatedRangeKm(
        socPercent: Double?,
        usableCapacityKwh: Double?,
        consumptionKwhPer100Km: Double?,
    ): Double? {
        val soc = socPercent ?: return null
        val capacity = usableCapacityKwh ?: return null
        val consumption = consumptionKwhPer100Km ?: return null
        if (!soc.isFinite() || soc !in 0.0..100.0 ||
            !capacity.isFinite() || capacity <= 0.0 ||
            !consumption.isFinite() || consumption <= 0.0) return null
        val range = (soc / 100.0 * capacity) / consumption * 100.0
        return range.takeIf { it.isFinite() && it >= 0.0 }
    }

    fun speedKmhFromGpsMetersPerSecond(metersPerSecond: Double?): Double? =
        metersPerSecond?.takeIf { it.isFinite() && it in 0.0..83.333333 }
            ?.times(3.6)
}

data class PricedStop(val priceTl: Double?)

/** Bilinmeyen bedel 0 TL değil. Kısmi toplam, kesin toplam gibi gösterilmez. */
object ChargeCostSummary {
    fun label(stops: List<PricedStop>): String? {
        if (stops.isEmpty()) return null
        if (stops.any { it.priceTl != null && (!it.priceTl.isFinite() || it.priceTl < 0.0) })
            return null
        val known = stops.mapNotNull { it.priceTl }
        if (known.isEmpty()) return null
        val amount = DashboardFormat.oneDecimal(known.sum())
        val missing = stops.size - known.size
        return if (missing == 0) "$amount TL (tahmini)" else
            "en az $amount TL ($missing durakta fiyat yok)"
    }
}

/** Sadece görünüm biçimlemesi, sayısal ölçüm üretmez. */
object DashboardFormat {
    private val locale = Locale.forLanguageTag("tr-TR")
    fun oneDecimal(value: Double): String =
        String.format(locale, "%.1f", value)
    fun whole(value: Double): String =
        String.format(locale, "%.0f", value)
    fun number(value: StampedValue<Double>, suffix: String = ""): String =
        value.value?.takeIf { it.isFinite() }?.let { whole(it) + suffix }
            ?: ("—" + suffix)
    fun time(nowEpochMs: Long): String =
        DateTimeFormatter.ofPattern("HH:mm", locale)
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(nowEpochMs))
}

/** F1 gerçek mod girdileri BOŞ; F3–F5'te repository'lerden doldurulacak. */
data class RealDashboardInput(
    val gpsSpeedMetersPerSecond: StampedValue<Double> = StampedValue(),
    val meteoTemperatureC: StampedValue<Double> = StampedValue(),
    val obdVerifiedSocPercent: StampedValue<Double> = StampedValue(),
    val obdVerifiedPowerKw: StampedValue<Double> = StampedValue(),
    val manualStartSocPercent: StampedValue<Double> = StampedValue(),
    val modelEnergySpentKwh: StampedValue<Double> = StampedValue(),
    val modelConsumptionKwhPer100Km: StampedValue<Double> = StampedValue(),
    val vehicleUsableCapacityKwh: StampedValue<Double> = StampedValue(),
    val routeProgressReliable: Boolean = false,
)

/** DEMO verisini kesinlikle kabul ETMEYEN gerçek veri eşlemesi. */
object RealDashboardMapper {
    private const val GPS_MAX_AGE = 10_000L
    private const val WEATHER_MAX_AGE = 60L * 60L * 1000L
    private const val OBD_MAX_AGE = 5_000L
    private const val MODEL_MAX_AGE = 60_000L
    private const val MANUAL_MAX_AGE = 90L * 24L * 60L * 60L * 1000L

    private fun StampedValue<Double>.valueFrom(
        origin: DashboardOrigin, now: Long, ttl: Long,
    ): Double? = freshValue(now, ttl)?.takeIf { source == origin && it.isFinite() }

    fun map(data: RealDashboardInput, nowEpochMs: Long): DashboardUiState {
        val speed = data.gpsSpeedMetersPerSecond.valueFrom(
            DashboardOrigin.GPS, nowEpochMs, GPS_MAX_AGE)
        val meteo = data.meteoTemperatureC.valueFrom(
            DashboardOrigin.OPEN_METEO, nowEpochMs, WEATHER_MAX_AGE)
        val obd = data.obdVerifiedSocPercent.valueFrom(
            DashboardOrigin.OBD_VERIFIED, nowEpochMs, OBD_MAX_AGE)
            ?.takeIf { it in 0.0..100.0 }
        val manual = data.manualStartSocPercent.valueFrom(
            DashboardOrigin.MANUAL, nowEpochMs, MANUAL_MAX_AGE)
            ?.takeIf { it in 0.0..100.0 }
        val consumed = data.modelEnergySpentKwh.valueFrom(
            DashboardOrigin.MODEL, nowEpochMs, MODEL_MAX_AGE)
            ?.takeIf { it >= 0.0 }
        val consumption = data.modelConsumptionKwhPer100Km.valueFrom(
            DashboardOrigin.MODEL, nowEpochMs, MODEL_MAX_AGE)
            ?.takeIf { it > 0.0 }
        // Kapasite her zaman aktif araç profilinden; F1'de profil veri akışı bağlı değil.
        val capacity = data.vehicleUsableCapacityKwh.freshValue(
            nowEpochMs, MANUAL_MAX_AGE)?.takeIf {
                data.vehicleUsableCapacityKwh.source in setOf(
                    DashboardOrigin.MODEL, DashboardOrigin.MANUAL) &&
                    it.isFinite() && it > 0.0
            }
        val estimatedSoc = if (obd == null && manual != null &&
            data.routeProgressReliable && consumed != null)
            DashboardMath.estimatedSocPercent(manual, consumed, capacity) else null
        val usableSoc = obd ?: estimatedSoc ?: manual
        val socSource = when {
            obd != null -> DashboardOrigin.OBD_VERIFIED
            estimatedSoc != null -> DashboardOrigin.MODEL
            manual != null -> DashboardOrigin.MANUAL
            else -> DashboardOrigin.NONE
        }
        fun <T> value(raw: T?, source: DashboardOrigin): StampedValue<T> =
            if (raw == null) StampedValue() else StampedValue(raw, source, nowEpochMs)
        return DashboardUiState(
            clock = StampedValue(DashboardFormat.time(nowEpochMs),
                DashboardOrigin.SYSTEM, nowEpochMs),
            speedKmh = value(DashboardMath.speedKmhFromGpsMetersPerSecond(speed),
                DashboardOrigin.GPS),
            outdoorTemperatureC = value(meteo, DashboardOrigin.OPEN_METEO),
            socPercent = value(usableSoc, socSource),
            estimatedRangeKm = value(
                DashboardMath.estimatedRangeKm(usableSoc, capacity, consumption),
                DashboardOrigin.MODEL),
            consumptionKwhPer100Km = value(consumption, DashboardOrigin.MODEL),
            powerKw = value(data.obdVerifiedPowerKw.valueFrom(
                DashboardOrigin.OBD_VERIFIED, nowEpochMs, OBD_MAX_AGE),
                DashboardOrigin.OBD_VERIFIED),
        )
    }
}

/**
 * YALNIZCA ayrı önizleme moduna ait örnekler.
 * RealDashboardMapper bunu çağırmaz, ortak bir depo/OBD durumunu değiştirmez.
 */
object DemoDashboardMapper {
    fun map(nowEpochMs: Long): DashboardUiState {
        fun <T> demo(value: T): StampedValue<T> =
            StampedValue(value, DashboardOrigin.DEMO, nowEpochMs)
        val capacity = 52.0 // Yalnız F1 DEMO araca özgü örnek; gerçek mod profil deposunu kullanır.
        val soc = 74.0
        val consumption = 17.2
        return DashboardUiState(
            demo = true,
            clock = StampedValue(DashboardFormat.time(nowEpochMs),
                DashboardOrigin.SYSTEM, nowEpochMs),
            outdoorTemperatureC = demo(18.0),
            speedKmh = demo(83.0),
            socPercent = demo(soc),
            estimatedRangeKm = demo(
                DashboardMath.estimatedRangeKm(soc, capacity, consumption) ?: 0.0),
            consumptionKwhPer100Km = demo(consumption),
            powerKw = demo(21.0),
            arrivalMinutes = demo(88.0),
            remainingDistanceKm = demo(110.0),
            arrivalSocPercent = demo(31.0),
            chargeTotalLabel = demo("en az 124,0 TL (1 durakta fiyat yok)"),
            weatherSamples = listOf(
                RouteWeatherSample(25.0, demo(17.0), demo(false)),
                RouteWeatherSample(50.0, demo(15.0), demo(true)),
                RouteWeatherSample(75.0, demo(13.0), demo(false)),
            ),
            nextCharge = NextChargeUiState(
                stationName = demo("Örnek DC istasyonu"),
                distanceKm = demo(48.0),
                chargingMinutes = demo(24.0),
                targetSocPercent = demo(75.0),
                costLabel = demo("124,0 TL (örnek, kısmi)"),
            ),
        )
    }
}
```

---

### 6. `app/src/main/java/app/evcockpit/ui/finalhmi/DashboardRepository.kt`

```kotlin
package app.evcockpit.ui.finalhmi

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * F1 deposu salt boş, bağımsız durum taşır.
 * OBD, rota, API veya konum kaynağına BAĞLANMAZ.
 * F3–F5 veri bağlayıcıları eklendiğinde yalnız gerçek girdiler buradan akacak.
 */
class DashboardRepository {
    private val mutable = MutableStateFlow(RealDashboardInput())
    val realInputs: StateFlow<RealDashboardInput> = mutable

    // F1'de veri yazıcı yöntem bulunmaması, önizleme verilerinin sürüşe sızmasını engeller.
}
```

---

### 7. `app/src/main/java/app/evcockpit/ui/finalhmi/MapFirstDashboardViewModel.kt`

```kotlin
package app.evcockpit.ui.finalhmi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Görünümden bağımsız zamanlayıcı ve Demo/Boş geçişi.
 * Demo, ayrı mapper ile yalnız bellekte oluşturulur; kayıt/OBD servisine gönderilmez.
 */
class MapFirstDashboardViewModel : ViewModel() {
    private val repository = DashboardRepository()
    private val mutable = MutableStateFlow(DemoDashboardMapper.map(System.currentTimeMillis()))
    val state: StateFlow<DashboardUiState> = mutable
    private var showDemo = true

    init {
        viewModelScope.launch {
            while (isActive) {
                refresh()
                delay(15_000L)
            }
        }
    }

    fun showDemo(enabled: Boolean) {
        showDemo = enabled
        refresh()
    }

    private fun refresh() {
        val now = System.currentTimeMillis()
        mutable.value = if (showDemo) DemoDashboardMapper.map(now)
            else RealDashboardMapper.map(repository.realInputs.value, now)
    }
}
```

---

### 8. `app/src/main/java/app/evcockpit/ui/finalhmi/MapHeroPane.kt`

```kotlin
package app.evcockpit.ui.finalhmi

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.evcockpit.ui.MapLayer
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

/**
 * F1: Gerçek MapLibre temel haritası (OpenFreeMap stil).
 * GPS, adres sorgusu, rota veya şarj verisiyle henüz BAĞLANTISI YOK.
 * Haritada rastgele rota, işaretçi ve trafik rengi çizilmez.
 */
@Composable
fun MapHeroPane(
    state: DashboardUiState,
    modifier: Modifier = Modifier,
    onOpenRouteTab: () -> Unit = {},
) {
    val t = CockpitDesignTokens
    val preview = LocalInspectionMode.current
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var layer by remember { mutableStateOf(MapLayer.DARK) }
    var layerMenu by remember { mutableStateOf(false) }
    val map = remember { mutableStateOf<MapLibreMap?>(null) }

    // F1'de mevcut MapLibre Android SDK'sını kullanıyoruz; kaynak ağ bağlantısı
    // yalnız harita stil/karolarına aittir, araç veya kullanıcı GPS verisi gönderilmez.
    val mapView = remember(context, preview) {
        if (preview) null else {
            MapLibre.getInstance(context)
            MapView(context).apply {
                onCreate(null)
                getMapAsync { instance ->
                    map.value = instance
                    instance.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        LatLng(39.0, 35.0), 5.2))
                    instance.setStyle(MapLayer.DARK.styleUrl)
                }
            }
        }
    }
    DisposableEffect(owner, mapView) {
        if (mapView == null) return@DisposableEffect onDispose { }
        var started = false
        var resumed = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    if (!started) { mapView.onStart(); started = true }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (!resumed) { mapView.onResume(); resumed = true }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    if (resumed) { mapView.onPause(); resumed = false }
                }
                Lifecycle.Event.ON_STOP -> {
                    if (started) { mapView.onStop(); started = false }
                }
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            mapView.onStart(); started = true
        }
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            mapView.onResume(); resumed = true
        }
        onDispose {
            owner.lifecycle.removeObserver(observer)
            if (resumed) mapView.onPause()
            if (started) mapView.onStop()
            mapView.onDestroy()
            map.value = null
        }
    }
    LaunchedEffect(layer, map.value) {
        val active = map.value ?: return@LaunchedEffect
        if (layer.styleUrl.isNotBlank()) active.setStyle(layer.styleUrl)
    }

    Surface(
        modifier = modifier,
        shape = t.corner,
        color = t.mapFallback,
        border = androidx.compose.foundation.BorderStroke(t.cardOutlineWidth, t.outline),
    ) {
        Box(Modifier.fillMaxSize()) {
            if (mapView != null) {
                AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
            } else {
                Box(Modifier.fillMaxSize().background(t.mapFallback),
                    contentAlignment = Alignment.Center) {
                    Text("MapLibre önizleme alanı\nRota ve istasyon işaretçisi yok",
                        color = t.secondary,
                        fontSize = t.bodyText)
                }
            }

            // Üstteki iki alan F1'de sahte adres sorgusu yapmaz; Rota sekmesine yönlendirir.
            Row(
                Modifier.align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(t.mutedScrim)
                    .padding(t.spaceXS),
                horizontalArrangement = Arrangement.spacedBy(t.spaceXS),
            ) {
                OutlinedButton(
                    onClick = onOpenRouteTab,
                    modifier = Modifier.weight(1f).heightIn(min = t.target),
                    shape = t.cornerSmall,
                ) {
                    Text("Nereden  —", color = t.text, fontSize = t.bodyText,
                        maxLines = 1)
                }
                OutlinedButton(
                    onClick = onOpenRouteTab,
                    modifier = Modifier.weight(1f).heightIn(min = t.target),
                    shape = t.cornerSmall,
                ) {
                    Text("Nereye  —", color = t.text, fontSize = t.bodyText,
                        maxLines = 1)
                }
            }

            // Gerçek manevra verisi F2'de bağlanacak; yoksa yön çizimi uydurulmaz.
            if (state.maneuverText.value != null &&
                state.maneuverText.timestampEpochMs != null) {
                Surface(
                    modifier = Modifier.align(Alignment.TopStart)
                        .padding(top = t.mapSummaryHeight, start = t.spaceS),
                    shape = t.cornerSmall,
                    color = t.mutedScrim,
                ) {
                    Column(Modifier.padding(t.spaceS)) {
                        Text("YÖNLENDİRME", color = t.accent, fontSize = t.labelText)
                        Text(state.maneuverText.value ?: "—", color = t.text)
                        Text(DashboardFormat.number(state.maneuverDistanceKm, " km sonra"),
                            color = t.secondary, fontSize = t.bodyText)
                    }
                }
            }

            Column(
                Modifier.align(Alignment.CenterStart)
                    .padding(t.spaceXS),
                verticalArrangement = Arrangement.spacedBy(t.spaceXS),
            ) {
                @Composable fun mapButton(
                    symbol: String,
                    description: String,
                    enabled: Boolean,
                    action: () -> Unit,
                ) {
                    OutlinedButton(
                        onClick = action,
                        enabled = enabled,
                        contentPadding = PaddingValues(t.spaceXXS),
                        shape = t.cornerSmall,
                        modifier = Modifier.sizeIn(minWidth = t.target, minHeight = t.target)
                            .semantics { contentDescription = description },
                    ) { Text(symbol, color = if (enabled) t.text else t.secondary,
                        fontSize = t.headText) }
                }
                mapButton("+", "Haritayı yakınlaştır", map.value != null) {
                    map.value?.animateCamera(CameraUpdateFactory.zoomIn())
                }
                mapButton("−", "Haritayı uzaklaştır", map.value != null) {
                    map.value?.animateCamera(CameraUpdateFactory.zoomOut())
                }
                mapButton("◎", "Konum (F3'te bağlanacak)", false) { }
                Box {
                    mapButton("▤", "Harita katmanını seç", true) { layerMenu = true }
                    DropdownMenu(
                        expanded = layerMenu,
                        onDismissRequest = { layerMenu = false },
                    ) {
                        val options = listOf(
                            "Sokak" to MapLayer.LIBERTY,
                            "Uydu" to MapLayer.SATELLITE,
                            "Arazi" to MapLayer.TOPOGRAPHIC,
                            "Koyu" to MapLayer.DARK,
                        )
                        options.forEach { (title, option) ->
                            DropdownMenuItem(
                                text = { Text(if (option.styleUrl.isBlank())
                                    "$title • API anahtarı F2" else title) },
                                enabled = option.styleUrl.isNotBlank(),
                                onClick = { layer = option; layerMenu = false },
                                modifier = Modifier.heightIn(min = t.target),
                            )
                        }
                    }
                }
            }

            Text(
                "Türkiye genel haritası • GPS konumu değil",
                color = t.secondary,
                fontSize = t.tinyText,
                modifier = Modifier.align(Alignment.Center).background(t.mutedScrim)
                    .padding(t.spaceXS),
            )

            // Harita altında tek yatay özet; fiyatı bilinmeyen durak asla 0 TL sayılmaz.
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(t.mutedScrim)
                    .padding(t.spaceXS),
                verticalArrangement = Arrangement.spacedBy(t.spaceXXS),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(t.spaceS)) {
                    MapSummaryValue(
                        "Varış", DashboardFormat.number(state.arrivalMinutes, " dk"),
                        Modifier.weight(1f))
                    MapSummaryValue(
                        "Kalan", DashboardFormat.number(state.remainingDistanceKm, " km"),
                        Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(t.spaceS)) {
                    MapSummaryValue(
                        "Varış SoC (tahmini)",
                        DashboardFormat.number(state.arrivalSocPercent, " %"),
                        Modifier.weight(1f))
                    MapSummaryValue(
                        "Şarj ücreti (tahmini)",
                        state.chargeTotalLabel.value ?: "—",
                        Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MapSummaryValue(label: String, value: String, modifier: Modifier) {
    val t = CockpitDesignTokens
    Column(modifier) {
        Text(label, color = t.secondary, fontSize = t.labelText, maxLines = 1)
        Text(value, color = t.text, fontSize = t.bodyText, maxLines = 1)
    }
}
```

---

### 9. `app/src/main/java/app/evcockpit/ui/finalhmi/TripMetricsPane.kt`

```kotlin
package app.evcockpit.ui.finalhmi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

/**
 * Hız yalnız doğrulanmış GPS metre/saniye verisinden gelir;
 * F1 DEMO sayıları ekranda büyük DEMO şeridiyle ayrılır.
 */
@Composable
fun TripMetricsPane(state: DashboardUiState, modifier: Modifier = Modifier) {
    val t = CockpitDesignTokens
    Column(modifier, verticalArrangement = Arrangement.spacedBy(t.spaceS)) {
        Surface(color = t.panel, shape = t.corner,
            border = androidx.compose.foundation.BorderStroke(t.cardOutlineWidth, t.outline)) {
            Column(Modifier.fillMaxWidth().padding(t.spaceM),
                verticalArrangement = Arrangement.spacedBy(t.spaceXXS)) {
                Text(if (state.demo) "HIZ • DEMO" else "HIZ • GPS",
                    color = t.secondary, fontSize = t.labelText)
                Text(DashboardFormat.number(state.speedKmh, " km/sa"),
                    color = t.text, fontSize = t.speedText,
                    fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }

        Surface(color = t.panel, shape = t.corner,
            border = androidx.compose.foundation.BorderStroke(t.cardOutlineWidth, t.outline)) {
            Column(Modifier.padding(t.spaceS),
                verticalArrangement = Arrangement.spacedBy(t.spaceS)) {
                Row(horizontalArrangement = Arrangement.spacedBy(t.spaceS)) {
                    InfoMetric("Batarya" + when (state.socPercent.source) {
                        DashboardOrigin.MANUAL -> " (manuel)"
                        DashboardOrigin.MODEL -> " (tahmini)"
                        DashboardOrigin.DEMO -> " (DEMO)"
                        else -> ""
                    }, DashboardFormat.number(state.socPercent, " %"),
                        Modifier.weight(1f))
                    InfoMetric("Menzil (tahmini)",
                        DashboardFormat.number(state.estimatedRangeKm, " km"),
                        Modifier.weight(1f))
                }
                val soc = state.socPercent.value?.takeIf { it.isFinite() &&
                    it in 0.0..100.0 }
                LinearProgressIndicator(
                    progress = { ((soc ?: 0.0) / 100.0).toFloat() },
                    modifier = Modifier.fillMaxWidth().height(t.spaceXS),
                    color = if (soc == null) t.neutralTrack else t.accent,
                    trackColor = t.neutralTrack,
                )
                if (soc == null) {
                    Text("Batarya seviyesi: —", color = t.secondary,
                        fontSize = t.labelText)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(t.spaceS)) {
                    InfoMetric("Tüketim (tahmini)",
                        state.consumptionKwhPer100Km.value?.takeIf { it.isFinite() }
                            ?.let { DashboardFormat.oneDecimal(it) + " kWh/100 km" }
                            ?: "— kWh/100 km", Modifier.weight(1f))
                    val power = state.powerKw.value?.takeIf { it.isFinite() }
                    InfoMetric("Anlık güç",
                        power?.let { DashboardFormat.oneDecimal(it) + " kW" } ?: "— kW",
                        Modifier.weight(1f),
                        valueIsRegeneration = power != null && power < 0.0)
                }
            }
        }
    }
}

@Composable
private fun InfoMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueIsRegeneration: Boolean = false,
) {
    val t = CockpitDesignTokens
    Column(
        modifier.background(t.panelRaised, t.cornerSmall)
            .heightIn(min = t.metricCardMinHeight)
            .padding(t.spaceS),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = t.secondary, fontSize = t.labelText, maxLines = 2)
        Text(value, color = if (valueIsRegeneration) t.regenerate else t.text,
            fontSize = t.titleText, maxLines = 2, fontWeight = FontWeight.SemiBold)
    }
}
```

---

### 10. `app/src/main/java/app/evcockpit/ui/finalhmi/RouteWeatherStrip.kt`

```kotlin
package app.evcockpit.ui.finalhmi

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * F1 hava şeridi: Open-Meteo çağrısı yapmaz, var olan zamanlı veriyi çizer.
 * Uydurma güneş/yağmur ikonu ve trafik lejantı bulunmaz.
 */
@Composable
fun RouteWeatherStrip(state: DashboardUiState, modifier: Modifier = Modifier) {
    val t = CockpitDesignTokens
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = t.panel,
        shape = t.corner,
        border = androidx.compose.foundation.BorderStroke(t.cardOutlineWidth, t.outline),
    ) {
        Column(Modifier.padding(t.spaceM),
            verticalArrangement = Arrangement.spacedBy(t.spaceS)) {
            Text("ROTA BOYUNCA HAVA", color = t.text,
                fontSize = t.titleText)
            if (state.weatherSamples.isEmpty()) {
                Text("Sıcaklık: — • Yağış: —", color = t.secondary, fontSize = t.bodyText)
                Text("Hava verileri F4 aşamasında bağlanacak.",
                    color = t.secondary, fontSize = t.labelText)
            } else {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(t.spaceS),
                ) {
                    state.weatherSamples.forEach { sample ->
                        Column(
                            Modifier.background(t.panelRaised, t.cornerSmall)
                                .widthIn(min = t.target).padding(t.spaceS),
                            verticalArrangement = Arrangement.spacedBy(t.spaceXS),
                        ) {
                            Text("≈" + DashboardFormat.whole(sample.distanceAlongKm) + " km",
                                color = t.accent, fontSize = t.labelText)
                            val temp = sample.temperatureC.value?.takeIf { it.isFinite() }
                                ?.let { DashboardFormat.whole(it) + " °C" } ?: "— °C"
                            Text(temp, color = t.text, fontSize = t.titleText)
                            Text(
                                when (sample.rainExpected.value) {
                                    true -> "Yağış uyarısı"
                                    false -> "Yağış uyarısı yok"
                                    null -> "Yağış: —"
                                }, color = t.secondary, fontSize = t.labelText)
                        }
                    }
                }
            }
        }
    }
}
```

---

### 11. `app/src/main/java/app/evcockpit/ui/finalhmi/ChargePlanCard.kt`

```kotlin
package app.evcockpit.ui.finalhmi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** İstasyon/plan henüz bağlanmadıysa bütün bilgiler — görünür. */
@Composable
fun ChargePlanCard(state: DashboardUiState, modifier: Modifier = Modifier) {
    val t = CockpitDesignTokens
    val charge = state.nextCharge
    Surface(
        modifier.fillMaxWidth(),
        color = t.panel,
        shape = t.corner,
        border = androidx.compose.foundation.BorderStroke(t.cardOutlineWidth, t.outline),
    ) {
        Column(
            Modifier.padding(t.spaceM),
            verticalArrangement = Arrangement.spacedBy(t.spaceS),
        ) {
            Text("SIRADAKİ ŞARJ PLANI", color = t.text, fontSize = t.titleText)
            Text(charge.stationName.value ?: "—", color = t.accent,
                fontSize = t.bodyText)
            Row(horizontalArrangement = Arrangement.spacedBy(t.spaceS)) {
                ChargeDetail("Mesafe", DashboardFormat.number(charge.distanceKm, " km"),
                    Modifier.weight(1f))
                ChargeDetail("Süre (tahmini)",
                    DashboardFormat.number(charge.chargingMinutes, " dk"),
                    Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(t.spaceS)) {
                ChargeDetail("Hedef SoC", DashboardFormat.number(
                    charge.targetSocPercent, " %"), Modifier.weight(1f))
                ChargeDetail("Ücret (tahmini)", charge.costLabel.value ?: "—",
                    Modifier.weight(1f))
            }
            Text("Boş/eksik tarife 0 TL değildir. Gerçek plan F4'te bağlanacak.",
                color = t.secondary, fontSize = t.labelText)
        }
    }
}

@Composable
private fun ChargeDetail(label: String, value: String, modifier: Modifier = Modifier) {
    val t = CockpitDesignTokens
    Column(modifier.background(t.panelRaised, t.cornerSmall)
        .padding(t.spaceS),
        verticalArrangement = Arrangement.spacedBy(t.spaceXS)) {
        Text(label, fontSize = t.labelText, color = t.secondary)
        Text(value, fontSize = t.bodyText, color = t.text, maxLines = 2)
    }
}
```

---

### 12. `app/src/main/java/app/evcockpit/ui/finalhmi/MapFirstDashboardScreen.kt`

```kotlin
package app.evcockpit.ui.finalhmi

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize

enum class FinalDashboardTab(val titleTr: String) {
    HARITA("Harita"),
    ROTA("Rota"),
    SARJ("Şarj"),
    ENERJI("Enerji"),
    AYARLAR("Ayarlar"),
}

object F1DashboardLayoutPolicy {
    /**
     * Geniş pencere = en az 840dp ve yatayda yüksekliğinden geniş.
     * Varsayımsal piksel oranı değil, gerçek kullanılabilir dp.
     */
    fun useWideMapFirst(widthDp: Int, heightDp: Int): Boolean =
        widthDp >= 840 && widthDp > heightDp

    val tabs: List<String> = FinalDashboardTab.entries.map { it.titleTr }
}

/**
 * 5 sekmeli F1 ekranı; yalnız HARİTA açık. Diğer sekmeler F2'ye kadar
 * açıklamalı yer tutucudur. A–C arka plan işlevleri eski ekranlarda korunur.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun MapFirstDashboardScreen(
    state: DashboardUiState,
    onDemoChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = CockpitDesignTokens
    var selected by rememberSaveable { mutableStateOf(FinalDashboardTab.HARITA) }
    Column(modifier.fillMaxSize().background(t.background)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val compactHeader = maxWidth < t.compactMaxWidth
            DashboardTopStatus(state, compactHeader, onDemoChange)
        }

        if (state.demo) {
            Row(
                Modifier.fillMaxWidth().background(t.demoBanner)
                    .heightIn(min = t.bannerMinHeight)
                    .padding(horizontal = t.spaceM, vertical = t.spaceXS),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text("DEMO — GERÇEK VERİ DEĞİL",
                    color = t.caution, fontWeight = FontWeight.Bold,
                    fontSize = t.bodyText)
            }
        }

        BoxWithConstraints(
            Modifier.weight(1f).fillMaxWidth()
                .padding(horizontal = t.spaceS, vertical = t.spaceXS),
        ) {
            // Material3 WindowSizeClass, Android'in dp tabanlı kompakt/orta/geniş
            // sınırlarını kullanır. Ayrıca yüksekliği kontrol ederek katlanır
            // ekranlarda aşırı dar yatay pencereleri iki sütuna zorlamayız.
            val windowClass = WindowSizeClass.calculateFromSize(DpSize(maxWidth, maxHeight))
            val wide = windowClass.widthSizeClass == WindowWidthSizeClass.Expanded &&
                F1DashboardLayoutPolicy.useWideMapFirst(
                    maxWidth.value.toInt(), maxHeight.value.toInt())

            if (selected == FinalDashboardTab.HARITA) {
                if (wide) {
                    Row(
                        Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(t.spaceS),
                    ) {
                        MapHeroPane(
                            state = state,
                            onOpenRouteTab = { selected = FinalDashboardTab.ROTA },
                            modifier = Modifier.weight(t.mapWeight).fillMaxHeight(),
                        )
                        Column(
                            Modifier.weight(t.dataWeight).fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(t.spaceS),
                        ) {
                            DashboardRightPane(state)
                        }
                    }
                } else {
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(t.spaceS),
                    ) {
                        MapHeroPane(
                            state = state,
                            onOpenRouteTab = { selected = FinalDashboardTab.ROTA },
                            modifier = Modifier.fillMaxWidth().height(t.phoneMapHeight),
                        )
                        DashboardRightPane(state)
                    }
                }
            } else {
                Surface(
                    color = t.panel, shape = t.corner,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Column(
                        Modifier.padding(t.spaceXL),
                        verticalArrangement = Arrangement.spacedBy(t.spaceM),
                    ) {
                        Text(selected.titleTr, fontSize = t.headText,
                            color = t.text)
                        Text("Bu sekme F2'de bağlanacak.",
                            color = t.secondary, fontSize = t.bodyText)
                        Text("F1 yalnız ana ekran iskeletini ve bağımsız veri " +
                            "gösterim kurallarını doğrular. A–C sekmeleri eski " +
                            "uygulamada korunmuştur.", color = t.secondary,
                            fontSize = t.bodyText)
                    }
                }
            }
        }

        FinalBottomBar(selected = selected, onSelect = { selected = it })
    }
}

@Composable
private fun DashboardRightPane(state: DashboardUiState) {
    val t = CockpitDesignTokens
    TripMetricsPane(state, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(t.spaceXXS))
    RouteWeatherStrip(state)
    Spacer(Modifier.height(t.spaceXXS))
    ChargePlanCard(state)
}

@Composable
private fun DashboardTopStatus(
    state: DashboardUiState,
    compact: Boolean,
    onDemoChange: (Boolean) -> Unit,
) {
    val t = CockpitDesignTokens
    val time = state.clock.value ?: "—"
    val temperature = state.outdoorTemperatureC.value?.takeIf { it.isFinite() }
        ?.let { DashboardFormat.whole(it) + " °C" } ?: "— °C"
    val battery = DashboardFormat.number(state.socPercent, " %")
    val range = DashboardFormat.number(state.estimatedRangeKm, " km")
    val header = @Composable {
        StatusLabel("Saat", time)
        StatusLabel("Dış sıcaklık", temperature)
        StatusLabel(if (state.socPercent.source == DashboardOrigin.MANUAL)
            "Batarya (manuel)" else if (state.socPercent.source in setOf(
                DashboardOrigin.MODEL, DashboardOrigin.DEMO))
            "Batarya (tahmini)" else "Batarya", battery)
        StatusLabel("Menzil (tahmini)", range)
    }
    Surface(color = t.panel, shape = t.cornerSmall) {
        if (compact) {
            Column(Modifier.fillMaxWidth().padding(t.spaceXS)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    StatusLabel("Saat", time)
                    StatusLabel("Dış sıcaklık", temperature)
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    StatusLabel("Batarya", battery)
                    StatusLabel("Menzil (tahmini)", range)
                }
                DemoModeButton(state.demo, onDemoChange)
            }
        } else {
            Row(
                Modifier.fillMaxWidth().heightIn(min = t.topBarMinHeight)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = t.spaceM),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(t.spaceL),
            ) {
                header()
                Spacer(Modifier.weight(1f))
                DemoModeButton(state.demo, onDemoChange)
            }
        }
    }
}

@Composable
private fun StatusLabel(title: String, value: String) {
    val t = CockpitDesignTokens
    Column(Modifier.padding(t.spaceXXS)) {
        Text(title, color = t.secondary, fontSize = t.labelText, maxLines = 1)
        Text(value, color = t.text, fontSize = t.bodyText,
            fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun DemoModeButton(demo: Boolean, onChange: (Boolean) -> Unit) {
    val t = CockpitDesignTokens
    Surface(
        onClick = { onChange(!demo) },
        color = if (demo) t.accentSoft else t.panelRaised,
        shape = t.cornerSmall,
        modifier = Modifier.heightIn(min = t.target)
            .widthIn(min = t.target),
    ) {
        Box(Modifier.padding(horizontal = t.spaceM),
            contentAlignment = Alignment.Center) {
            Text(if (demo) "Boş veri" else "DEMO aç", color = t.text,
                fontSize = t.bodyText)
        }
    }
}

@Composable
private fun FinalBottomBar(
    selected: FinalDashboardTab,
    onSelect: (FinalDashboardTab) -> Unit,
) {
    val t = CockpitDesignTokens
    Row(
        Modifier.fillMaxWidth().background(t.panel)
            .padding(horizontal = t.spaceXS, vertical = t.spaceXXS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(t.spaceXXS),
    ) {
        FinalDashboardTab.entries.forEach { tab ->
            Surface(
                onClick = { onSelect(tab) },
                modifier = Modifier.weight(1f).heightIn(min = t.tabBarHeight),
                shape = t.cornerSmall,
                color = if (tab == selected) t.accentSoft else t.panel,
                border = if (tab == selected)
                    androidx.compose.foundation.BorderStroke(t.cardOutlineWidth, t.accent)
                else null,
            ) {
                Box(contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(t.spaceXXS)) {
                    Text(tab.titleTr,
                        fontSize = t.tabText,
                        color = if (tab == selected) t.text else t.secondary,
                        fontWeight = if (tab == selected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1)
                }
            }
        }
    }
}
```

---

### 13. `app/src/main/java/app/evcockpit/ui/finalhmi/MapFirstDashboardPreview.kt`

```kotlin
package app.evcockpit.ui.finalhmi

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * Üç farklı dp pencere. Bunlar fiziksel cihaz ölçümü değildir.
 * Preview'de MapLibre native View açılmaz: sahte rota gösterilmez.
 */
@Preview(
    name = "F1 960x540 yatay",
    widthDp = 960, heightDp = 540,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun F1Yatay960() {
    F1DemoPreview()
}

@Preview(
    name = "F1 1280x800 geniş yatay",
    widthDp = 1280, heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun F1Yatay1280() {
    F1DemoPreview()
}

@Preview(
    name = "F1 393x873 POCO F5 dikey düzen",
    widthDp = 393, heightDp = 873,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun F1Dikey393() {
    F1DemoPreview()
}

@Composable
private fun F1DemoPreview() {
    FinalCockpitTheme {
        MapFirstDashboardScreen(
            state = DemoDashboardMapper.map(1_795_460_000_000L),
            onDemoChange = {},
        )
    }
}
```

---

### 14. `app/src/main/java/app/evcockpit/ui/finalhmi/MapFirstDashboardActivity.kt`

```kotlin
package app.evcockpit.ui.finalhmi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue

/**
 * F1 bağımsız ekranı: eski ana Activity ve A–C sekmeleri aynen yerinde.
 * Sadece Ayarlar'daki açık kullanıcı dokunuşuyla açılır.
 * Varsayılan DEMO; "Boş veri" düğmesi gerçek modun veri-yok davranışını gösterir.
 */
class MapFirstDashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: MapFirstDashboardViewModel = viewModel()
            val state by vm.state.collectAsStateWithLifecycle()
            FinalCockpitTheme {
                MapFirstDashboardScreen(
                    state = state,
                    onDemoChange = vm::showDemo,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
                )
            }
        }
    }
}
```

---

### 15. `app/src/test/java/app/evcockpit/ui/finalhmi/DashboardMathTest.kt`

```kotlin
package app.evcockpit.ui.finalhmi

import org.junit.Assert.*
import org.junit.Test

class DashboardMathTest {
    @Test fun socModel52KwhOrnekHesap() {
        // 80% - (10 / 52)*100 = %60,769...
        assertEquals(60.769230769, DashboardMath.estimatedSocPercent(
            80.0, 10.0, 52.0)!!, 0.00001)
    }

    @Test fun socKapasiteProfilParametresindenGelir() {
        val at52 = DashboardMath.estimatedSocPercent(80.0, 10.0, 52.0)!!
        val at45 = DashboardMath.estimatedSocPercent(80.0, 10.0, 45.0)!!
        assertTrue(at45 < at52)
        assertEquals(57.77777777, at45, 0.0001)
    }

    @Test fun socOlcumYoksaRastgeleAzalmaz() {
        assertNull(DashboardMath.estimatedSocPercent(80.0, null, 52.0))
        assertNull(DashboardMath.estimatedSocPercent(null, 4.0, 52.0))
        assertNull(DashboardMath.estimatedSocPercent(80.0, 4.0, null))
    }

    @Test fun socGecersizKapasiteVeEnerjiReddedilir() {
        assertNull(DashboardMath.estimatedSocPercent(80.0, 10.0, 0.0))
        assertNull(DashboardMath.estimatedSocPercent(80.0, 10.0, -52.0))
        assertNull(DashboardMath.estimatedSocPercent(80.0, -2.0, 52.0))
        assertNull(DashboardMath.estimatedSocPercent(101.0, 1.0, 52.0))
        assertNull(DashboardMath.estimatedSocPercent(-1.0, 1.0, 52.0))
        assertNull(DashboardMath.estimatedSocPercent(Double.NaN, 1.0, 52.0))
        assertNull(DashboardMath.estimatedSocPercent(10.0, 20.0, 52.0))
    }

    @Test fun socSinirDegerleriGercekOlcumOlabilir() {
        assertEquals(0.0, DashboardMath.estimatedSocPercent(0.0, 0.0, 52.0)!!, 0.001)
        assertEquals(100.0, DashboardMath.estimatedSocPercent(
            100.0, 0.0, 52.0)!!, 0.001)
        assertEquals(50.0, DashboardMath.estimatedSocPercent(
            75.0, 13.0, 52.0)!!, 0.001)
    }

    @Test fun menzilDogruBirimdeHesaplanir() {
        // 80% x 52 kWh = 41.6 kWh; /16kWh/100km = 260 km.
        assertEquals(260.0, DashboardMath.estimatedRangeKm(
            80.0, 52.0, 16.0)!!, 0.00001)
    }

    @Test fun menzilSifirNegatifNullVeSonsuzTuketimReddedilir() {
        assertNull(DashboardMath.estimatedRangeKm(null, 52.0, 16.0))
        assertNull(DashboardMath.estimatedRangeKm(50.0, 52.0, 0.0))
        assertNull(DashboardMath.estimatedRangeKm(50.0, 52.0, -4.0))
        assertNull(DashboardMath.estimatedRangeKm(50.0, 52.0, Double.NaN))
        assertNull(DashboardMath.estimatedRangeKm(50.0, 52.0, Double.POSITIVE_INFINITY))
        assertNull(DashboardMath.estimatedRangeKm(50.0, null, 16.0))
        assertNull(DashboardMath.estimatedRangeKm(50.0, -52.0, 16.0))
        assertNull(DashboardMath.estimatedRangeKm(200.0, 52.0, 16.0))
    }

    @Test fun menzilSinirDegerleri() {
        assertEquals(0.0, DashboardMath.estimatedRangeKm(
            0.0, 52.0, 16.0)!!, 0.001)
        assertEquals(520.0, DashboardMath.estimatedRangeKm(
            100.0, 52.0, 10.0)!!, 0.001)
    }

    @Test fun gpsHizMpsKmSaDonusumu() {
        assertEquals(90.0, DashboardMath.speedKmhFromGpsMetersPerSecond(25.0)!!, 0.001)
        assertNull(DashboardMath.speedKmhFromGpsMetersPerSecond(null))
        assertNull(DashboardMath.speedKmhFromGpsMetersPerSecond(-1.0))
        assertNull(DashboardMath.speedKmhFromGpsMetersPerSecond(Double.NaN))
    }

    @Test fun kismiFiyatKesinToplamDegildir() {
        assertNull(ChargeCostSummary.label(listOf(PricedStop(null), PricedStop(null))))
        assertNull(ChargeCostSummary.label(emptyList()))
        val partial = ChargeCostSummary.label(
            listOf(PricedStop(120.0), PricedStop(null), PricedStop(55.0)))!!
        assertTrue(partial.startsWith("en az "))
        assertTrue(partial.contains("1 durakta fiyat yok"))
        assertTrue(partial.contains("175"))
        val full = ChargeCostSummary.label(listOf(PricedStop(120.0), PricedStop(55.0)))!!
        assertFalse(full.startsWith("en az "))
        assertTrue(full.contains("tahmini"))
    }

    @Test fun kaynagiVeZamaniOlmayanDegerSifirOlmaz() {
        val raw = StampedValue(82.0, DashboardOrigin.GPS, timestampEpochMs = null)
        assertNull(raw.freshValue(1000L, 100L))
        val old = StampedValue(82.0, DashboardOrigin.GPS, timestampEpochMs = 1000L)
        assertNull(old.freshValue(3000L, 100L))
        assertEquals(82.0, old.freshValue(1050L, 100L)!!, 0.001)
    }
}
```

---

### 16. `app/src/test/java/app/evcockpit/ui/finalhmi/DashboardMapperAndLayoutTest.kt`

```kotlin
package app.evcockpit.ui.finalhmi

import org.junit.Assert.*
import org.junit.Test

class DashboardMapperAndLayoutTest {
    private val now = 1_795_460_000_000L

    @Test fun gercekModBosKalirDemoGorunmez() {
        val empty = RealDashboardMapper.map(RealDashboardInput(), now)
        assertFalse(empty.demo)
        assertNull(empty.speedKmh.value)
        assertNull(empty.socPercent.value)
        assertNull(empty.estimatedRangeKm.value)
        assertNull(empty.chargeTotalLabel.value)
        assertTrue(empty.weatherSamples.isEmpty())
    }

    @Test fun demoModeliYalnizAyrikDemoMapperUretir() {
        val demo = DemoDashboardMapper.map(now)
        assertTrue(demo.demo)
        assertEquals(DashboardOrigin.DEMO, demo.socPercent.source)
        assertEquals(DashboardOrigin.DEMO, demo.speedKmh.source)
        assertTrue(demo.weatherSamples.isNotEmpty())
        val real = RealDashboardMapper.map(
            RealDashboardInput(
                gpsSpeedMetersPerSecond = StampedValue(
                    25.0, DashboardOrigin.DEMO, now),
                obdVerifiedSocPercent = StampedValue(
                    75.0, DashboardOrigin.DEMO, now),
                modelConsumptionKwhPer100Km = StampedValue(
                    17.0, DashboardOrigin.DEMO, now),
            ), now)
        assertFalse(real.demo)
        assertNull(real.speedKmh.value)
        assertNull(real.socPercent.value)
        assertNull(real.consumptionKwhPer100Km.value)
    }

    @Test fun hizYalnizTazeGpsKaynaklidir() {
        val valid = RealDashboardMapper.map(RealDashboardInput(
            gpsSpeedMetersPerSecond = StampedValue(
                25.0, DashboardOrigin.GPS, now - 2_000L),
        ), now)
        assertEquals(90.0, valid.speedKmh.value!!, 0.001)
        assertEquals(DashboardOrigin.GPS, valid.speedKmh.source)
        val stale = RealDashboardMapper.map(RealDashboardInput(
            gpsSpeedMetersPerSecond = StampedValue(
                25.0, DashboardOrigin.GPS, now - 20_000L),
        ), now)
        assertNull(stale.speedKmh.value)
        val wrong = RealDashboardMapper.map(RealDashboardInput(
            gpsSpeedMetersPerSecond = StampedValue(
                25.0, DashboardOrigin.OBD_VERIFIED, now),
        ), now)
        assertNull(wrong.speedKmh.value)
    }

    @Test fun manuelSocGuzergahIlerlemesiYokkenAyniKalir() {
        val data = RealDashboardInput(
            manualStartSocPercent = StampedValue(80.0, DashboardOrigin.MANUAL, now),
            modelEnergySpentKwh = StampedValue(10.0, DashboardOrigin.MODEL, now),
            vehicleUsableCapacityKwh = StampedValue(52.0, DashboardOrigin.MODEL, now),
            modelConsumptionKwhPer100Km = StampedValue(16.0, DashboardOrigin.MODEL, now),
            routeProgressReliable = false,
        )
        val manual = RealDashboardMapper.map(data, now)
        assertEquals(80.0, manual.socPercent.value!!, 0.001)
        assertEquals(DashboardOrigin.MANUAL, manual.socPercent.source)
        val progressed = RealDashboardMapper.map(
            data.copy(routeProgressReliable = true), now)
        assertEquals(60.769230769, progressed.socPercent.value!!, 0.00001)
        assertEquals(DashboardOrigin.MODEL, progressed.socPercent.source)
        assertEquals(197.5, progressed.estimatedRangeKm.value!!, 0.01)
    }

    @Test fun veriEskidiyseUzerineTahminYapilmaz() {
        val data = RealDashboardInput(
            manualStartSocPercent = StampedValue(78.0, DashboardOrigin.MANUAL, now),
            modelEnergySpentKwh = StampedValue(6.0, DashboardOrigin.MODEL, now - 300_000L),
            modelConsumptionKwhPer100Km = StampedValue(16.0, DashboardOrigin.MODEL, now - 300_000L),
            vehicleUsableCapacityKwh = StampedValue(52.0, DashboardOrigin.MODEL, now),
            routeProgressReliable = true,
        )
        val actual = RealDashboardMapper.map(data, now)
        assertEquals(78.0, actual.socPercent.value!!, 0.001)
        assertEquals(DashboardOrigin.MANUAL, actual.socPercent.source)
        assertNull(actual.estimatedRangeKm.value)
    }

    @Test fun pencereSiniflariVeAltBesSekme() {
        assertFalse(F1DashboardLayoutPolicy.useWideMapFirst(393, 873))
        assertFalse(F1DashboardLayoutPolicy.useWideMapFirst(700, 400))
        assertTrue(F1DashboardLayoutPolicy.useWideMapFirst(960, 540))
        assertTrue(F1DashboardLayoutPolicy.useWideMapFirst(1280, 800))
        assertEquals(listOf("Harita", "Rota", "Şarj", "Enerji", "Ayarlar"),
            F1DashboardLayoutPolicy.tabs)
        assertEquals(5, FinalDashboardTab.entries.size)
    }

    @Test fun tiklamaHedefiEnAz56Dp() {
        assertTrue(CockpitDesignTokens.target.value >= 56f)
        assertTrue(CockpitDesignTokens.tabBarHeight.value >= 56f)
    }
}
```
