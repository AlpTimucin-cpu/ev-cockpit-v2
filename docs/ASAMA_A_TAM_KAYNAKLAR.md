# Aşama A — değişen dosyaların TAM içeriği

Bu belge `feature/stage-a-hilt-map-20261123` dalında Aşama A sırasında değiştirilen **bütün 13 kaynak/yapılandırma/test dosyasının tam sürümüdür**. Parça veya “geri kalanı aynı” ifadesi kullanılmamıştır. Dosyalar `main` dalına birleştirilmemiştir.

Bu dosya bir proje kodu yerine geçmez; depodaki konumlarına göre doğrudan kopyalanabilir. Telefon ile geliştirme için GitHub PR sayfasından ilgili dosyaları açmak yeterlidir. Aşama A için aşağıdaki dosyaları mevcut klasör yollarında oluşturun/değiştirin. GitHub üzerindeki dalda zaten yüklenmiştir; tekrar yüklemeniz gerekmez.


---

## 1. `.github/workflows/android.yml`

```yaml
name: Android CI

on:
  push:
    # Her özellik dalında da APK artifaktı üret; Nightly yalnız main'de kalır.
    branches: ['**']
  pull_request:
  workflow_dispatch:

permissions:
  contents: read

jobs:
  build:
    name: Unit tests and debug APK
    runs-on: ubuntu-latest
    timeout-minutes: 30

    steps:
      - name: Checkout source
        uses: actions/checkout@v4

      - name: Set up Java 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - name: Set up Gradle without wrapper
        uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: '8.11.1'

      - name: Load public test debug signing certificate
        run: |
          set -euo pipefail
          base64 --decode ci/public-debug-ec.p12.b64 > "$RUNNER_TEMP/ev-cockpit-public-debug.p12"
          echo "EV_COCKPIT_PUBLIC_DEBUG_P12=$RUNNER_TEMP/ev-cockpit-public-debug.p12" >> "$GITHUB_ENV"

      - name: Test and build debug APK
        run: gradle :app:testDebugUnitTest :app:assembleDebug --stacktrace

      - name: JUnit test count (XML evidence)
        run: |
          python3 - <<'PY'
          import glob, xml.etree.ElementTree as ET
          paths=glob.glob("app/build/test-results/testDebugUnitTest/TEST-*.xml")
          assert paths, "JUnit results missing"
          suites=[ET.parse(p).getroot() for p in paths]
          total=sum(int(x.get("tests",0)) for x in suites)
          bad=sum(int(x.get("failures",0))+int(x.get("errors",0)) for x in suites)
          skipped=sum(int(x.get("skipped",0)) for x in suites)
          print(f"M8_JUNIT_TOTAL={total} PASSED={total-bad-skipped} FAILED={bad} SKIPPED={skipped}")
          assert total>0 and bad==0
          for suite in suites:
            for output in suite.findall(".//system-out"):
              if output.text:
                for line in output.text.splitlines():
                  if line.startswith(("M8_PERF_", "M8_SAFETY_", "M8_SCENARIO ")):
                    print(line)
          PY

      - name: Audit 16 KB native ELF and ZIP alignment
        run: |
          set -euo pipefail
          python3 ci/native_page_size_audit_test.py
          python3 ci/native_page_size_audit.py app/build/outputs/apk/debug/app-debug.apk | tee native-page-audit.txt
          ZIPALIGN="$(find "$ANDROID_HOME/build-tools" -maxdepth 2 -name zipalign -type f | sort -V | tail -1)"
          test -n "$ZIPALIGN"
          "$ZIPALIGN" -c -P 16 4 app/build/outputs/apk/debug/app-debug.apk

      - name: Upload native alignment evidence
        uses: actions/upload-artifact@v4
        with:
          name: ev-cockpit-v2-native-16k-audit
          path: native-page-audit.txt
          if-no-files-found: error

      - name: Verify stable APK certificate with apksigner
        run: |
          set -euo pipefail
          APK_SIGNER="$(find "$ANDROID_HOME/build-tools" -maxdepth 2 -name apksigner -type f | sort -V | tail -1)"
          test -n "$APK_SIGNER"
          "$APK_SIGNER" verify --print-certs app/build/outputs/apk/debug/app-debug.apk | tee apksigner-certs.txt
          grep -qiF '829f81de4a87111e0b7b5052b87de77b033859fbd03d45cb38433cf57c7b89fb' apksigner-certs.txt
          echo 'Public test debug certificate matched expected fingerprint.'

      - name: Upload signing evidence
        uses: actions/upload-artifact@v4
        with:
          name: ev-cockpit-v2-public-debug-signing-evidence
          path: apksigner-certs.txt
          if-no-files-found: error

      - name: Upload debug APK artifact
        uses: actions/upload-artifact@v4
        with:
          name: ev-cockpit-v2-debug
          path: app/build/outputs/apk/debug/app-debug.apk
          if-no-files-found: error
          retention-days: 14

  nightly:
    name: Publish nightly pre-release
    needs: build
    if: github.ref == 'refs/heads/main' && (github.event_name == 'push' || github.event_name == 'workflow_dispatch')
    runs-on: ubuntu-latest
    timeout-minutes: 10
    concurrency:
      group: ev-cockpit-v2-nightly
      cancel-in-progress: false
    permissions:
      contents: write
    steps:
      - name: Download verified CI artifact
        uses: actions/download-artifact@v4
        with:
          name: ev-cockpit-v2-debug
          path: nightly

      - name: Update single nightly pre-release
        env:
          GH_TOKEN: ${{ github.token }}
        run: |
          set -euo pipefail
          APK='nightly/app-debug.apk'
          test -s "$APK"
          cat > nightly-notes.md <<EOF
          Debug test APK; not a stable or vehicle-validated release.
          Commit: $GITHUB_SHA
          Successful CI run: https://github.com/$GITHUB_REPOSITORY/actions/runs/$GITHUB_RUN_ID
          MEB PID scales and ELM327 compatibility remain unverified on vehicle.
          EOF
          if gh release view nightly --repo "$GITHUB_REPOSITORY" >/dev/null 2>&1; then
            gh release upload nightly "$APK" --repo "$GITHUB_REPOSITORY" --clobber
            gh release edit nightly --repo "$GITHUB_REPOSITORY" --title 'Nightly - ID.4 Yol Arkadaşı' --prerelease --latest=false --notes-file nightly-notes.md
          else
            gh release create nightly "$APK" --repo "$GITHUB_REPOSITORY" --target "$GITHUB_SHA" --title 'Nightly - ID.4 Yol Arkadaşı' --prerelease --latest=false --notes-file nightly-notes.md
          fi
          gh release view nightly --repo "$GITHUB_REPOSITORY" --json assets --jq '.assets[] | select(.name == "app-debug.apk") | .url' | grep -q .
```

---

## 2. `build.gradle.kts`

```kotlin
plugins {
    id("com.android.application") version "8.10.1" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("org.jetbrains.kotlin.kapt") version "2.0.21" apply false
    id("com.google.dagger.hilt.android") version "2.57.1" apply false
}
```

---

## 3. `app/build.gradle.kts`

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
        versionCode = 44
        versionName = "0.3.21-stability"
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

## 4. `app/src/main/java/app/evcockpit/CockpitApplication.kt`

```kotlin
package app.evcockpit

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import app.evcockpit.obd.ObdRepository
import app.evcockpit.diagnostics.CrashLog
import app.evcockpit.location.LocationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Process-scoped state holder so Activity recreation does not cancel the OBD session. */
@HiltAndroidApp
class CockpitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
    }

    val locationRepository: LocationRepository by lazy { LocationRepository() }
    val obdRepository: ObdRepository by lazy {
        ObdRepository(this, CoroutineScope(SupervisorJob() + Dispatchers.IO))
    }
}
```

---

## 5. `app/src/main/java/app/evcockpit/MainActivity.kt`

```kotlin
package app.evcockpit

import dagger.hilt.android.AndroidEntryPoint
import android.Manifest
import android.content.Intent
import android.provider.Settings
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.evcockpit.obd.ObdLogShare
import app.evcockpit.obd.SafeTestReportExporter
import app.evcockpit.obd.ObdViewModel
import app.evcockpit.obd.ManualSocStore
import app.evcockpit.navigation.NavigationViewModel
import app.evcockpit.navigation.PlaceAutocompleteViewModel
import app.evcockpit.weather.RouteHourlyWeatherViewModel
import app.evcockpit.passenger.PassengerPlayerModel
import app.evcockpit.location.SpeedSelector
import app.evcockpit.integration.MolaOpenContract
import kotlinx.coroutines.flow.MutableStateFlow
import app.evcockpit.ui.PassengerVideoScreen
import app.evcockpit.ui.RouteScreen
import app.evcockpit.ui.PlannerScreen
import app.evcockpit.planui.PlannerViewModel
import app.evcockpit.ui.tesla.TeslaDashboardScreen
import app.evcockpit.ui.tesla.TeslaAction
import app.evcockpit.ui.tesla.DriveMode
import app.evcockpit.ui.tesla.DriveModeViewModel
import app.evcockpit.ui.tesla.DriveModePolicy
import app.evcockpit.integration.MolaLinkViewModel
import app.evcockpit.integration.MolaStatus
import app.evcockpit.ui.tesla.MolaLaunchCard
import app.evcockpit.ui.tesla.DashboardGuidancePolicy
import app.evcockpit.ui.tesla.GuidanceAction
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.evcockpit.ui.tesla.SafeSocSelection
import app.evcockpit.storage.VehicleProfileStore
import app.evcockpit.planner.model.VehicleProfile
import app.evcockpit.planner.model.PlanResult
import app.evcockpit.ui.ObdTestScreen
import app.evcockpit.ui.RouteSettingsScreen
import app.evcockpit.ui.CockpitTheme
import app.evcockpit.ui.CockpitTabs
import app.evcockpit.ui.DisplayPreferences
import kotlinx.coroutines.launch

private data class ExternalOpenRequest(val generation: Int = 0, val tab: Int? = null)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val externalOpenRequest = MutableStateFlow(ExternalOpenRequest())

    private fun acceptOpenIntent(value: Intent?) {
        if (value?.action != Intent.ACTION_VIEW) return
        val target = MolaOpenContract.tabFor(value.dataString) ?: return
        val previous = externalOpenRequest.value
        externalOpenRequest.value = ExternalOpenRequest(previous.generation + 1, target)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        acceptOpenIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        acceptOpenIntent(intent)
        setContent {
            val openRequest by externalOpenRequest.collectAsState()
            val displayPreferences = remember { DisplayPreferences(applicationContext) }
            val darkTheme by displayPreferences.darkTheme.collectAsState(
                initial = true,
            )
            val keepScreenOn by displayPreferences.keepScreenOn.collectAsState(
                initial = false,
            )
            DisposableEffect(keepScreenOn) {
                if (keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                onDispose { /* next effect handles updates; Activity teardown clears the flag */ }
            }
            CockpitTheme(dark = darkTheme) {
                val obdViewModel: ObdViewModel = viewModel()
                val navViewModel: NavigationViewModel = viewModel()
                val placeSearchModel: PlaceAutocompleteViewModel = viewModel()
                val routeWeatherModel: RouteHourlyWeatherViewModel = viewModel()
                val passengerModel: PassengerPlayerModel = viewModel()
                val plannerModel: PlannerViewModel = viewModel()
                val driveModel: DriveModeViewModel = viewModel()
                val molaModel: MolaLinkViewModel = viewModel()
                App(obdViewModel, navViewModel, placeSearchModel, routeWeatherModel, passengerModel, plannerModel,
                    driveModel, molaModel, displayPreferences, darkTheme, keepScreenOn, openRequest)
            }
        }
    }
}

@Composable
private fun App(vm: ObdViewModel, navVm: NavigationViewModel,
    placeSearchModel: PlaceAutocompleteViewModel,
    routeWeatherModel: RouteHourlyWeatherViewModel,
    passengerModel: PassengerPlayerModel, plannerModel: PlannerViewModel,
    driveModel: DriveModeViewModel,
    molaModel: MolaLinkViewModel,
    displayPreferences: DisplayPreferences,
    darkTheme: Boolean, keepScreenOn: Boolean,
    openRequest: ExternalOpenRequest) {
    val context = LocalContext.current
    val molaState by molaModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, molaModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) molaModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
            molaModel.refresh()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val vehicleProfileStore = remember(context) { VehicleProfileStore(context.applicationContext) }
    val vehicleProfile by vehicleProfileStore.profile.collectAsStateWithLifecycle(
        initialValue = VehicleProfile.defaults())
    val planUi by plannerModel.state.collectAsStateWithLifecycle()
    val manualStore = remember(context) { ManualSocStore(context.applicationContext) }
    val manualSoc by manualStore.socPct.collectAsStateWithLifecycle(initialValue = null)
    val saveScope = rememberCoroutineScope()
    val state by vm.state.collectAsStateWithLifecycle()
    val drive by driveModel.state.collectAsStateWithLifecycle()
    val socReading = SafeSocSelection.select(state, manualSoc, drive)
    val logs by vm.log.collectAsStateWithLifecycle()
    val logPath by vm.logFilePath.collectAsStateWithLifecycle()
    val diagnosisPath by vm.diagnosisFilePath.collectAsStateWithLifecycle()
    val diagnosisSummary by vm.diagnosisSummary.collectAsStateWithLifecycle()
    val devices by vm.devices.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val gps by vm.gps.collectAsStateWithLifecycle()
    val gpsFresh = drive.gpsFresh
    val navState by navVm.state.collectAsStateWithLifecycle()
    val weatherSummary by routeWeatherModel.summary.collectAsStateWithLifecycle()
    LaunchedEffect(navState.route) {
        // Only ViewModel/repository makes the network call, not this Composable.
        routeWeatherModel.refresh(navState.route)
    }
    val voiceState by navVm.voice.collectAsStateWithLifecycle()
    var shareError by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableIntStateOf(0) }
    var showMolaCard by remember { mutableStateOf(false) }
    LaunchedEffect(drive.mode) {
        if (drive.mode != DriveMode.PARKED) showMolaCard = false
    }
    LaunchedEffect(openRequest.generation) {
        openRequest.tab?.let { requested ->
            if (DriveModePolicy.allowedTab(requested, drive.mode)) tab = requested
        }
    }
    LaunchedEffect(drive.mode, tab) {
        val mustClose = (drive.mode == DriveMode.DRIVING && tab in setOf(3,4,5)) ||
            (drive.mode != DriveMode.PARKED && tab == 4)
        if (mustClose) {
            passengerModel.pause()
            tab = 0
        }
    }

    val permissions = remember {
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { vm.refreshBonded() }
    LaunchedEffect(vm) { launcher.launch(permissions) }

    val gpsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        if (results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) vm.startGps() else vm.denyGps()
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        .windowInsetsPadding(WindowInsets.safeDrawing)) {
        if (tab != 0) CockpitTabs(selected = tab, onSelect = { selected ->
            if (DriveModePolicy.allowedTab(selected, drive.mode)) tab = selected
        })
        when (tab) {
            0 -> TeslaDashboardScreen(
                s = state,
                soc = socReading,
                usableKwhAtSoh = vehicleProfile.capacityAtSohKwh,
                gps = gps,
                gpsFresh = gpsFresh,
                navigation = navState,
                plan = (planUi.plan as? PlanResult.Success).takeIf {
                    planUi.route == navState.route
                },
                driveMode = drive,
                weatherSummary = weatherSummary,
                onSelectDestination = navVm::selectDestination,
                molaInstalled = molaState.status == MolaStatus.READY ||
                    molaState.status == MolaStatus.CANNOT_LAUNCH,
                guidance = DashboardGuidancePolicy.evaluate(
                    state, socReading, gps, gpsFresh, navState,
                    planUi.plan.takeIf { planUi.route == navState.route }),
                onGuidanceAction = { action ->
                    if (drive.mode != DriveMode.DRIVING) when (action) {
                        GuidanceAction.NONE -> Unit
                        GuidanceAction.SETTINGS -> tab = 5
                        GuidanceAction.LOCATION_PERMISSION -> gpsPermissionLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION))
                        GuidanceAction.OBD_TEST -> tab = 1
                        GuidanceAction.ROUTE -> tab = 2
                        GuidanceAction.PLAN -> tab = 3
                    }
                },
                onAction = { action ->
                    when (action) {
                        TeslaAction.ROTA -> tab = 2
                        TeslaAction.PLAN -> tab = 3
                        TeslaAction.MEDYA, TeslaAction.VIDEO -> tab = 4
                        TeslaAction.MOLA -> {
                            if (drive.mode == DriveMode.PARKED) {
                                molaModel.refresh()
                                showMolaCard = true
                            }
                        }
                        TeslaAction.AYARLAR -> tab = 5
                    }
                },
            )
            1 -> ObdTestScreen(
                state = state,
                log = logs,
                logFilePath = logPath,
                diagnosisFilePath = diagnosisPath,
                diagnosisSummary = diagnosisSummary,
                devices = devices,
                message = shareError ?: message,
                onRefresh = vm::refreshBonded,
                onConnect = vm::connect,
                onDiagnose = vm::diagnose,
                onExportTestReport = { carScannerNote ->
                    shareError = null
                    val lastLogs = logs.takeLast(200)
                    val lastSummary = diagnosisSummary
                    saveScope.launch {
                        try {
                            val path = withContext(Dispatchers.IO) {
                                SafeTestReportExporter.create(context, lastSummary, lastLogs, carScannerNote)
                            }
                            ObdLogShare.launch(context, path)
                        } catch (error: Exception) {
                            shareError = "Test raporu dışa aktarılamadı: " +
                                error.javaClass.simpleName
                        }
                    }
                },
                onShareDiagnosis = {
                    shareError = null
                    try {
                        if (!ObdLogShare.launch(context, diagnosisPath)) {
                            shareError = "Tanı logu bulunamadı."
                        }
                    } catch (error: Exception) {
                        shareError = "Tanı logu paylaşılamadı: " +
                            error.javaClass.simpleName
                    }
                },
                onDemo = vm::startDemo,
                onStop = vm::stop,
                onShareLog = {
                    shareError = null
                    try {
                        if (!ObdLogShare.launch(context, logPath)) {
                            shareError = "Paylaşılacak kayıt henüz oluşturulmadı."
                        }
                    } catch (error: Exception) {
                        shareError = "Kayıt paylaşılamadı: ${error.message ?: "Bilinmeyen hata"}"
                    }
                },
                onOpenBluetoothSettings = {
                    shareError = null
                    try {
                        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                    } catch (error: Exception) {
                        shareError = "Bluetooth ayarları açılamadı: ${error.message ?: "Bilinmeyen hata"}"
                    }
                },
            )
            2 -> RouteScreen(
                state = navState,
                gps = gps,
                socPct = socReading.pct,
                socSource = socReading.source,
                usableKwh = vehicleProfile.capacityAtSohKwh,
                voice = voiceState,
                onToggleVoice = navVm::toggleVoice,
                onSpeakNextManeuver = navVm::testNextManeuver,
                onCalculate = navVm::calculate,
                onSelectDestination = navVm::selectDestination,
                onSelectNamedDestination = navVm::selectNamedDestination,
                onSelectOrigin = navVm::selectOrigin,
                onUseGpsOrigin = navVm::useGpsOrigin,
                onClearDestinationSelection = navVm::clearDestinationSelection,
                onSelectAlternative = navVm::selectAlternative,
                searchVm = placeSearchModel,
                routeWeather = weatherSummary,
                onClear = navVm::clear,
            )
            3 -> PlannerScreen(
                vm = plannerModel, navigation = navState.route, soc = socReading,
                onOpenRoute = { tab = 2 }, onOpenSettings = { tab = 5 },
                onNavigate = { stops, destination ->
                    navVm.navigateVia(stops,destination)
                    tab = 2
                },
                gps = gps,
                onSelectDestination = navVm::selectDestination,
            )
            4 -> PassengerVideoScreen(
                vm = passengerModel,
                gps = gps,
                route = navState.route,
                destination = navState.destination,
                speedKmh = SpeedSelector.select(
                    obdConnected = state.connected && drive.obdSpeedFresh, demo = state.demo,
                    obdKmh = state.speedKmh, gpsKmh = gps.speedKmh,
                    gpsFresh = gpsFresh).kmh,
                onSelectDestination = navVm::selectDestination,
            )
            else -> RouteSettingsScreen(
                gps = gps,
                darkTheme = darkTheme,
                keepScreenOn = keepScreenOn,
                onDarkTheme = { value -> saveScope.launch { displayPreferences.setDarkTheme(value) } },
                onKeepScreenOn = { value -> saveScope.launch { displayPreferences.setKeepScreenOn(value) } },
                manualSocPct = manualSoc,
                onSaveManualSoc = { value -> saveScope.launch { manualStore.save(value) } },
                onClearManualSoc = { saveScope.launch { manualStore.clear() } },
                onStartGps = {
                    gpsPermissionLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    )
                },
                onStopGps = vm::stopGps,
            )
        }
    }
    if (showMolaCard && drive.mode == DriveMode.PARKED) {
        MolaLaunchCard(
            state = molaState,
            onOpen = molaModel::openMola,
            onInstallGuide = molaModel::openInstallGuide,
            onAppSettings = molaModel::openSettings,
            onRefresh = molaModel::refresh,
            onDismiss = { showMolaCard = false },
        )
    }

}
```

---

## 6. `app/src/main/java/app/evcockpit/di/LocationModule.kt`

```kotlin
package app.evcockpit.di

import android.content.Context
import app.evcockpit.CockpitApplication
import app.evcockpit.location.LocationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Konum servisi ile mevcut ekranların tek bir GPS akışını paylaşmasını sağlar.
 * Önceden oluşturulan uygulama deposu korunur; ayrı StateFlow üretilmez.
 */
@Module
@InstallIn(SingletonComponent::class)
object LocationModule {
    @Provides
    @Singleton
    fun provideLocationRepository(@ApplicationContext context: Context): LocationRepository =
        (context.applicationContext as CockpitApplication).locationRepository
}
```

---

## 7. `app/src/main/java/app/evcockpit/location/LocationRepository.kt`

```kotlin
package app.evcockpit.location

import android.location.Location
import android.os.SystemClock
import app.evcockpit.navigation.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow

data class GpsSnapshot(
    val point: GeoPoint? = null,
    val speedKmh: Double? = null,
    val accuracyMeters: Float? = null,
    val provider: String? = null,
    val receivedElapsedRealtimeMs: Long = 0L,
    val active: Boolean = false,
    val status: String = "Konum takibi başlatılmadı",
    // GNSS hareket doğrultusu: cihazın pusula açısı değildir.
    val courseDegrees: Double? = null,
) {
    fun isFresh(nowElapsedRealtimeMs: Long): Boolean =
        point != null && receivedElapsedRealtimeMs > 0L &&
            nowElapsedRealtimeMs - receivedElapsedRealtimeMs in 0L..GpsFixAgePolicy.MAX_AGE_MS
}

class LocationRepository {
    val state = MutableStateFlow(GpsSnapshot())

    fun starting() {
        state.value = GpsSnapshot(active = true, status = "GPS konumu bekleniyor")
    }

    fun failure(text: String) {
        state.value = GpsSnapshot(status = text)
    }

    fun stop() {
        state.value = GpsSnapshot(status = "Konum takibi durduruldu")
    }

    fun update(location: Location) {
        val now = SystemClock.elapsedRealtime()
        // A delayed/cached LocationManager callback must never be stamped "fresh".
        val fixTime = GpsFixAgePolicy.acceptedTimestampMs(
            now, location.elapsedRealtimeNanos,
        ) ?: return
        val previous = state.value
        // Prefer fresh GNSS data over coarse network fixes.
        if (location.provider != android.location.LocationManager.GPS_PROVIDER &&
            previous.provider == android.location.LocationManager.GPS_PROVIDER &&
            previous.isFresh(now)
        ) return

        val point = runCatching { GeoPoint(location.latitude, location.longitude) }.getOrNull()
            ?: return
        val accuracy = if (location.hasAccuracy()) location.accuracy else null
        val speed = if (location.hasSpeed() && location.speed.isFinite() &&
            location.speed in 0f..83.333f && (accuracy == null || accuracy <= 100f)
        ) location.speed.toDouble() * 3.6 else null
        state.value = GpsSnapshot(
            point = point,
            speedKmh = speed,
            accuracyMeters = accuracy,
            provider = location.provider,
            receivedElapsedRealtimeMs = fixTime,
            active = true,
            courseDegrees = if (location.hasBearing() &&
                location.bearing.isFinite() && location.bearing in 0f..360f &&
                (accuracy == null || accuracy <= 100f)
            ) location.bearing.toDouble() else null,
            status = if (location.provider == android.location.LocationManager.GPS_PROVIDER) {
                "GPS sinyali alındı (doğruluk: ${accuracy?.toInt() ?: "?"} m)"
            } else "Yaklaşık ağ konumu; kesin GPS değil",
        )
    }
}
```

---

## 8. `app/src/main/java/app/evcockpit/location/LocationTrackingService.kt`

```kotlin
package app.evcockpit.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import app.evcockpit.CockpitApplication
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import app.evcockpit.MainActivity

/**
 * User-started location foreground service. No background-start workaround,
 * no fake GPS position and no ACCESS_BACKGROUND_LOCATION request.
 */
@AndroidEntryPoint
class LocationTrackingService : Service() {
    @Inject lateinit var repo: LocationRepository
    private var manager: LocationManager? = null
    private var listener: LocationListener? = null

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_START) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        try {
            val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
            val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
            if (!fine && !coarse) throw SecurityException("Konum izni eksik")
            ensureForeground()
            val locationManager = getSystemService(LocationManager::class.java)
                ?: throw IllegalStateException("Konum servisi bulunamadı")
            manager = locationManager
            repo.starting()
            val update = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    repo.update(location)
                }
            }
            listener = update
            var requested = false
            if (fine && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 1_000L, 0f, update, Looper.getMainLooper(),
                )
                requested = true
            }
            if (coarse && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 5_000L, 10f, update, Looper.getMainLooper(),
                )
                requested = true
            }
            if (!requested) throw IllegalStateException("Konum sağlayıcıları kapalı")
        } catch (error: Exception) {
            val message = when (error) {
                is SecurityException -> "Konum izni gerekli; telefon ayarlarını kontrol et"
                else -> "Konum servisi kullanılamıyor: ${error.message ?: "Bilinmeyen hata"}"
            }
            stopSelf(startId)
            // onDestroy clears the fix; publish actionable status again afterwards.
            repo.failure(message)
            return START_NOT_STICKY
        }
        return START_NOT_STICKY
    }

    private fun ensureForeground() {
        val channel = "nav_location"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(channel, "Navigasyon konumu", NotificationManager.IMPORTANCE_LOW),
            )
        }
        val intent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification: Notification = NotificationCompat.Builder(this, channel)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("ID.4 Yol Arkadaşı — konum")
            .setContentText("Konum alınıyor; gerçek rota henüz doğrulanmadı")
            .setOngoing(true)
            .setContentIntent(intent)
            .build()
        startForeground(1104, notification)
    }

    override fun onDestroy() {
        val update = listener
        if (update != null) runCatching { manager?.removeUpdates(update) }
        manager = null
        listener = null
        if (repo.state.value.active) repo.stop()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "app.evcockpit.location.START"
    }
}
```

---

## 9. `app/src/main/java/app/evcockpit/ui/MapLayerPreferences.kt`

```kotlin
package app.evcockpit.ui

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.evMapLayerPrefs by preferencesDataStore("ev_map_layer_v1")

/**
 * Only whitelisted, documented MapLibre/OpenFreeMap styles.
 * Styles use OpenStreetMap data, NOT satellite or a live-traffic feed.
 */
enum class MapLayer(val id: String, val titleTr: String, val styleUrl: String) {
    DARK("dark", "Koyu", "https://tiles.openfreemap.org/styles/dark"),
    LIBERTY("liberty", "Yol / sokak", "https://tiles.openfreemap.org/styles/liberty"),
    BRIGHT("bright", "Açık", "https://tiles.openfreemap.org/styles/bright"),
    POSITRON("positron", "Sade", "https://tiles.openfreemap.org/styles/positron"),
    SATELLITE("satellite", "Uydu (MapTiler)", ""),
    TOPOGRAPHIC("topographic", "Arazi / topografya (MapTiler)", "");

    companion object {
        fun fromId(raw: String?): MapLayer =
            entries.firstOrNull { it.id == raw } ?: DARK
    }
}

class MapLayerPreferences(context: Context) {
    private val prefs = context.applicationContext.evMapLayerPrefs
    private val key = stringPreferencesKey("layer")
    val selected = prefs.data.map { MapLayer.fromId(it[key]) }
    suspend fun choose(layer: MapLayer) {
        prefs.edit { it[key] = layer.id }
    }
}

/**
 * Özel harita sağlayıcısının anahtarı olmadan uydu/topografya URL'si oluşturulmaz.
 * URL uygulama loguna yazılmaz. Harita karo/stil istekleri sağlayıcıya gider.
 */
object MapStyleResolver {
    fun url(layer: MapLayer, mapTilerKey: String?): String? = when (layer) {
        MapLayer.SATELLITE, MapLayer.TOPOGRAPHIC -> {
            val key = mapTilerKey?.trim()
            if (key.isNullOrEmpty() || !key.matches(Regex("[a-zA-Z0-9_-]{6,256}")))
                null
            else {
                val styleId = if (layer == MapLayer.SATELLITE) "satellite" else "topo"
                "https://api.maptiler.com/maps/$styleId/style.json?key=$key"
            }
        }
        else -> layer.styleUrl
    }
}
```

---

## 10. `app/src/main/java/app/evcockpit/ui/MapTilerKeyStore.kt`

```kotlin
package app.evcockpit.ui

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.evcockpit.navigation.RouteKeyCipher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

private val Context.mapTilerCredentials by preferencesDataStore(name = "maptiler_credentials_v1")

/**
 * MapTiler anahtarı APK içine gömülmez; Android Keystore AES-GCM ile şifrelenir.
 * Anahtar yalnızca MapTiler'a yapılacak harita stili/karosu isteğinde kullanılır.
 */
class MapTilerKeyStore(context: Context) {
    private val app = context.applicationContext
    private val field = stringPreferencesKey("ciphertext_v1")

    val key: Flow<String?> = app.mapTilerCredentials.data.map { values ->
        values[field]?.let { encrypted ->
            runCatching { RouteKeyCipher.decrypt(encrypted) }.getOrNull()
        }
    }.flowOn(Dispatchers.IO)

    suspend fun save(rawKey: String) {
        val trimmed = rawKey.trim()
        require(trimmed.matches(Regex("[a-zA-Z0-9_-]{6,256}"))) {
            "MapTiler anahtarının biçimi geçersiz."
        }
        val encrypted = RouteKeyCipher.encrypt(trimmed)
        app.mapTilerCredentials.edit { it[field] = encrypted }
    }

    suspend fun clear() {
        app.mapTilerCredentials.edit { it.remove(field) }
    }
}
```

---

## 11. `app/src/main/java/app/evcockpit/ui/OnlineMap.kt`

```kotlin
package app.evcockpit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.awaitCancellation
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import kotlinx.coroutines.launch
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.Polyline
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.annotations.IconFactory
import app.evcockpit.planner.charging.ChargerCandidate
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import kotlin.math.abs
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import app.evcockpit.navigation.GeoPoint
import app.evcockpit.navigation.NavigationRoute
import app.evcockpit.location.GpsSnapshot
import app.evcockpit.navigation.RouteProgress
import app.evcockpit.ui.tesla.RoutePolylineProgress

/**
 * MapLibre Native + OpenFreeMap public online tiles. No offline tile download.
 * Initial camera shows Türkiye; it is NOT a claimed GPS location.
 * Provider availability / phone rendering have not yet been device-verified.
 */
@Composable
fun OnlineMap(
    modifier: Modifier = Modifier,
    route: NavigationRoute?,
    destination: GeoPoint?,
    gps: GpsSnapshot,
    weatherSummary: String = "",
    onSelectDestination: (GeoPoint) -> Unit,
    stopMarkers: List<ChargerCandidate> = emptyList(),
    onStopSelected: (ChargerCandidate) -> Unit = {},
    focusedStop: ChargerCandidate? = null,
    onLoadFailed: () -> Unit = {},
    onStyleReady: () -> Unit = {},
    canChangeLayer: Boolean = true,
) {
    val context = LocalContext.current
    val layerPreferences = remember(context) { MapLayerPreferences(context) }
    val layer by layerPreferences.selected.collectAsState(initial = MapLayer.DARK)
    val mapTilerStore = remember(context) { MapTilerKeyStore(context) }
    val mapTilerKey by mapTilerStore.key.collectAsState(initial = null)
    val styleUrl = MapStyleResolver.url(layer, mapTilerKey)
        ?: MapLayer.DARK.styleUrl
    val chosenStyleUrl = rememberUpdatedState(styleUrl)
    val scope = rememberCoroutineScope()
    val mapStyleApplied = remember { mutableStateOf("") }
    var followCamera by remember { mutableStateOf(false) }
    var tiltedCamera by remember { mutableStateOf(false) }
    var northUp by remember { mutableStateOf(false) }
    val layerMenu = remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapReady = remember { mutableStateOf<MapLibreMap?>(null) }
    val chosenDestination = rememberUpdatedState(onSelectDestination)
    val chosenStop = rememberUpdatedState(onStopSelected)
    val displayedStops = rememberUpdatedState(stopMarkers)
    val mapFailureCallback = rememberUpdatedState(onLoadFailed)
    val mapReadyCallback = rememberUpdatedState(onStyleReady)
    val mapView = remember(context) {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
            addOnDidFailLoadingMapListener(
                MapView.OnDidFailLoadingMapListener { _ ->
                    mapFailureCallback.value()
                },
            )
            getMapAsync { map ->
                map.addOnMapLongClickListener { location ->
                    chosenDestination.value(GeoPoint(location.latitude, location.longitude))
                    true
                }
                map.setOnMarkerClickListener { marker ->
                    val found = displayedStops.value.firstOrNull {
                        abs(it.latitude - marker.position.latitude) < 0.00001 &&
                        abs(it.longitude - marker.position.longitude) < 0.00001
                    }
                    if(found!=null) { chosenStop.value(found);true } else false
                }
                mapStyleApplied.value = chosenStyleUrl.value
                map.setStyle(chosenStyleUrl.value) {
                    mapReady.value = map
                    mapReadyCallback.value()
                }
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(39.0, 35.0), 5.2))
            }
        }
    }

    DisposableEffect(mapView, lifecycleOwner) {
        val lifecycle = lifecycleOwner.lifecycle
        var destroyed = false
        fun destroyOnce() {
            if (!destroyed) {
                destroyed = true
                mapView.onDestroy()
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> destroyOnce()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStart()
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onResume()
        onDispose {
            lifecycle.removeObserver(observer)
            mapReady.value = null
            if (!destroyed) {
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
                destroyOnce()
            }
        }
    }

    val loadedMap = mapReady.value
    LaunchedEffect(styleUrl) {
        val map = mapReady.value
        if (map != null && styleUrl != mapStyleApplied.value) {
            mapReady.value = null
            map.setStyle(styleUrl) {
                mapStyleApplied.value = styleUrl
                mapReady.value = map
                mapReadyCallback.value()
            }
        }
    }
    LaunchedEffect(loadedMap, focusedStop) {
        if(loadedMap!=null && focusedStop!=null) {
            followCamera = false
            loadedMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                LatLng(focusedStop.latitude,focusedStop.longitude),12.0))
        }
    }
    val gpsFresh = rememberGpsFreshness(gps)
    // Takip kapalıyken de 2B/3B değişimi mevcut harita merkezinde uygulanır.
    LaunchedEffect(loadedMap, tiltedCamera, northUp) {
        val map = loadedMap ?: return@LaunchedEffect
        if (followCamera) return@LaunchedEffect
        val current = map.cameraPosition
        val camera = CameraPosition.Builder()
            .target(current.target)
            .zoom(current.zoom)
            .bearing(if (northUp) 0.0 else current.bearing)
            .tilt(if (tiltedCamera) 45.0 else 0.0)
            .build()
        map.animateCamera(CameraUpdateFactory.newCameraPosition(camera))
    }
    // Takip isteğe bağlıdır; GPS güncel değilse kamera sahte konuma gitmez.
    // GNSS course hareket yönüdür; pusula okumak gibi yorumlanmaz.
    LaunchedEffect(loadedMap, gps.point, gpsFresh, gps.courseDegrees,
        followCamera, tiltedCamera, northUp) {
        val map = loadedMap ?: return@LaunchedEffect
        val point = gps.point ?: return@LaunchedEffect
        if (!followCamera || !gpsFresh) return@LaunchedEffect
        val camera = CameraPosition.Builder()
            .target(LatLng(point.latitude, point.longitude))
            .zoom(15.0)
            .bearing(if (northUp) 0.0 else gps.courseDegrees ?: 0.0)
            .tilt(if (tiltedCamera) 45.0 else 0.0)
            .build()
        map.animateCamera(CameraUpdateFactory.newCameraPosition(camera))
    }
    LaunchedEffect(loadedMap, route, destination, stopMarkers, gps.point, gpsFresh) {
        val currentMap = loadedMap ?: return@LaunchedEffect
        var currentLine: Polyline? = null
        var passedLine: Polyline? = null
        var currentMarker: Marker? = null
        val chargingMarkers = mutableListOf<Marker>()
        try {
            if (route != null) {
                val position = if (gpsFresh && gps.point != null)
                    RouteProgress.locate(route, gps.point) else null
                val progress = if (position != null && route.distanceMeters > 0.0)
                    ((route.distanceMeters - position.remainingMeters) / route.distanceMeters)
                        .coerceIn(0.0, 1.0) else 0.0
                val parts = RoutePolylineProgress.split(route.geometry, progress)
                if (parts.passed.size >= 2) {
                    passedLine = currentMap.addPolyline(
                        PolylineOptions()
                            .addAll(parts.passed.map { LatLng(it.latitude, it.longitude) })
                            .color(android.graphics.Color.rgb(100, 110, 117))
                            .width(8f),
                    )
                }
                if (parts.remaining.size >= 2) {
                    currentLine = currentMap.addPolyline(
                        PolylineOptions()
                            .addAll(parts.remaining.map { LatLng(it.latitude, it.longitude) })
                            .color(android.graphics.Color.rgb(61, 220, 132))
                            .width(8f),
                    )
                }
            }
            if (destination != null) {
                currentMarker = currentMap.addMarker(
                    MarkerOptions()
                        .position(LatLng(destination.latitude, destination.longitude))
                        .title("Seçilen hedef"),
                )
            }
            stopMarkers.forEach { station ->
                val color=PlannerMarkerPolicy.color(station)
                val bitmap=Bitmap.createBitmap(44,44,Bitmap.Config.ARGB_8888)
                val draw=AndroidCanvas(bitmap)
                val paint=Paint(Paint.ANTI_ALIAS_FLAG)
                paint.color=android.graphics.Color.WHITE
                draw.drawCircle(22f,22f,21f,paint)
                paint.color=color
                draw.drawCircle(22f,22f,17f,paint)
                val icon=IconFactory.getInstance(context).fromBitmap(bitmap)
                chargingMarkers+=currentMap.addMarker(MarkerOptions()
                    .position(LatLng(station.latitude,station.longitude))
                    .title(PlannerMarkerPolicy.label(station).take(120))
                    .snippet("doluluk bilinmiyor • veri TAHMİN").icon(icon))
            }
            awaitCancellation()
        } finally {
            currentLine?.let { currentMap.removePolyline(it) }
            passedLine?.let { currentMap.removePolyline(it) }
            currentMarker?.let { currentMap.removeMarker(it) }
            chargingMarkers.forEach { currentMap.removeMarker(it) }
        }
    }

    val position = if (route != null && gpsFresh &&
        gps.point != null
    ) RouteProgress.locate(route, gps.point) else null
    val maneuver = position?.nextManeuverIndex?.let { route?.maneuvers?.getOrNull(it) }
    Box(modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        Box(modifier = Modifier.align(Alignment.TopStart)) {
            OutlinedButton(onClick = { layerMenu.value = true }, enabled = canChangeLayer) {
                Text("Katman: " + layer.titleTr, fontSize = 14.sp, color = Color.White)
            }
            DropdownMenu(expanded = layerMenu.value,
                onDismissRequest = { layerMenu.value = false }) {
                MapLayer.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.titleTr + if (option == layer) " ✓" else "") },
                        enabled = MapStyleResolver.url(option, mapTilerKey) != null,
                        onClick = {
                            layerMenu.value = false
                            scope.launch { layerPreferences.choose(option) }
                        },
                    )
                }
            }
        }
        if (canChangeLayer) {
            Column(
                modifier = Modifier.align(Alignment.CenterEnd)
                    .background(Color(0x99000000)).padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                OutlinedButton(
                    enabled = gpsFresh && gps.point != null,
                    onClick = { followCamera = !followCamera },
                ) { Text(if (followCamera) "Takibi kapat" else "Konumumu izle") }
                OutlinedButton(onClick = { tiltedCamera = !tiltedCamera }) {
                    Text(if (tiltedCamera) "2B görünüm" else "3B açı (45°)")
                }
                OutlinedButton(onClick = { northUp = !northUp }) {
                    Text(if (northUp) "Hareket yönü" else "Kuzey yukarı")
                }
                if (!gpsFresh || gps.point == null) {
                    Text("GPS güncel değil", color = Color.White, fontSize = 12.sp)
                }
            }
        }
        Text(
            when {
                route != null -> "OpenFreeMap • rota çizgisi (sahada doğrulanmadı)"
                destination != null -> "Hedef seçildi • Rota sekmesinde hesapla"
                else -> "OpenFreeMap • hedef seçmek için haritaya uzun bas"
            },
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.TopCenter)
                .background(Color(0xC9000000)).padding(8.dp),
        )
        if (weatherSummary.isNotBlank()) {
            Text(weatherSummary, color = Color.White, fontSize = 14.sp,
                modifier = Modifier.align(Alignment.TopEnd)
                    .background(Color(0xD9000000)).padding(6.dp))
        }
        Text(MapAttribution.LABEL, color = Color.White, fontSize = 14.sp,
            modifier = Modifier.align(Alignment.BottomEnd)
                .background(Color(0xDD000000)).padding(horizontal = 7.dp, vertical = 4.dp))
        if (maneuver != null) {
            Text(
                "SONRAKİ MANEVRA • ${maneuver.instructionTr}\n" +
                    "Yaklaşık: ${position.distanceToManeuverMeters?.toInt() ?: "--"} m",
                color = Color.White, fontSize = 14.sp,
                modifier = Modifier.align(Alignment.BottomStart)
                    .background(Color(0xD9000000)).padding(10.dp),
            )
        }
    }
}
```

---

## 12. `app/src/main/java/app/evcockpit/ui/RouteSettingsScreen.kt`

```kotlin
package app.evcockpit.ui

import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import app.evcockpit.ui.NightlyUpdatePanel
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

## 13. `app/src/test/java/app/evcockpit/ui/MapLayerPreferencesTest.kt`

```kotlin
package app.evcockpit.ui

import org.junit.Assert.*
import org.junit.Test

class MapLayerPreferencesTest {
    @Test fun openFreeMapKatmanlariAnahtarIstemiyor() {
        val open = listOf(MapLayer.DARK, MapLayer.LIBERTY, MapLayer.BRIGHT, MapLayer.POSITRON)
        assertEquals(6, MapLayer.entries.size)
        open.forEach { layer ->
            assertTrue(layer.styleUrl.startsWith("https://tiles.openfreemap.org/styles/"))
            assertEquals(layer.styleUrl, MapStyleResolver.url(layer, null))
        }
    }

    @Test fun uyduVeTopografiAnahtarsizAcilamaz() {
        assertNull(MapStyleResolver.url(MapLayer.SATELLITE, null))
        assertNull(MapStyleResolver.url(MapLayer.TOPOGRAPHIC, ""))
        assertNull(MapStyleResolver.url(MapLayer.TOPOGRAPHIC, "url?key=evil"))
        assertEquals(
            "https://api.maptiler.com/maps/satellite/style.json?key=OrnekKey_123",
            MapStyleResolver.url(MapLayer.SATELLITE, "OrnekKey_123"),
        )
        assertEquals(
            "https://api.maptiler.com/maps/topo/style.json?key=OrnekKey_123",
            MapStyleResolver.url(MapLayer.TOPOGRAPHIC, "OrnekKey_123"),
        )
    }

    @Test fun kayitliKatmanVeBozukTercihGuvenliCozulur() {
        assertEquals(MapLayer.DARK, MapLayer.fromId(null))
        assertEquals(MapLayer.DARK, MapLayer.fromId(""))
        assertEquals(MapLayer.DARK, MapLayer.fromId("https://evil.example"))
        assertEquals(MapLayer.SATELLITE, MapLayer.fromId("satellite"))
        assertEquals(MapLayer.TOPOGRAPHIC, MapLayer.fromId("topographic"))
    }
}
```
