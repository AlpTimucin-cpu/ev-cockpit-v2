# A0 0.7.0 — TAM KAYNAKLAR / BÖLÜM 1 (7 dosya)

Bu belgedeki kodlar `feature/a0-f1-product-main-20261124` dalında A0 için değiştirilmiş dosyaların **tam içeriğidir**. Tek satır atlanmamış, “gerisi aynı” kullanılmamıştır. Bu dosyalar zaten GitHub'a yüklüdür.

**Bölüm 1:** Gradle, CI, manifest, ana Activity, Ayarlar, OBD örnek denetimi, F1 ana gezinmesi.  
**Diğer dosyalar:** [Bölüm 2](./A0_070_TAM_KAYNAKLAR_BOLUM_2.md) · [Bölüm 3](./A0_070_TAM_KAYNAKLAR_BOLUM_3.md).  
**Sürüm:** 0.7.0 (51). **CI ve cihaz testi durumu:** A0 son commit durumuyla ayrıca doğrulanır; kodun mevcudiyeti çalışma kanıtı değildir.


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

      - name: Compile unsigned release and verify debug demo exclusion
        run: |
          set -euo pipefail
          gradle :app:testReleaseUnitTest :app:assembleRelease --stacktrace
          APK='app/build/outputs/apk/release/app-release-unsigned.apk'
          test -s "$APK"
          if unzip -p "$APK" 'classes*.dex' | strings | grep -q 'DemoDashboardMapper'; then
            echo 'ERROR: Debug DEMO mapper was found in release DEX.' >&2
            exit 1
          fi
          echo 'Release compiled; DEMO mapper not present in release DEX.'

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

## 2. `app/build.gradle.kts`

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
        versionCode = 51
        versionName = "0.7.0"
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
    // A0: NavHost sekme geçmişi; Apache-2.0, AAR ~1,2 MB (paket boyutu CI ile ölçülür).
    implementation("androidx.navigation:navigation-compose:2.8.5")
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

## 3. `app/src/main/AndroidManifest.xml`

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

## 4. `app/src/main/java/app/evcockpit/MainActivity.kt`

```kotlin
package app.evcockpit

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.evcockpit.navigation.NavigationViewModel
import app.evcockpit.navigation.PlaceAutocompleteViewModel
import app.evcockpit.obd.ManualSocStore
import app.evcockpit.obd.ObdLogShare
import app.evcockpit.obd.ObdViewModel
import app.evcockpit.obd.SafeTestReportExporter
import app.evcockpit.planui.PlannerViewModel
import app.evcockpit.planner.model.PlanResult
import app.evcockpit.planner.model.VehicleProfile
import app.evcockpit.storage.VehicleProfileStore
import app.evcockpit.ui.DisplayPreferences
import app.evcockpit.ui.ObdTestScreen
import app.evcockpit.ui.PlannerScreen
import app.evcockpit.ui.RouteScreen
import app.evcockpit.ui.RouteSettingsScreen
import app.evcockpit.ui.finalhmi.CockpitDesignTokens
import app.evcockpit.ui.finalhmi.FinalCockpitTheme
import app.evcockpit.ui.finalhmi.FinalDashboardTab
import app.evcockpit.ui.finalhmi.MapFirstDashboardScreen
import app.evcockpit.ui.finalhmi.MapFirstDashboardViewModel
import app.evcockpit.ui.tesla.DriveModeViewModel
import app.evcockpit.ui.tesla.SafeSocSelection
import app.evcockpit.weather.RouteHourlyWeatherViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A0: Tek ana Activity, NavHost başlangıcı HARİTA / F1.
 * Mola, medya ve eski PARK / ŞARJ arayüzü bu gezinmede bulunmaz.
 * OBD, GPS, rota, planlama depoları ve DataStore aynı kalır.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val displayPrefs = remember { DisplayPreferences(applicationContext) }
            val keepScreenOn by displayPrefs.keepScreenOn.collectAsStateWithLifecycle(false)
            val darkPref by displayPrefs.darkTheme.collectAsStateWithLifecycle(true)
            DisposableEffect(keepScreenOn) {
                if (keepScreenOn)
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
            }

            val f1: MapFirstDashboardViewModel = viewModel()
            val dashboard by f1.state.collectAsStateWithLifecycle()
            val obd: ObdViewModel = viewModel()
            val nav: NavigationViewModel = viewModel()
            val search: PlaceAutocompleteViewModel = viewModel()
            val planner: PlannerViewModel = viewModel()
            val weather: RouteHourlyWeatherViewModel = viewModel()
            val drive: DriveModeViewModel = viewModel()
            val scope = rememberCoroutineScope()

            val obdState by obd.state.collectAsStateWithLifecycle()
            val gps by obd.gps.collectAsStateWithLifecycle()
            val obdLog by obd.log.collectAsStateWithLifecycle()
            val logFilePath by obd.logFilePath.collectAsStateWithLifecycle()
            val diagnosisPath by obd.diagnosisFilePath.collectAsStateWithLifecycle()
            val diagnosisSummary by obd.diagnosisSummary.collectAsStateWithLifecycle()
            val devices by obd.devices.collectAsStateWithLifecycle()
            val obdMessage by obd.message.collectAsStateWithLifecycle()
            val driveState by drive.state.collectAsStateWithLifecycle()
            val navState by nav.state.collectAsStateWithLifecycle()
            val voiceState by nav.voice.collectAsStateWithLifecycle()
            val weatherSummary by weather.summary.collectAsStateWithLifecycle()
            val planState by planner.state.collectAsStateWithLifecycle()

            val manualStore = remember { ManualSocStore(applicationContext) }
            val manualSoc by manualStore.socPct.collectAsStateWithLifecycle(null)
            val profileStore = remember { VehicleProfileStore(applicationContext) }
            val profile by profileStore.profile.collectAsStateWithLifecycle(
                VehicleProfile.defaults())
            val socReading = SafeSocSelection.select(obdState, manualSoc, driveState)
            var diagnosisError by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(navState.route) {
                // İstek yalnız mevcut ViewModel'de yapılır; ekran G/Ç işlemez.
                weather.refresh(navState.route)
            }

            // İzinler yalnız ilgili kullanıcı eylemiyle istenir; açılışta izin yağmuru yok.
            val gpsPermission = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
            ) { result ->
                if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                ) obd.startGps() else obd.denyGps()
            }
            val bluetoothPermission = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
            ) { _ -> obd.refreshBonded() }

            val openDiagnostics: () -> Unit = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    bluetoothPermission.launch(
                        arrayOf(Manifest.permission.BLUETOOTH_CONNECT))
                } else {
                    obd.refreshBonded()
                }
            }

            FinalCockpitTheme {
                MapFirstDashboardScreen(
                    state = dashboard,
                    onDemoChange = f1::showDemo,
                    demoAvailable = f1.demoAvailable,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
                    tabContent = { tab, openDiagnosticRoute, navigateToTab ->
                        when (tab) {
                            FinalDashboardTab.ROTA -> RouteScreen(
                                state = navState,
                                gps = gps,
                                socPct = socReading.pct,
                                socSource = socReading.source,
                                vehicleProfile = profile,
                                voice = voiceState,
                                onToggleVoice = nav::toggleVoice,
                                onSpeakNextManeuver = nav::testNextManeuver,
                                onCalculate = nav::calculateSelected,
                                onSelectDestination = nav::selectDestination,
                                onSelectNamedDestination = nav::selectNamedDestination,
                                onSelectOrigin = nav::selectOrigin,
                                onUseGpsOrigin = nav::useGpsOrigin,
                                onClearDestinationSelection = nav::clearDestinationSelection,
                                onSelectAlternative = nav::selectAlternative,
                                onManualSoc = { value ->
                                    scope.launch { manualStore.save(value) }
                                },
                                searchVm = search,
                                routeWeather = weatherSummary,
                                onClear = nav::clear,
                            )
                            FinalDashboardTab.SARJ -> PlannerScreen(
                                vm = planner,
                                navigation = navState.route,
                                soc = socReading,
                                gps = gps,
                                onOpenRoute = { navigateToTab(FinalDashboardTab.ROTA) },
                                onOpenSettings = { navigateToTab(FinalDashboardTab.AYARLAR) },
                                onNavigate = { stops, destination ->
                                    nav.navigateVia(stops, destination)
                                    navigateToTab(FinalDashboardTab.ROTA)
                                },
                                onSelectDestination = nav::selectDestination,
                            )
                            FinalDashboardTab.ENERJI -> A0EnergyPlaceholder()
                            FinalDashboardTab.AYARLAR -> RouteSettingsScreen(
                                gps = gps,
                                darkTheme = darkPref,
                                keepScreenOn = keepScreenOn,
                                onDarkTheme = { value ->
                                    scope.launch { displayPrefs.setDarkTheme(value) }
                                },
                                onKeepScreenOn = { value ->
                                    scope.launch { displayPrefs.setKeepScreenOn(value) }
                                },
                                manualSocPct = manualSoc,
                                onSaveManualSoc = { value ->
                                    scope.launch { manualStore.save(value) }
                                },
                                onClearManualSoc = {
                                    scope.launch { manualStore.clear() }
                                },
                                onStartGps = {
                                    gpsPermission.launch(arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                    ))
                                },
                                onStopGps = obd::stopGps,
                                onOpenObdTest = {
                                    openDiagnostics()
                                    openDiagnosticRoute()
                                },
                            )
                            else -> Unit // HARİTA, NavHost'un ilk hedefidir.
                        }
                    },
                    diagnosticContent = {
                        ObdTestScreen(
                            state = obdState,
                            log = obdLog,
                            logFilePath = logFilePath,
                            diagnosisFilePath = diagnosisPath,
                            diagnosisSummary = diagnosisSummary,
                            devices = devices,
                            message = diagnosisError ?: obdMessage,
                            onRefresh = obd::refreshBonded,
                            onConnect = obd::connect,
                            onDiagnose = obd::diagnose,
                            onExportTestReport = { note ->
                                diagnosisError = null
                                scope.launch {
                                    try {
                                        val file = withContext(Dispatchers.IO) {
                                            SafeTestReportExporter.create(
                                                this@MainActivity, diagnosisSummary,
                                                obdLog.takeLast(200), note)
                                        }
                                        ObdLogShare.launch(this@MainActivity, file)
                                    } catch (_: Exception) {
                                        diagnosisError = "OBD test raporu paylaşılamadı."
                                    }
                                }
                            },
                            onShareDiagnosis = {
                                if (!ObdLogShare.launch(this@MainActivity, diagnosisPath))
                                    diagnosisError = "Tanı günlüğü bulunamadı."
                            },
                            onDemo = {
                                // OBD testindeki örnek değerler hiçbir zaman üretimde açılmaz.
                                if (BuildConfig.DEBUG) obd.startDemo()
                            },
                            onStop = obd::stop,
                            onShareLog = {
                                if (!ObdLogShare.launch(this@MainActivity, logFilePath))
                                    diagnosisError = "Paylaşılacak günlük bulunamadı."
                            },
                            onOpenBluetoothSettings = {
                                try {
                                    startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                                } catch (_: Exception) {
                                    diagnosisError = "Bluetooth ayarları açılamadı."
                                }
                            },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun A0EnergyPlaceholder() {
    val t = CockpitDesignTokens
    Surface(
        color = t.panel, shape = t.corner,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            Modifier.padding(t.spaceXL),
            verticalArrangement = Arrangement.spacedBy(t.spaceM),
        ) {
            Text("Enerji", color = t.text, fontSize = t.headText)
            Text("Henüz doğrulanmış bir yolculuk enerji kaydı bulunmuyor.",
                color = t.secondary, fontSize = t.bodyText)
            Text("Mevcut enerji ve şarj hesapları Şarj sekmesindeki gelişmiş planlayıcıda korunuyor.",
                color = t.secondary, fontSize = t.bodyText)
        }
    }
}
```

---

## 5. `app/src/main/java/app/evcockpit/ui/RouteSettingsScreen.kt`

```kotlin
package app.evcockpit.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import app.evcockpit.R
import app.evcockpit.location.GpsSnapshot
import app.evcockpit.navigation.EncryptedRouteKeyStore
import app.evcockpit.obd.ManualSocInput
import app.evcockpit.planner.model.SourcedValue
import app.evcockpit.planner.model.VehicleProfile
import app.evcockpit.storage.VehicleProfileStore
import app.evcockpit.ui.finalhmi.CockpitDesignTokens
import kotlinx.coroutines.launch

/**
 * A0: Yeni F1 paletiyle beş açık bölüm. Her eski ayar kendi deposuna yazar;
 * buradan F1 demo Activity'si veya eski PARK/ŞARJ ekranı açılmaz.
 */
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
    onOpenObdTest: () -> Unit = {},
) {
    val t = CockpitDesignTokens
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val routeKeys = remember(context) { EncryptedRouteKeyStore(context) }
    val orsConfigured by routeKeys.isConfigured.collectAsState(initial = false)
    val mapTiler = remember(context) { MapTilerKeyStore(context) }
    val mapTilerKey by mapTiler.key.collectAsState(initial = null)
    val profileStore = remember(context) { VehicleProfileStore(context.applicationContext) }
    val profile by profileStore.profile.collectAsState(initial = VehicleProfile.defaults())
    var compatibility by remember { mutableStateOf(DeviceCompatibilityDiagnostics.capture(context)) }

    var mapTilerInput by remember { mutableStateOf("") }
    var orsInput by remember { mutableStateOf("") }
    var socInput by remember { mutableStateOf("") }
    var dcInput by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().background(t.background)
            .verticalScroll(rememberScrollState())
            .padding(t.spaceM),
        verticalArrangement = Arrangement.spacedBy(t.spaceM),
    ) {
        SettingsSection(
            stringResource(R.string.a0_connections),
            stringResource(R.string.a0_connections_desc),
        ) {
            SettingsAction(stringResource(R.string.a0_obd_open), onClick = onOpenObdTest)
            SettingsNote(stringResource(R.string.a0_obd_note))
            Text(
                stringResource(R.string.a0_gps_status, gps.status),
                color = t.text, fontSize = t.bodyText,
            )
            Text(
                stringResource(R.string.a0_gps_accuracy,
                    gps.accuracyMeters?.toInt()?.toString()
                        ?: context.getString(R.string.a0_location_unknown),
                    gps.provider ?: context.getString(R.string.a0_provider_unknown)),
                color = t.secondary, fontSize = t.labelText,
            )
            SettingsAction(stringResource(R.string.a0_gps_start), onClick = onStartGps)
            SettingsAction(stringResource(R.string.a0_gps_stop),
                onClick = onStopGps, outlined = true)
            SettingsNote(stringResource(R.string.a0_gps_permission))
            SettingsNote(stringResource(R.string.a0_compat_header))
            SettingsNote(compatibility.safeReport())
            SettingsAction(stringResource(R.string.a0_compat_refresh), outlined = true) {
                compatibility = DeviceCompatibilityDiagnostics.capture(context)
            }
            SettingsAction(stringResource(R.string.a0_compat_copy), outlined = true) {
                val clipboard = context.getSystemService(ClipboardManager::class.java)
                message = if (clipboard != null) {
                    clipboard.setPrimaryClip(ClipData.newPlainText(
                        context.getString(R.string.a0_compat_clipboard_name),
                        compatibility.safeReport()))
                    context.getString(R.string.a0_compat_copy_ok)
                } else context.getString(R.string.a0_compat_copy_fail)
            }
            if (!compatibility.preciseLocationGranted)
                SettingsNote(stringResource(R.string.a0_gps_precise))
            SettingsNote(BackgroundSettingsGuide.explanation(
                Build.MANUFACTURER, Build.DISPLAY, Build.FINGERPRINT))
            SettingsAction(stringResource(R.string.a0_battery_settings), outlined = true) {
                message = context.getString(
                    if (BackgroundSettingsGuide.openBatteryOptimizationSettings(context))
                        R.string.a0_battery_opened else R.string.a0_battery_error)
            }
            SettingsAction(stringResource(R.string.a0_autostart_settings), outlined = true) {
                message = context.getString(
                    if (BackgroundSettingsGuide.openAutostartOrAppDetails(context))
                        R.string.a0_autostart_opened else R.string.a0_autostart_error)
            }
            SettingsNote(stringResource(R.string.a0_sys_settings_note))
        }

        SettingsSection(
            stringResource(R.string.a0_vehicle),
            stringResource(R.string.a0_vehicle_desc),
        ) {
            SettingsNote(stringResource(R.string.a0_dc_default,
                profile.maxDcKw.value.toInt()))
            SettingsNote(stringResource(R.string.a0_dc_observed,
                profile.observedDcKw?.value?.let { DashboardNumber(it) }
                    ?: context.getString(R.string.a0_none)))
            SettingsInput(
                value = dcInput,
                onChange = { dcInput = it },
                label = stringResource(R.string.a0_dc_label),
                keyboardType = KeyboardType.Decimal,
            )
            SettingsAction(stringResource(R.string.a0_dc_save)) {
                val kw = dcInput.trim().replace(',', '.').toDoubleOrNull()
                if (kw == null || !kw.isFinite() || kw !in 1.0..350.0)
                    message = context.getString(R.string.a0_dc_invalid)
                else {
                    val updated = profile.copy(
                        observedDcKw = SourcedValue(
                            kw, "KULLANICI ARAÇ EKRANI GÖZLEMİ",
                            false, "Tek oturum; azami DC şarj gücü doğrulanmadı"))
                    scope.launch {
                        try {
                            profileStore.save(updated)
                            dcInput = ""
                            message = null
                        } catch (_: Exception) {
                            message = context.getString(R.string.a0_dc_invalid)
                        }
                    }
                }
            }
            SettingsAction(
                stringResource(R.string.a0_dc_clear), outlined = true,
                enabled = profile.observedDcKw != null,
            ) {
                scope.launch { profileStore.save(profile.copy(observedDcKw = null)) }
            }
            SettingsAction(
                stringResource(R.string.a0_dc_reset), outlined = true,
            ) {
                scope.launch { profileStore.reset() }
            }
            SettingsNote(stringResource(R.string.a0_dc_note))
            SettingsNote(stringResource(R.string.a0_manual_soc))
            SettingsNote(stringResource(R.string.a0_manual_current,
                manualSocPct?.let { DashboardNumber(it) }
                    ?: context.getString(R.string.a0_none)))
            SettingsInput(
                value = socInput,
                onChange = { socInput = it },
                label = stringResource(R.string.a0_soc_label),
                keyboardType = KeyboardType.Decimal,
            )
            SettingsAction(stringResource(R.string.a0_soc_save)) {
                val parsed = ManualSocInput.parse(socInput)
                if (parsed == null) {
                    message = context.getString(R.string.a0_soc_invalid)
                } else {
                    onSaveManualSoc(parsed)
                    socInput = ""
                    message = null
                }
            }
            SettingsAction(stringResource(R.string.a0_soc_clear),
                enabled = manualSocPct != null, outlined = true,
                onClick = onClearManualSoc)
            SettingsNote(stringResource(R.string.a0_soc_note))
        }

        SettingsSection(
            stringResource(R.string.a0_map),
            stringResource(R.string.a0_map_desc),
        ) {
            SettingsNote(stringResource(R.string.a0_layers_note))
            SettingsNote(stringResource(R.string.a0_maptiler_current,
                context.getString(if (mapTilerKey.isNullOrBlank())
                    R.string.a0_key_absent else R.string.a0_key_saved)))
            SettingsInput(
                value = mapTilerInput,
                onChange = { mapTilerInput = it.take(256) },
                label = stringResource(R.string.a0_maptiler_label),
                keyboardType = KeyboardType.Password,
                secret = true,
            )
            SettingsAction(stringResource(R.string.a0_maptiler_save),
                enabled = mapTilerInput.isNotBlank()) {
                scope.launch {
                    try {
                        mapTiler.save(mapTilerInput)
                        mapTilerInput = ""
                        message = context.getString(R.string.a0_maptiler_saved)
                    } catch (_: Exception) {
                        message = context.getString(R.string.a0_maptiler_save_failed)
                    }
                }
            }
            SettingsAction(stringResource(R.string.a0_maptiler_clear),
                enabled = !mapTilerKey.isNullOrBlank(), outlined = true) {
                scope.launch {
                    try {
                        mapTiler.clear()
                        mapTilerInput = ""
                        message = context.getString(R.string.a0_maptiler_cleared)
                    } catch (_: Exception) {
                        message = context.getString(R.string.a0_maptiler_clear_failed)
                    }
                }
            }
            SettingsNote(stringResource(R.string.a0_maptiler_note))
            SettingsNote(stringResource(R.string.a0_ors_name))
            SettingsNote(stringResource(R.string.a0_ors_current,
                context.getString(if (orsConfigured)
                    R.string.a0_key_saved else R.string.a0_key_absent)))
            SettingsAction(stringResource(R.string.a0_ors_web), outlined = true) {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://account.heigit.org/")))
                } catch (_: Exception) {
                    message = context.getString(R.string.a0_ors_web_failed)
                }
            }
            SettingsInput(
                value = orsInput, onChange = { orsInput = it },
                label = stringResource(R.string.a0_ors_key_label),
                keyboardType = KeyboardType.Password, secret = true,
            )
            SettingsAction(stringResource(R.string.a0_ors_save),
                enabled = orsInput.isNotBlank()) {
                scope.launch {
                    try {
                        routeKeys.save(orsInput)
                        orsInput = ""
                        message = context.getString(R.string.a0_ors_saved)
                    } catch (_: Exception) {
                        message = context.getString(R.string.a0_ors_save_failed)
                    }
                }
            }
            SettingsAction(stringResource(R.string.a0_ors_clear),
                enabled = orsConfigured, outlined = true) {
                scope.launch {
                    try {
                        routeKeys.clear()
                        orsInput = ""
                        message = context.getString(R.string.a0_ors_cleared)
                    } catch (_: Exception) {
                        message = context.getString(R.string.a0_ors_clear_failed)
                    }
                }
            }
            SettingsNote(stringResource(R.string.a0_ors_note))
        }

        SettingsSection(
            stringResource(R.string.a0_units),
            stringResource(R.string.a0_units_desc),
        ) {
            SettingsNote(stringResource(R.string.a0_units_speed))
            SettingsNote(stringResource(R.string.a0_units_energy))
            SettingsNote(stringResource(R.string.a0_units_cost))
            SettingsSwitch(
                stringResource(R.string.a0_keep_screen),
                stringResource(R.string.a0_keep_screen_desc),
                keepScreenOn, onKeepScreenOn,
            )
            SettingsSwitch(
                stringResource(R.string.a0_theme_pref),
                stringResource(R.string.a0_theme_pref_desc),
                darkTheme, onDarkTheme,
            )
        }

        SettingsSection(
            stringResource(R.string.a0_about),
            stringResource(R.string.a0_about_desc),
        ) {
            SettingsNote(stringResource(R.string.a0_privacy))
            SettingsNote(stringResource(R.string.a0_privacy_note))
            SettingsNote(stringResource(R.string.a0_credit))
            SettingsNote(stringResource(R.string.a0_a0_state))
            Text(stringResource(R.string.a0_nightly),
                color = t.text, fontSize = t.titleText,
                fontWeight = FontWeight.SemiBold)
            NightlyUpdatePanel()
        }

        message?.let { Surface(color = t.panelRaised, shape = t.cornerSmall) {
            Text(it, color = t.secondary,
                modifier = Modifier.padding(t.spaceM), fontSize = t.bodyText)
        } }
    }
}

private fun DashboardNumber(number: Double): String =
    java.lang.String.format(java.util.Locale.forLanguageTag("tr-TR"), "%.1f", number)

@Composable
private fun SettingsSection(
    title: String, subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val t = CockpitDesignTokens
    Surface(
        color = t.panel, shape = t.corner,
        border = BorderStroke(t.cardOutlineWidth, t.outline),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(t.spaceL),
            verticalArrangement = Arrangement.spacedBy(t.spaceS),
        ) {
            Text(title, color = t.text, fontSize = t.headText,
                fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = t.secondary, fontSize = t.labelText)
            content()
        }
    }
}

@Composable
private fun SettingsNote(text: String) {
    val t = CockpitDesignTokens
    Text(text, color = t.secondary, fontSize = t.bodyText,
        modifier = Modifier.fillMaxWidth())
}

@Composable
private fun SettingsAction(
    label: String,
    outlined: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val t = CockpitDesignTokens
    if (outlined) {
        OutlinedButton(onClick = onClick, enabled = enabled,
            shape = t.cornerSmall,
            modifier = Modifier.fillMaxWidth().heightIn(min = t.target)) {
            Text(label, fontSize = t.bodyText)
        }
    } else {
        Button(onClick = onClick, enabled = enabled,
            shape = t.cornerSmall,
            modifier = Modifier.fillMaxWidth().heightIn(min = t.target)) {
            Text(label, fontSize = t.bodyText)
        }
    }
}

@Composable
private fun SettingsInput(
    value: String, onChange: (String) -> Unit,
    label: String, keyboardType: KeyboardType,
    secret: Boolean = false,
) {
    val t = CockpitDesignTokens
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (secret)
            PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = t.cornerSmall,
        modifier = Modifier.fillMaxWidth().heightIn(min = t.target),
    )
}

@Composable
private fun SettingsSwitch(
    label: String, explanation: String, checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val t = CockpitDesignTokens
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = t.target),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(t.spaceM),
    ) {
        Column(Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(t.spaceXXS)) {
            Text(label, color = t.text, fontSize = t.bodyText)
            Text(explanation, color = t.secondary, fontSize = t.labelText)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
```

---

## 6. `app/src/main/java/app/evcockpit/ui/ObdTestScreen.kt`

```kotlin
package app.evcockpit.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.evcockpit.obd.VehicleState
import app.evcockpit.planui.ObdEvidence

@SuppressLint("MissingPermission")
@Composable
fun ObdTestScreen(
    state: VehicleState,
    log: List<String>,
    logFilePath: String?,
    diagnosisFilePath: String?,
    diagnosisSummary: String,
    devices: List<BluetoothDevice>,
    message: String?,
    onRefresh: () -> Unit,
    onConnect: (BluetoothDevice) -> Unit,
    onDiagnose: () -> Unit,
    onShareDiagnosis: () -> Unit,
    onExportTestReport: (String) -> Unit,
    onDemo: () -> Unit,
    onStop: () -> Unit,
    onShareLog: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var carScannerNote by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    LaunchedEffect(log.size) { if (log.isNotEmpty()) listState.scrollToItem(log.lastIndex) }

    Row(modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            Modifier.weight(0.4f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("OBD bağlantı testi", fontSize = 18.sp)
            Text(state.statusText, fontSize = 14.sp)
            SystemCompatibilityPanel()
            Text("ELM327 / ECU veri güveni", fontSize = 14.sp)
            ObdEvidence.describe(state.connected,state.demo,state.socPct,state.speedKmh)
                .forEach { text ->
                    Text(text, fontSize = 11.sp)
                }
            message?.let { Text(it, color = Color(0xFFFFB300), fontSize = 12.sp) }
            Text("Cihazı telefonun Bluetooth ayarlarında eşleştirin (PIN genellikle 1234 veya 0000).", fontSize = 12.sp)
            OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                Text("Eşleşmiş cihazları yenile")
            }
            if (devices.isEmpty()) {
                Text("Eşleşmiş cihaz yok", fontSize = 13.sp)
                OutlinedButton(onClick = onOpenBluetoothSettings, modifier = Modifier.fillMaxWidth()) {
                    Text("Bluetooth ayarlarını aç")
                }
            } else {
                devices.forEach { device ->
                    Button(onClick = { onConnect(device) }, modifier = Modifier.fillMaxWidth()) {
                        Text("${device.name ?: "?"} (${device.address})", fontSize = 13.sp)
                    }
                }
            }
            Button(onClick = onDiagnose, enabled = devices.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()) {
                Text("Tanı modunu çalıştır")
            }
            Text("Araç park halinde Ready/kontak açıkken dene. Tanı, normal sorgulamayı durdurur.",
                fontSize = 12.sp)
            Text("Tanı özeti (ham ECU sonucu): " + diagnosisSummary,
                fontSize = 12.sp)
            OutlinedButton(onClick = onShareDiagnosis,
                enabled = diagnosisFilePath != null,
                modifier = Modifier.fillMaxWidth()) {
                Text("Ayrı tanı logunu paylaş")
            }
            diagnosisFilePath?.let { Text("Tanı kaydı: " + it, fontSize = 10.sp) }
            // A0: Üretim derlemesinde sahte OBD gösterme eylemi bulunmaz.
            if (app.evcockpit.BuildConfig.DEBUG) {
                OutlinedButton(onClick = onDemo, modifier = Modifier.fillMaxWidth()) {
                    Text("DEMO — gerçek araç verisi değil")
                }
            }
            OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) { Text("Bağlantıyı kes") }
            OutlinedButton(onClick = onShareLog, modifier = Modifier.fillMaxWidth()) {
                Text("OBD logunu paylaş")
            }
            Text("Car Scanner karşılaştırması (isteğe bağlı, elle girilir)", fontSize = 13.sp)
            Text("Aynı adaptörle okunan protokol/010D/SoC değerlerini not et. VIN, konum, MAC veya API anahtarı yazma.", fontSize = 11.sp)
            OutlinedTextField(value = carScannerNote,
                onValueChange = { carScannerNote = it.take(600) },
                modifier = Modifier.fillMaxWidth(), maxLines = 5,
                label = { Text("Car Scanner gözlem notu (maks. 600 karakter)") })
            Button(onClick = { onExportTestReport(carScannerNote) }, modifier = Modifier.fillMaxWidth()) {
                Text("Güvenli test raporunu dışa aktar")
            }
            Text("Test raporunda API anahtarı, koordinat ve VIN gizlenir.", fontSize = 12.sp)
            logFilePath?.let { Text("Kayıt: $it", fontSize = 10.sp) }
        }
        LazyColumn(Modifier.weight(0.6f).fillMaxSize(), state = listState) {
            items(log) { line ->
                val color = when {
                    line.contains("HATA") || line.contains("!") -> Color(0xFFFF8A80)
                    line.contains("= ") -> Color(0xFF69F0AE)
                    else -> Color.Unspecified
                }
                Text(line, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = color)
            }
        }
    }
}
```

---

## 7. `app/src/main/java/app/evcockpit/ui/finalhmi/MapFirstDashboardScreen.kt`

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
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
 * A0 üretim kabuğu: NavHost başlangıç hedefi HARİTA.
 * Diğer sekmeler A–C servislerinin mevcut ekranlarına bağlanabilir.
 * Kayıtlı veri bulunmazsa gerçek mapper yalnız boş değer gösterir.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun MapFirstDashboardScreen(
    state: DashboardUiState,
    onDemoChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    demoAvailable: Boolean = false,
    tabContent: @Composable (FinalDashboardTab, () -> Unit, (FinalDashboardTab) -> Unit) -> Unit = { tab, _, _ ->
        A0UnconnectedTab(tab)
    },
    diagnosticContent: @Composable () -> Unit = {
        Text("Tanı ekranı mevcut değil.", color = CockpitDesignTokens.secondary)
    },
) {
    val t = CockpitDesignTokens
    val controller = rememberNavController()
    val backEntry by controller.currentBackStackEntryAsState()
    val selected = FinalDashboardTab.entries.firstOrNull {
        it.name == backEntry?.destination?.route
    } ?: if (backEntry?.destination?.route == A0_DIAGNOSTIC_ROUTE)
        FinalDashboardTab.AYARLAR else FinalDashboardTab.HARITA
    val navigateTab: (FinalDashboardTab) -> Unit = { destination ->
        controller.navigate(destination.name) {
            popUpTo(controller.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    Column(modifier.fillMaxSize().background(t.background)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val compactHeader = maxWidth < t.compactMaxWidth
            DashboardTopStatus(state, compactHeader, onDemoChange, demoAvailable)
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

            NavHost(
                navController = controller,
                startDestination = FinalDashboardTab.HARITA.name,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(FinalDashboardTab.HARITA.name) {
                    if (wide) {
                        Row(
                            Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(t.spaceS),
                        ) {
                            MapHeroPane(
                                state = state,
                                onOpenRouteTab = { navigateTab(FinalDashboardTab.ROTA) },
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
                                onOpenRouteTab = { navigateTab(FinalDashboardTab.ROTA) },
                                modifier = Modifier.fillMaxWidth().height(t.phoneMapHeight),
                            )
                            DashboardRightPane(state)
                        }
                    }
                }
                FinalDashboardTab.entries.filter { it != FinalDashboardTab.HARITA }
                    .forEach { destination ->
                        composable(destination.name) {
                            tabContent(destination, {
                                controller.navigate(A0_DIAGNOSTIC_ROUTE) {
                                    launchSingleTop = true
                                }
                            }, navigateTab)
                        }
                    }
                composable(A0_DIAGNOSTIC_ROUTE) {
                    diagnosticContent()
                }
            }
        }

        FinalBottomBar(selected = selected, onSelect = navigateTab)
    }
}

private const val A0_DIAGNOSTIC_ROUTE = "obd_tani"

/** A0'da henüz bağlanmamış alanlarda sahte veri ya da boş etkileşim yok. */
@Composable
private fun A0UnconnectedTab(tab: FinalDashboardTab) {
    val t = CockpitDesignTokens
    Surface(color = t.panel, shape = t.corner, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.padding(t.spaceXL), verticalArrangement = Arrangement.spacedBy(t.spaceM)) {
            Text(tab.titleTr, color = t.text, fontSize = t.headText)
            Text("Bu alan henüz gerçek veri kaynağına bağlanmadı.",
                color = t.secondary, fontSize = t.bodyText)
        }
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
    demoAvailable: Boolean,
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
                if (demoAvailable) DemoModeButton(state.demo, onDemoChange)
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
                if (demoAvailable) DemoModeButton(state.demo, onDemoChange)
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
            Text(if (demo) "Demo modu: açık" else "Demo modu: kapalı", color = t.text,
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
