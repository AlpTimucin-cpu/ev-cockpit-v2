package app.evcockpit.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.evcockpit.obd.VehicleState
import kotlin.math.abs

private val Bg = Color(0xFF0B0D10)
private val Panel = Color(0xFF151A20)
private val Dim = Color(0xFF8A949E)
private val Track = Color(0xFF22272E)

/** Tesla tarzı: solda hız / güç / batarya, sağda harita alanı (Aşama 2). */
@Composable
fun DashboardScreen(s: VehicleState, usableKwh: Double, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxSize().background(Bg).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            Modifier.weight(0.38f).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    s.speedKmh?.let { "%.0f".format(it) } ?: "--",
                    color = Color.White, fontSize = 96.sp, fontWeight = FontWeight.Light,
                )
                Text("km/h", color = Dim, fontSize = 18.sp)
                if (s.demo) Text("DEMO", color = Color(0xFFFFB300), fontSize = 14.sp)
            }
            PowerBar(s.powerKw)
            BatteryBlock(s.socPct, usableKwh)
        }
        OnlineMap(
            modifier = Modifier.weight(0.62f).fillMaxHeight().clip(RoundedCornerShape(20.dp)),
        )
    }
}

@Composable
private fun PowerBar(kw: Double?, maxKw: Double = 125.0) {
    Column {
        Text(kw?.let { "%.0f kW".format(it) } ?: "-- kW", color = Color.White, fontSize = 22.sp)
        Spacer(Modifier.height(6.dp))
        Canvas(Modifier.fillMaxWidth().height(14.dp)) {
            val mid = size.width / 2
            drawRoundRect(Track, size = size, cornerRadius = CornerRadius(7.dp.toPx()))
            if (kw != null) {
                val frac = (abs(kw) / maxKw).coerceIn(0.0, 1.0).toFloat()
                val w = mid * frac
                if (kw >= 0) drawRect(Color.White, Offset(mid, 0f), Size(w, size.height))
                else drawRect(Color(0xFF3DDC84), Offset(mid - w, 0f), Size(w, size.height))
            }
            drawRect(Dim, Offset(mid - 1f, 0f), Size(2f, size.height))
        }
    }
}

@Composable
private fun BatteryBlock(soc: Double?, usableKwh: Double) {
    val pct = soc?.coerceIn(0.0, 100.0)
    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                pct?.let { "%.0f".format(it) } ?: "--",
                color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Light,
            )
            Text(" %", color = Dim, fontSize = 22.sp, modifier = Modifier.padding(bottom = 10.dp))
        }
        Spacer(Modifier.height(6.dp))
        Canvas(Modifier.fillMaxWidth().height(10.dp)) {
            drawRoundRect(Track, size = size, cornerRadius = CornerRadius(5.dp.toPx()))
            if (pct != null) {
                val color = when {
                    pct > 20 -> Color(0xFF3DDC84)
                    pct > 10 -> Color(0xFFFFB300)
                    else -> Color(0xFFFF5252)
                }
                drawRoundRect(
                    color, size = Size(size.width * (pct / 100.0).toFloat(), size.height),
                    cornerRadius = CornerRadius(5.dp.toPx()),
                )
            }
        }
        if (pct != null) {
            Spacer(Modifier.height(4.dp))
            Text("≈ %.1f kWh".format(pct / 100.0 * usableKwh), color = Dim, fontSize = 14.sp)
        }
    }
}
