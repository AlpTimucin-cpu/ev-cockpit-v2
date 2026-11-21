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
/** Compatibility aliases for existing screens; new UI uses TeslaTokens directly. */
object CockpitColors {
    val background = app.evcockpit.ui.tesla.TeslaTokens.background
    val panel = app.evcockpit.ui.tesla.TeslaTokens.surface
    val panelRaised = app.evcockpit.ui.tesla.TeslaTokens.surfaceRaised
    val accent = app.evcockpit.ui.tesla.TeslaTokens.accent
    val muted = app.evcockpit.ui.tesla.TeslaTokens.secondaryText
    val outline = app.evcockpit.ui.tesla.TeslaTokens.outline
    val green = app.evcockpit.ui.tesla.TeslaTokens.accent
    val amber = app.evcockpit.ui.tesla.TeslaTokens.warning
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
fun CockpitTheme(@Suppress("UNUSED_PARAMETER") dark: Boolean, content: @Composable () -> Unit) {
    // A single, static dark palette. No system dynamic color or night/day switch.
    val tokens = app.evcockpit.ui.tesla.TeslaTokens
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = tokens.accent,
            onPrimary = tokens.background,
            background = tokens.background,
            onBackground = tokens.text,
            surface = tokens.surface,
            onSurface = tokens.text,
            surfaceVariant = tokens.surfaceRaised,
            onSurfaceVariant = tokens.secondaryText,
            outline = tokens.outline,
            error = tokens.error,
            onError = tokens.background,
            secondary = tokens.accent,
            onSecondary = tokens.background,
            tertiary = tokens.warning,
            onTertiary = tokens.background,
        ),
        typography = app.evcockpit.ui.tesla.TeslaTypography,
        content = content,
    )
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
