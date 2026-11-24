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
