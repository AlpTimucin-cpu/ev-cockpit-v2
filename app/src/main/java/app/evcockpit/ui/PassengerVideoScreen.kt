package app.evcockpit.ui

import android.content.Intent
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import app.evcockpit.location.GpsSnapshot
import app.evcockpit.navigation.GeoPoint
import app.evcockpit.navigation.NavigationRoute
import app.evcockpit.passenger.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * PHONE ONLY. No Car App Library or Android Auto video surface.
 * The same player instance survives FULL/SPLIT/FLOATING layout switches.
 */
@Composable
fun PassengerVideoScreen(
    vm: PassengerPlayerModel,
    gps: GpsSnapshot,
    route: NavigationRoute?,
    destination: GeoPoint?,
    speedKmh: Double?,
    onSelectDestination: (GeoPoint) -> Unit,
) {
    val ctx = LocalContext.current
    val prefs = remember(ctx) { PassengerPreferences(ctx.applicationContext) }
    val layout by prefs.layout.collectAsState(initial = PassengerLayout.FULL)
    val speedLock by prefs.lock.collectAsState(initial = false)
    val scope = rememberCoroutineScope()
    val state by vm.state.collectAsState()
    var videoUrl by remember { mutableStateOf("") }
    var youtubeUrl by remember { mutableStateOf("") }
    var dragOffset by remember { mutableStateOf(IntOffset(24, 24)) }
    var molaStatus by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                ctx.contentResolver.takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            vm.playLocalDocument(uri)
        }
    }

    val webView = remember(ctx) {
        WebView(ctx).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = true
            webChromeClient = WebChromeClient()
        }
    }
    val currentYoutube = (state.source as? PassengerSource.YouTubeEmbed)?.videoId
    LaunchedEffect(currentYoutube) {
        if (currentYoutube != null) {
            webView.loadDataWithBaseURL("https://www.youtube.com",
                YouTubeIframeHtml.render(currentYoutube), "text/html", "UTF-8", null)
        } else webView.loadUrl("about:blank")
    }
    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }
    LaunchedEffect(speedLock, speedKmh) {
        if (VideoSpeedLock.shouldPause(speedLock, speedKmh)) vm.pause()
    }
    Column(Modifier.fillMaxSize().padding(6.dp)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = {
                molaStatus = when (MolaCompanionLauncher.open(ctx)) {
                    MolaCompanionLauncher.Result.OPENED ->
                        "Mola ayrı bir uygulama olarak açıldı."
                    MolaCompanionLauncher.Result.NOT_INSTALLED ->
                        "Mola bu telefonda kurulu değil. EV Cockpit oynatıcısı bağımsızdır."
                    MolaCompanionLauncher.Result.UNAVAILABLE ->
                        "Mola açılamadı. Uygulamanın kurulumunu kontrol et."
                }
            }) { Text("Mola'yı aç (harici)") }
            Button(onClick = { picker.launch(arrayOf("video/*")) }) { Text("Yerel dosya seç") }
            OutlinedTextField(value = videoUrl, onValueChange = { videoUrl = it },
                singleLine = true, label = { Text("Doğrudan HTTPS video URL") },
                modifier = Modifier.width(240.dp))
            Button(onClick = { vm.playDirectUrl(videoUrl) }) { Text("URL aç") }
            OutlinedTextField(value = youtubeUrl, onValueChange = { youtubeUrl = it },
                singleLine = true, label = { Text("YouTube video bağlantısı") },
                modifier = Modifier.width(240.dp))
            Button(onClick = { vm.playYouTube(youtubeUrl) }) { Text("YouTube IFrame") }
        }
        molaStatus?.let { Text(it) }
        Text("Mola ayrı uygulamadır; Android Auto video entegrasyonu sağlamaz.")
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PassengerLayout.entries.forEach { option ->
                OutlinedButton(onClick = { scope.launch { prefs.setLayout(option) } }) {
                    Text(when (option) {
                        PassengerLayout.FULL -> "Tam ekran"
                        PassengerLayout.SPLIT -> "Bölünmüş"
                        PassengerLayout.FLOATING -> "Yüzen"
                    } + if (layout == option) " ✓" else "")
                }
            }
            Text("Hız kilidi (varsayılan kapalı)")
            Switch(checked = speedLock,
                onCheckedChange = { enabled -> scope.launch { prefs.setSpeedLock(enabled) } })
            Text("Kulaklık ipucu: medya sesini kulaklığa yönlendirebilirsin.")
        }
        Text(state.message + " • Video yalnız telefonda • TTS ses kısma DOĞRULANMADI")
        Box(Modifier.fillMaxWidth().weight(1f)) {
            when (layout) {
                PassengerLayout.FULL -> VideoSurface(state.source, vm, webView,
                    Modifier.fillMaxSize())
                PassengerLayout.SPLIT -> Column(Modifier.fillMaxSize()) {
                    VideoSurface(state.source, vm, webView,
                        Modifier.fillMaxWidth().weight(0.57f))
                    OnlineMap(modifier = Modifier.fillMaxWidth().weight(0.43f),
                        route = route, destination = destination, gps = gps,
                        onSelectDestination = onSelectDestination)
                }
                PassengerLayout.FLOATING -> {
                    OnlineMap(modifier = Modifier.fillMaxSize(),
                        route = route, destination = destination, gps = gps,
                        onSelectDestination = onSelectDestination)
                    Box(Modifier.offset { dragOffset }
                        .fillMaxWidth(0.55f).fillMaxHeight(0.66f)
                        .pointerInput(Unit) {
                            detectDragGestures { change, delta ->
                                change.consume()
                                dragOffset = IntOffset(
                                    dragOffset.x + delta.x.roundToInt(),
                                    dragOffset.y + delta.y.roundToInt())
                            }
                        }.background(Color.Black)) {
                        VideoSurface(state.source, vm, webView, Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoSurface(
    source: PassengerSource?, vm: PassengerPlayerModel,
    webView: WebView, modifier: Modifier,
) {
    when (source) {
        is PassengerSource.YouTubeEmbed -> AndroidView(
            factory = {
                (webView.parent as? ViewGroup)?.removeView(webView)
                webView
            }, modifier = modifier)
        is PassengerSource.DirectHttps, is PassengerSource.LocalDocument -> AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    player = vm.player
                    useController = true
                }
            }, update = { view -> view.player = vm.player }, modifier = modifier)
        null -> Box(modifier.background(Color.Black),
            contentAlignment = Alignment.Center) {
            Text("Dosya veya DRM'siz HTTPS video URL seç.", color = Color.White)
        }
    }
}
