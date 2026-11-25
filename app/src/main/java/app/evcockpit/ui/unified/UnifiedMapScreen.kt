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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.evcockpit.CockpitApplication
import app.evcockpit.R
import app.evcockpit.ui.MapLayer
import app.evcockpit.ui.MapLayerPreferences
import app.evcockpit.ui.MapTilerKeyStore
import app.evcockpit.ui.MapStyleResolver
import org.maplibre.android.camera.CameraUpdateFactory
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
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
    modifier: Modifier = Modifier,
) {
    val t = CockpitDesignTokens
    val state by vm.state.collectAsStateWithLifecycle()
    val library: C7LibraryViewModel = viewModel()
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
    val layerPrefs = remember(context.applicationContext) { MapLayerPreferences(context.applicationContext) }
    val mapKeys = remember(context.applicationContext) { MapTilerKeyStore(context.applicationContext) }
    val preferredLayer by layerPrefs.selected.collectAsStateWithLifecycle(MapLayer.DARK)
    val mapKey by mapKeys.key.collectAsStateWithLifecycle(null)
    val scope = rememberCoroutineScope()
    var layerMenu by remember { mutableStateOf(false) }
    val host = LocalSharedMapHost.current
    val slot = remember { Any() }
    LaunchedEffect(host, preferredLayer, mapKey, host?.activeSlot) {
        if (host != null && host.activeSlot === slot) {
            val style = MapStyleResolver.url(preferredLayer, mapKey)
            if (style != null) host.requestStyle(preferredLayer, style)
            else host.requestStyle(MapLayer.DARK)
        }
    }
    val density = LocalDensity.current
    val focus = LocalFocusManager.current
    val failure = UserMessage(stringResource(R.string.a0b_c4_failed))
    val invalid = UserMessage(stringResource(R.string.a0b_c4_invalid))
    val gpsFailed = UserMessage(stringResource(R.string.a0b_c4_gps_failed))
    val gpsLabel = stringResource(R.string.a0b_c4_gps_origin)
    val addressSource = stringResource(R.string.a0b_c4_geocoder_source)
    val startGpsMessage = UserMessage(stringResource(R.string.a0b_c6_gps_required))
    val restoreNotice = UserMessage(stringResource(R.string.a0b_c7_recalculate))
    val savedSource = stringResource(R.string.a0b_c7_source_saved)
    fun select(fieldValue: PlaceField, option: PhotonPlace) {
        val place = Place(
            "${option.point.latitude},${option.point.longitude}", option.label,
            option.point.latitude, option.point.longitude, addressSource,
        )
        if (!place.valid) return
        focus.clearFocus()
        autocomplete.selected(fieldValue, option)
        library.saveAddress("RECENT", place)
        vm.dispatch(if (fieldValue == PlaceField.ORIGIN)
            UnifiedEvent.ChooseOrigin(place) else UnifiedEvent.ChooseDestination(place))
        field = null
        vm.dispatch(UnifiedEvent.SetDetent(SheetDetent.HALF))
    }
    field?.let { currentField ->
        val origin = currentField == PlaceField.ORIGIN
        AddressSearchDialog(
            title = stringResource(if (origin) R.string.a0b_c4_origin else R.string.a0b_c4_destination),
            query = if (origin) search.originText else search.destinationText,
            options = if (origin) search.originResults else search.destinationResults,
            status = if (origin) search.originStatus else search.destinationStatus,
            loading = if (origin) search.searchingOrigin else search.searchingDestination,
            onQuery = { autocomplete.onTextChanged(currentField, it) },
            onRetry = { autocomplete.retry(currentField) },
            onChoose = { select(currentField, it) },
            onDismiss = { field = null },
        )
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
    BoxWithConstraints(modifier.fillMaxSize().background(t.background)) {
        val heightPx = with(density) { maxHeight.toPx() }
        val topPx = with(density) { t.spaceM.toPx().toInt() }
        val bottomPx = (heightPx * t.sheetHalfFraction).toInt()
        val attributionInset = with(density) { t.mapAttributionSafeInset.toPx().toInt() }
        val covered = if (navigating) 0 else when (state.detent) {
            SheetDetent.PEEK -> with(density) { t.sheetPeekHeight.toPx().toInt() }
            SheetDetent.HALF -> bottomPx
            SheetDetent.FULL -> 0
        }
        LaunchedEffect(host?.map, host?.activeSlot, covered) {
            if (host != null && host.activeSlot === slot) host.map?.uiSettings?.apply {
                setLogoMargins(attributionInset, 0, 0, attributionInset + covered)
                setAttributionMargins(0, 0, attributionInset, attributionInset + covered)
            }
        }
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
        Column(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(t.spaceXS),
            verticalArrangement = Arrangement.spacedBy(t.spaceXS)) {
            if (state.detent != SheetDetent.FULL) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(t.spaceXS)) {
                    val zoomIn = stringResource(R.string.a0b_c8_zoom_in)
                    val zoomOut = stringResource(R.string.a0b_c8_zoom_out)
                    OutlinedButton(onClick = { host?.map?.animateCamera(CameraUpdateFactory.zoomIn()) },
                        enabled = host?.map != null, modifier = Modifier.size(t.mapControlTouchTarget)
                            .semantics { contentDescription = zoomIn }, contentPadding = PaddingValues(t.spaceXXS)) { Text("+") }
                    OutlinedButton(onClick = { host?.map?.animateCamera(CameraUpdateFactory.zoomOut()) },
                        enabled = host?.map != null, modifier = Modifier.size(t.mapControlTouchTarget)
                            .semantics { contentDescription = zoomOut }, contentPadding = PaddingValues(t.spaceXXS)) { Text("−") }
                    Box {
                        OutlinedButton(onClick = { layerMenu = true }, modifier = Modifier.heightIn(min = t.mapControlTouchTarget)) {
                            Text(stringResource(R.string.a0b_c8_layers))
                        }
                        DropdownMenu(expanded = layerMenu, onDismissRequest = { layerMenu = false }) {
                            MapLayer.entries.forEach { layer ->
                                val style = MapStyleResolver.url(layer, mapKey)
                                DropdownMenuItem(text = { Text(layer.titleTr + if (style == null)
                                    stringResource(R.string.a0b_c8_key_required) else "") },
                                    enabled = style != null, onClick = {
                                        layerMenu = false
                                        if (style != null) {
                                            host?.selectLayer(layer, style)
                                            scope.launch { layerPrefs.choose(layer) }
                                        }
                                    })
                            }
                        }
                    }
                    if (state.detent == SheetDetent.PEEK) Button(onClick = {
                        vm.dispatch(UnifiedEvent.SetDetent(SheetDetent.FULL))
                    }, modifier = Modifier.weight(1f).heightIn(min = t.mapControlTouchTarget),
                        contentPadding = PaddingValues(t.spaceXS)) {
                        Text(stringResource(R.string.a0b_unified_search_where), maxLines = 2)
                    }
                }
                if (host?.loadState == MapHostLoadState.ERROR) {
                    OutlinedButton(onClick = { host?.retry() }) { Text(stringResource(R.string.a0b_c8_map_retry)) }
                } else if (host?.loadState == MapHostLoadState.LOADING) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                host?.issue?.let { Text(it, color = t.caution, fontSize = t.labelText) }
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
                    val label = stringResource(if (origin) R.string.a0b_c4_origin else R.string.a0b_c4_destination)
                    OutlinedButton(onClick = {
                        focus.clearFocus()
                        autocomplete.onTextChanged(currentField, "")
                        field = currentField
                    }, modifier = Modifier.fillMaxWidth().heightIn(min = t.target)) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(label, color = t.secondary, fontSize = t.labelText)
                            Text(selected?.name ?: stringResource(R.string.a0b_search_pick), color = t.text,
                                maxLines = 2)
                        }
                    }
                }
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
                C7LibraryPanel(state, library,
                    onChoose = { which, item ->
                        focus.clearFocus()
                        field = null
                        val place = Place("saved:${item.point.latitude},${item.point.longitude}", item.label,
                            item.point.latitude, item.point.longitude, savedSource)
                        autocomplete.selected(which, PhotonPlace(item.label, item.point))
                        vm.dispatch(if (which == PlaceField.ORIGIN) UnifiedEvent.ChooseOrigin(place)
                            else UnifiedEvent.ChooseDestination(place))
                        vm.dispatch(UnifiedEvent.SetDetent(SheetDetent.HALF))
                    },
                    onOpen = { item ->
                        focus.clearFocus()
                        field = null
                        vm.restoreSavedRoute(item, restoreNotice)
                    },
                )
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
