package app.evcockpit.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Tesla-inspired visual language; no proprietary assets, branding or driving semantics. */
object CockpitColors {
    val background = Color(0xFF090C11)
    val panel = Color(0xFF151B24)
    val panelRaised = Color(0xFF202938)
    val accent = Color(0xFF94AEFF)
    val muted = Color(0xFFB0B9C9)
    val outline = Color(0xFF344154)
    val green = Color(0xFF45D7A4)
    val amber = Color(0xFFFFC36F)
}

object CockpitLayoutPolicy {
    /** The handset is fixed landscape but Android display size and font scaling vary. */
    fun useTwoColumns(widthDp: Int): Boolean = widthDp >= 600
    fun compactDashboard(heightDp: Int): Boolean = heightDp < 340
    fun speedLabel(kmh: Double?): String =
        if (kmh == null || !kmh.isFinite() || kmh < 0.0) "--"
        else String.format(java.util.Locale.ROOT, "%.0f", kmh)

    val tabLabels: List<String> = listOf(
        "Gösterge", "OBD Test", "Rota", "Plan", "Yolcu video", "Ayarlar"
    )
}

@Composable
fun CockpitTheme(dark: Boolean, content: @Composable () -> Unit) {
    val colors = if (dark) darkColorScheme(
        primary = CockpitColors.accent,
        onPrimary = Color(0xFF08111F),
        background = CockpitColors.background,
        onBackground = Color(0xFFF5F7FA),
        surface = CockpitColors.panel,
        onSurface = Color(0xFFF5F7FA),
        surfaceVariant = CockpitColors.panelRaised,
        onSurfaceVariant = CockpitColors.muted,
        outline = CockpitColors.outline,
        error = CockpitColors.amber,
    ) else lightColorScheme(
        primary = Color(0xFF3457A5),
        onPrimary = Color.White,
        background = Color(0xFFF3F6FB),
        onBackground = Color(0xFF172133),
        surface = Color.White,
        onSurface = Color(0xFF172133),
        surfaceVariant = Color(0xFFE6ECF5),
        onSurfaceVariant = Color(0xFF526071),
        outline = Color(0xFFBEC8D7),
        error = Color(0xFFA34220),
    )
    MaterialTheme(colorScheme = colors, content = content)
}

@Composable
fun CockpitTabs(selected: Int, onSelect: (Int) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Column(Modifier.widthIn(min = 106.dp).padding(end = 6.dp)) {
                Text(
                    "ID.4",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "YOL ARKADAŞI",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    letterSpacing = 0.7.sp,
                )
            }
            Row(
                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CockpitLayoutPolicy.tabLabels.forEachIndexed { index, title ->
                    val active = selected == index
                    Surface(
                        onClick = { onSelect(index) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (active) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        border = if (active) BorderStroke(
                            1.dp, MaterialTheme.colorScheme.primary
                        ) else null,
                        modifier = Modifier.heightIn(min = 48.dp)
                            .semantics { contentDescription = "$title sekmesi" },
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 13.sp,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CockpitMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}
