package app.evcockpit.ui.unified

import android.os.SystemClock
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.evcockpit.obd.ObdViewModel
import app.evcockpit.ui.tesla.DriveModeViewModel
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.evcockpit.CockpitApplication
import app.evcockpit.R
import app.evcockpit.navigation.PlaceAutocompleteViewModel
import app.evcockpit.navigation.PlaceField
import app.evcockpit.navigation.PhotonPlace
import app.evcockpit.ui.finalhmi.CockpitDesignTokens
import app.evcockpit.ui.unified.map.*
import app.evcockpit.ui.unified.sheet.*

/** C4: real place and route preview; production NavHost switches in C8. */
@Composable
fun UnifiedMapScreen(
    vm: UnifiedViewModel = viewModel(),
    onExit: () -> Unit,
) {
    val t = CockpitDesignTokens
    val state by vm.state.collectAsStateWithLifecycle()
    val live: C6NavigationViewModel = viewModel()
    val progress by live.state.collectAsStateWithLifecycle()
    val voiceEnabled by live.voiceEnabled.collectAsStateWithLifecycle()
    val voiceStatus by live.voiceStatus.collectAsStateWithLifecycle()
    val obd: ObdViewModel = viewModel()
    val gps by obd.gps.collectAsStateWithLifecycle()
    val drive: DriveModeViewModel = viewModel()
    val driveState by drive.state.collectAsStateWithLifecycle()
    val navigating = state.mode == SheetMode.Navigating
    val navRoute = state.currentRoute?.takeIf { navigating }
    val lifecycle = LocalLifecycleOwner.current
    DisposableEffect(lifecycle, navRoute) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> live.track(navRoute)
                Lifecycle.Event.ON_STOP -> live.suspendTracking()
                else -> Unit
            }
        }
        lifecycle.lifecycle.addObserver(observer)
        if (lifecycle.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) live.track(navRoute)
        onDispose {
            lifecycle.lifecycle.removeObserver(observer)
            live.suspendTracking()
        }
    }
    val gpsPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true) obd.startGps()
        else obd.denyGps()
    }
    val c5: C5RouteDataViewModel = viewModel()
    val c5Data by c5.state.collectAsStateWithLifecycle()
    val routeId = state.currentRoute?.id
    LaunchedEffect(routeId) { c5.selectRoute(routeId) }
    DisposableEffect(c5) { onDispose { c5.selectRoute(null) } }
    val autocomplete: PlaceAutocompleteViewModel = viewModel()
    val search by autocomplete.state.collectAsStateWithLifecycle()
    var field by remember { mutableStateOf<PlaceField?>(null) }
    val context = LocalContext.current
    val host = LocalSharedMapHost.current
    val slot = remember { Any() }
    val density = LocalDensity.current
    val focus = LocalFocusManager.current
    val failure = UserMessage(stringResource(R.string.a0b_c4_failed))
    val invalid = UserMessage(stringResource(R.string.a0b_c4_invalid))
    val gpsFailed = UserMessage(stringResource(R.string.a0b_c4_gps_failed))
    val gpsLabel = stringResource(R.string.a0b_c4_gps_origin)
    val addressSource = stringResource(R.string.a0b_c4_geocoder_source)
    val startGpsMessage = UserMessage(stringResource(R.string.a0b_c6_gps_required))
    fun select(fieldValue: PlaceField, option: PhotonPlace) {
        val place = Place(
            "${option.point.latitude},${option.point.longitude}", option.label,
            option.point.latitude, option.point.longitude, addressSource,
        )
        if (!place.valid) return
        focus.clearFocus()
        autocomplete.selected(fieldValue, option)
        vm.dispatch(if (fieldValue == PlaceField.ORIGIN)
            UnifiedEvent.ChooseOrigin(place) else UnifiedEvent.ChooseDestination(place))
        field = null
        vm.dispatch(UnifiedEvent.SetDetent(SheetDetent.HALF))
    }
    val overlay = remember(state.origin, state.destination, state.routes, state.selectedRouteIndex, c5Data, routeId, progress.vehicle, navigating) {
        val base = C5Projection.overlay(C4RouteOverlays.from(state), c5Data, routeId)
        base.copy(vehicle = progress.vehicle,
            alternativeRoutes = if (navigating) emptyList() else base.alternativeRoutes)
    }
    LaunchedEffect(host?.map, navigating, host?.activeSlot) {
        if (host != null && host.activeSlot === slot) {
            host.cameraController?.setMode(if (navigating) FollowMode.FOLLOW_BEARING else FollowMode.OFF)
            if (navigating) progress.vehicle?.let { host.cameraController?.follow(it) }
        }
    }
    LaunchedEffect(host, host?.activeSlot, overlay) {
        if (host != null && host.activeSlot === slot) host.updateOverlays(overlay)
    }
    DisposableEffect(host, slot) {
        onDispose {
            if (host != null && host.activeSlot === slot) {
                host.cameraController?.setMode(FollowMode.OFF)
                host.updateOverlays(OverlayModel())
            }
            live.endNavigation()
        }
    }
    BackHandler {
        focus.clearFocus()
        field = null
        if (navigating) {
            live.endNavigation()
            vm.dispatch(UnifiedEvent.StopNavigation)
        } else vm.dispatch(UnifiedEvent.Back)
    }
    LaunchedEffect(state.exitRequested) {
        if (state.exitRequested) {
            vm.dispatch(UnifiedEvent.ExitConsumed)
            onExit()
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(t.background)) {
        val heightPx = with(density) { maxHeight.toPx() }
        val topPx = with(density) { t.spaceM.toPx().toInt() }
        val bottomPx = (heightPx * t.sheetHalfFraction).toInt()
        LaunchedEffect(host?.readyMap, host?.activeSlot, overlay.selectedRoute, heightPx, navigating) {
            if (!navigating && host != null && host.activeSlot === slot && overlay.selectedRoute.size >= 2 &&
                heightPx > topPx + bottomPx) {
                runCatching { host.cameraController?.fitRoute(overlay.selectedRoute, topPx, bottomPx) }
            }
        }
        MapHostViewport(
            modifier = Modifier.fillMaxSize(),
            unavailableMessage = stringResource(R.string.a0b_unified_map_no_data),
            requestedSlot = slot,
        )
        if (navigating) {
            NavigationPanel(
                progress, voiceEnabled, voiceStatus,
                onVoice = live::toggleVoice,
                onStop = {
                    live.endNavigation()
                    vm.dispatch(UnifiedEvent.StopNavigation)
                },
                onRecenter = {
                    host?.cameraController?.setMode(FollowMode.FOLLOW_BEARING)
                    progress.vehicle?.let { host?.cameraController?.follow(it) }
                },
                onReroute = {
                    val fix = progress.vehicle
                    if (fix != null && C6NavigationPolicy.validFix(gps, SystemClock.elapsedRealtime()) &&
                        driveState.allowComplexActions) {
                        live.endNavigation()
                        vm.dispatch(UnifiedEvent.StopNavigation)
                        vm.dispatch(UnifiedEvent.ChooseOrigin(Place("gps", gpsLabel,
                            fix.point.latitude, fix.point.longitude, "GPS")))
                        vm.calculateRoutes(failure, invalid)
                    }
                },
                canReroute = progress.vehicle != null && driveState.allowComplexActions,
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(t.spaceM),
            )
        } else {
        if (state.detent == SheetDetent.PEEK) {
            Button(onClick = { vm.dispatch(UnifiedEvent.SetDetent(SheetDetent.FULL)) },
                modifier = Modifier.align(Alignment.TopCenter)
                    .padding(t.spaceM).heightIn(min = t.target)) {
                Text(stringResource(R.string.a0b_unified_search_where))
            }
        }
        ThreeDetentSheet(
            detent = state.detent,
            onDetent = { vm.dispatch(UnifiedEvent.SetDetent(it)) },
            modifier = Modifier.fillMaxSize(),
        ) {
            if (state.mode == SheetMode.Idle || state.mode is SheetMode.RoutePreview) {
                Text(stringResource(R.string.a0b_unified_route),
                    fontSize = t.titleText, color = t.text)
                for (currentField in listOf(PlaceField.ORIGIN, PlaceField.DESTINATION)) {
                    val origin = currentField == PlaceField.ORIGIN
                    val selected = if (origin) state.origin else state.destination
                    val query = if (origin) search.originText else search.destinationText
                    val options = if (origin) search.originResults else search.destinationResults
                    val status = if (origin) search.originStatus else search.destinationStatus
                    val loading = if (origin) search.searchingOrigin else search.searchingDestination
                    OutlinedTextField(
                        value = selected?.name ?: query,
                        onValueChange = { value ->
                            if (selected != null) vm.dispatch(if (origin)
                                UnifiedEvent.ChooseOrigin(null) else UnifiedEvent.ChooseDestination(null))
                            field = currentField
                            vm.dispatch(UnifiedEvent.SetDetent(SheetDetent.FULL))
                            autocomplete.onTextChanged(currentField, value)
                        },
                        label = { Text(stringResource(if (origin) R.string.a0b_c4_origin else R.string.a0b_c4_destination)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().heightIn(min = t.target)
                            .onFocusChanged {
                                if (it.isFocused) {
                                    field = currentField
                                    vm.dispatch(UnifiedEvent.SetDetent(SheetDetent.FULL))
                                }
                            },
                    )
                    if (field == currentField && selected == null) {
                        Text(status, color = t.secondary, fontSize = t.labelText)
                        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                        options.forEach { option ->
                            OutlinedButton(onClick = { select(currentField, option) },
                                modifier = Modifier.fillMaxWidth().heightIn(min = t.target)) {
                                Text(option.label)
                            }
                        }
                    }
                    if (origin) {
                        if (!gps.active) OutlinedButton(onClick = {
                            gpsPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION))
                        }, modifier = Modifier.fillMaxWidth().heightIn(min = t.target)) {
                            Text(stringResource(R.string.a0b_c6_start_gps))
                        }
                        OutlinedButton(onClick = {
                            val gps = (context.applicationContext as? CockpitApplication)
                                ?.locationRepository?.state?.value
                            val point = gps?.point
                            if (gps != null && point != null && gps.isFresh(SystemClock.elapsedRealtime()) &&
                                (gps.accuracyMeters ?: 999f) <= 100f) {
                                autocomplete.useGpsOrigin()
                                vm.dispatch(UnifiedEvent.ChooseOrigin(Place(
                                    "gps", gpsLabel, point.latitude, point.longitude, "GPS")))
                                field = null
                                vm.dispatch(UnifiedEvent.SetDetent(SheetDetent.HALF))
                            } else vm.dispatch(UnifiedEvent.AddBanner(Banner(gpsFailed, BannerKind.WARNING)))
                        }, modifier = Modifier.fillMaxWidth().heightIn(min = t.target)) {
                            Text(stringResource(R.string.a0b_c4_gps))
                        }
                    }
                }
                Button(onClick = {
                    focus.clearFocus()
                    field = null
                    vm.calculateRoutes(failure, invalid)
                }, enabled = state.origin != null && state.destination != null && state.routes != Loadable.Loading,
                    modifier = Modifier.fillMaxWidth().heightIn(min = t.target)) {
                    Text(stringResource(R.string.a0b_c4_calculate))
                }
                when (val routes = state.routes) {
                    Loadable.Loading -> {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text(stringResource(R.string.a0b_c4_loading), color = t.secondary)
                    }
                    is Loadable.Failed -> Text(routes.reason.textTr, color = t.caution)
                    is Loadable.Ready -> {
                        routes.value.forEachIndexed { index, route ->
                            val selected = state.selectedRouteIndex == index
                            OutlinedButton(onClick = { vm.dispatch(UnifiedEvent.ChooseAlternative(index)) },
                                modifier = Modifier.fillMaxWidth().heightIn(min = t.target)) {
                                Text(stringResource(if (selected) R.string.a0b_c4_selected_option else R.string.a0b_c4_option,
                                    index + 1, route.distanceMeters / 1000.0, (route.durationSeconds / 60.0).toInt()))
                            }
                        }
                        if (routes.value.size == 1) Text(stringResource(R.string.a0b_c4_single), color = t.secondary)
                        state.currentRoute?.let { route ->
                            Text(stringResource(R.string.a0b_c4_source, route.source), color = t.secondary)
                            Button(onClick = {
                                if (C6NavigationPolicy.validFix(gps, SystemClock.elapsedRealtime())) {
                                    focus.clearFocus()
                                    vm.dispatch(UnifiedEvent.StartNavigation)
                                } else {
                                    vm.dispatch(UnifiedEvent.AddBanner(Banner(startGpsMessage, BannerKind.WARNING)))
                                }
                            }, modifier = Modifier.fillMaxWidth().heightIn(min = t.target)) {
                                Text(stringResource(R.string.a0b_c6_start))
                            }
                            C5RoutePanel(route, c5Data) { c5.refresh(route) }
                        }
                    }
                    Loadable.Idle -> Unit
                }
            } else when (state.mode) {
                SheetMode.Navigating -> Unit
                is SheetMode.StopDetail -> StopDetailPanel(state)
                SheetMode.Energy -> EnergyPanel(state)
                else -> Unit
            }
            state.banners.lastOrNull()?.let { banner ->
                Surface(shape = t.cornerSmall, color = t.panelRaised) {
                    Text(banner.message.textTr, color = t.caution,
                        modifier = Modifier.padding(t.spaceM), fontSize = t.bodyText)
                }
            }
        }
        }
    }
}
