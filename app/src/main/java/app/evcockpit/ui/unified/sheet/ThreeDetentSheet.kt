package app.evcockpit.ui.unified.sheet

import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import app.evcockpit.R
import app.evcockpit.ui.finalhmi.CockpitDesignTokens
import app.evcockpit.ui.unified.SheetDetent
import kotlin.math.roundToInt

/**
 * Mevcut Compose Foundation AnchoredDraggable (yeni bağımlılık yok).
 * Harita ve üst arama çubuğu bu panelin ARKASINDA, dokunma alanları ayrı.
 * İçerik kaydırma ile panel tutacağı sürükleme hareketi birbirine karışmaz.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ThreeDetentSheet(
    detent: SheetDetent,
    onDetent: (SheetDetent) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val t = CockpitDesignTokens
    val density = LocalDensity.current
    BoxWithConstraints(modifier.fillMaxSize()) {
        val availablePx = constraints.maxHeight.toFloat()
        val peekPx = with(density) { t.sheetPeekHeight.toPx() }
        val spacingPx = with(density) { t.sheetAnchorMinGap.toPx() }
        val fullTopPx = with(density) { t.sheetDefaultFullTopInset.toPx() }
        val safePeek = peekPx.coerceAtMost((availablePx - spacingPx * 2).coerceAtLeast(0f))
        val halfHeight = (availablePx * t.sheetHalfFraction)
            .coerceAtLeast(safePeek + spacingPx)
            .coerceAtMost((availablePx - spacingPx).coerceAtLeast(0f))
        val anchors = remember(availablePx, safePeek, halfHeight, fullTopPx) {
            DraggableAnchors {
                SheetDetent.FULL at fullTopPx
                SheetDetent.HALF at (availablePx - halfHeight)
                SheetDetent.PEEK at (availablePx - safePeek)
            }
        }
        val drag = remember {
            AnchoredDraggableState(
                initialValue = detent,
                positionalThreshold = { it * t.sheetPositionalThresholdFraction },
                velocityThreshold = { with(density) { t.sheetVelocityThreshold.toPx() } },
                snapAnimationSpec = tween(),
                decayAnimationSpec = exponentialDecay(),
            )
        }
        SideEffect { drag.updateAnchors(anchors) }
        LaunchedEffect(detent, availablePx) {
            if (drag.currentValue != detent) drag.animateTo(detent)
        }
        LaunchedEffect(drag.currentValue) {
            if (drag.currentValue != detent) onDetent(drag.currentValue)
        }

        val offset = drag.offset.takeIf { it.isFinite() } ?: (availablePx - safePeek)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .offset { IntOffset(0, offset.roundToInt()) },
            color = t.panel,
            shape = t.sheetCorner,
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = t.sheetContentPadding),
                verticalArrangement = Arrangement.spacedBy(t.panelCardSpacing),
            ) {
                Box(
                    Modifier.fillMaxWidth()
                        .heightIn(min = t.sheetHandleTouchHeight)
                        .anchoredDraggable(drag, Orientation.Vertical)
                        .semantics {
                            contentDescription =
                                "Panel yüksekliğini değiştir, yukarı veya aşağı sürükle"
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.width(t.sheetHandleWidth)
                            .height(t.sheetHandleHeight)
                            .background(t.sheetHandleColor, t.cornerTiny),
                    )
                }
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(t.panelCardSpacing),
                    content = content,
                )
            }
        }
    }
}
