package app.altrove.ui

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import app.altrove.ThemeMode
import kotlin.math.hypot
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Switches theme with a circular reveal that grows from the tapped button: the old screen is
 * frozen in a bitmap and wiped away to uncover the new one. The same transition as Stacco.
 */
@Stable
internal class ThemeSwitcher(
    private val scope: CoroutineScope,
    private val layer: GraphicsLayer,
    private val apply: (ThemeMode) -> Unit,
) {
    var snapshot by mutableStateOf<ImageBitmap?>(null)
        private set
    var center by mutableStateOf(Offset.Unspecified)
        private set
    val progress = Animatable(0f)

    /**
     * @param origin where the circle starts, in root coordinates.
     * @param reveal false when the colours won't change (say, "system" while the phone is
     *   already in that mode): then the new mode is simply stored.
     */
    fun switch(mode: ThemeMode, origin: Offset, reveal: Boolean) {
        if (snapshot != null) return
        if (!reveal || !ValueAnimator.areAnimatorsEnabled()) {
            apply(mode)
            return
        }
        scope.launch {
            val bitmap = runCatching { layer.toImageBitmap() }.getOrNull()
            if (bitmap == null) {
                apply(mode)
                return@launch
            }
            center = origin
            snapshot = bitmap
            progress.snapTo(0f)
            apply(mode)
            // Let the new theme compose before starting the wipe.
            withFrameNanos { }
            withFrameNanos { }
            progress.animateTo(1f, tween(760, easing = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)))
            snapshot = null
        }
    }
}

/** Records the whole screen, so that a theme switch can wipe the old one away. */
@Composable
internal fun ThemeRoot(onApply: (ThemeMode) -> Unit, content: @Composable (ThemeSwitcher) -> Unit) {
    val scope = rememberCoroutineScope()
    val layer = rememberGraphicsLayer()
    val apply by rememberUpdatedState(onApply)
    val switcher = remember { ThemeSwitcher(scope, layer) { apply(it) } }
    Box(
        Modifier
            .fillMaxSize()
            .drawWithContent {
                layer.record { this@drawWithContent.drawContent() }
                drawLayer(layer)
                val snap = switcher.snapshot
                if (snap != null) {
                    val c = if (switcher.center.isSpecified) switcher.center else Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = hypot(max(c.x, size.width - c.x), max(c.y, size.height - c.y))
                    val radius = maxRadius * switcher.progress.value
                    val hole = Path().apply { addOval(Rect(c, radius)) }
                    clipPath(hole, ClipOp.Difference) { drawImage(snap) }
                }
            },
    ) {
        content(switcher)
    }
}
