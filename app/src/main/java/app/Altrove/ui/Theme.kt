package app.altrove.ui

import android.graphics.Typeface
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/**
 * Every colour of the app in one mood: the night sky, or "white space", its light twin.
 *
 * The screen reads them through [Space] while composing; the hand-drawn scene (the sky, the
 * globe) gets the whole palette passed in.
 */
@Immutable
internal data class SpacePalette(
    val dark: Boolean,

    // The sky
    val skyTop: Color,
    val skyMiddle: Color,
    val skyBottom: Color,
    /** Three nebulae at rest, and the same three while "elsewhere". */
    val nebulaCool: List<Color>,
    val nebulaWarm: List<Color>,
    val nebulaStrength: Float,
    /** White, violet and warm stars. */
    val stars: List<Color>,
    val starStrength: Float,
    val meteor: Color,
    val flash: Color,
    val flashStrength: Float,

    // The globe
    /** From the lit side to the rim. */
    val globeBody: List<Color>,
    val globeGlow: Color,
    val globeGlowActive: Color,
    val land: Color,
    val landActive: Color,
    val landSpark: Color,
    val graticuleStrength: Float,
    val beacon: Color,
    val beaconCore: Color,
    val orbit: Color,
    val satellite: Color,
    val satelliteCore: Color,
    /** Shock waves and the rings thrown off at the start. */
    val wave: Color,

    // The screen
    val background: Color,
    val text: Color,
    val textBody: Color,
    val muted: Color,
    val faint: Color,
    /** Glass panels: a tint at the top fading into [panelBottom]. */
    val panel: Color,
    val panelBottom: Color,
    val panelOpacity: Float,
    /** The lit edge of a panel. */
    val panelEdge: Color,
    /** Hairlines and faint fills: white on the night sky, ink on the white one. */
    val edge: Color,
    val scrim: Color,
    /** Text on bright buttons (cyan, amber), in both moods. */
    val ink: Color,

    // Accents
    val violet: Color,
    val violetDeep: Color,
    val lavender: Color,
    val cyan: Color,
    val magenta: Color,
    val gold: Color,
    val amber: Color,
    val danger: Color,
    /** Panel tints: during a session, and for the setup card. */
    val activeTint: Color,
    val setupTint: Color,
    val setupTitle: Color,
    /** The coffee button. */
    val link: Color,
)

/** Deep space, with the violet of the icon at rest and a warmer magenta while "elsewhere". */
internal val NightSpace = SpacePalette(
    dark = true,
    skyTop = Color(0xFF0D0D2E),
    skyMiddle = Color(0xFF070718),
    skyBottom = Color(0xFF04040F),
    nebulaCool = listOf(Color(0xFF5B3DF5), Color(0xFF0FA5C4), Color(0xFFB8328F)),
    nebulaWarm = listOf(Color(0xFFC2318B), Color(0xFFFF8A4C), Color(0xFF6C4DFF)),
    nebulaStrength = 1f,
    stars = listOf(Color(0xFFF3F0FF), Color(0xFFC6BDFF), Color(0xFFFFE1B0)),
    starStrength = 1f,
    meteor = Color(0xFFF3F0FF),
    flash = Color.White,
    flashStrength = 0.32f,
    globeBody = listOf(Color(0xFF26266E), Color(0xFF10103A), Color(0xFF070720)),
    globeGlow = Color(0xFF8C7CFF),
    globeGlowActive = Color(0xFFFF6FB5),
    land = Color(0xFFC6BDFF),
    landActive = Color(0xFFFFE4F2),
    landSpark = Color.White,
    graticuleStrength = 0.13f,
    beacon = Color(0xFFFFDDA1),
    beaconCore = Color.White,
    orbit = Color(0xFFC6BDFF),
    satellite = Color(0xFF72E6F7),
    satelliteCore = Color.White,
    wave = Color.White,
    background = Color(0xFF070718),
    text = Color.White,
    textBody = Color(0xFFF3F0FF),
    muted = Color(0xFFF3F0FF).copy(alpha = 0.66f),
    faint = Color(0xFFF3F0FF).copy(alpha = 0.5f),
    panel = Color(0xFF17174A),
    panelBottom = Color(0xFF0D0D2E),
    panelOpacity = 0.85f,
    panelEdge = Color.White,
    edge = Color.White,
    scrim = Color(0xFF02020A).copy(alpha = 0.55f),
    ink = Color(0xFF070718),
    violet = Color(0xFF8C7CFF),
    violetDeep = Color(0xFF5546E0),
    lavender = Color(0xFFC6BDFF),
    cyan = Color(0xFF72E6F7),
    magenta = Color(0xFFFF6FB5),
    gold = Color(0xFFFFD58A),
    amber = Color(0xFFFFC46B),
    danger = Color(0xFFFF9B9B),
    activeTint = Color(0xFF6A1D6E),
    setupTint = Color(0xFF4A3516),
    setupTitle = Color(0xFFFFD58A),
    link = Color(0xFFFFD58A),
)

/**
 * "White space": a pale lavender void, dark stars, a pearl Earth with violet continents. Same
 * shapes and accents as the night, inked for a light background.
 */
internal val WhiteSpace = SpacePalette(
    dark = false,
    skyTop = Color(0xFFFFFFFF),
    skyMiddle = Color(0xFFF6F4FF),
    skyBottom = Color(0xFFEAE6FF),
    nebulaCool = listOf(Color(0xFFA594FF), Color(0xFF7FDCEB), Color(0xFFF2A5D2)),
    nebulaWarm = listOf(Color(0xFFFFA3CF), Color(0xFFFFC59A), Color(0xFFB3A2FF)),
    nebulaStrength = 1.15f,
    stars = listOf(Color(0xFF4A3FB5), Color(0xFF8C7CFF), Color(0xFFD08A3A)),
    starStrength = 0.72f,
    meteor = Color(0xFF6C5CE7),
    flash = Color(0xFF8C7CFF),
    flashStrength = 0.2f,
    globeBody = listOf(Color(0xFFFFFFFF), Color(0xFFEEEAFF), Color(0xFFD4CCFF)),
    globeGlow = Color(0xFF8C7CFF),
    globeGlowActive = Color(0xFFFF6FB5),
    land = Color(0xFF5546E0),
    landActive = Color(0xFFC2318B),
    landSpark = Color(0xFFF5A524),
    graticuleStrength = 0.2f,
    beacon = Color(0xFFF59E0B),
    beaconCore = Color(0xFFD9480F),
    orbit = Color(0xFF8C7CFF),
    satellite = Color(0xFF0EA5C6),
    satelliteCore = Color(0xFF3A2FBF),
    wave = Color(0xFF5546E0),
    background = Color(0xFFF6F4FF),
    text = Color(0xFF1B1740),
    textBody = Color(0xFF2A2550),
    muted = Color(0xFF2A2550).copy(alpha = 0.66f),
    faint = Color(0xFF2A2550).copy(alpha = 0.5f),
    panel = Color(0xFFFFFFFF),
    panelBottom = Color(0xFFF8F6FF),
    panelOpacity = 0.86f,
    panelEdge = Color(0xFF8C7CFF),
    edge = Color(0xFF1B1740),
    scrim = Color(0xFF1B1740).copy(alpha = 0.28f),
    ink = Color(0xFF070718),
    violet = Color(0xFF6C5CE7),
    violetDeep = Color(0xFF4A3DD6),
    lavender = Color(0xFF4A3DD6),
    cyan = Color(0xFF0EA5C6),
    magenta = Color(0xFFE0479B),
    gold = Color(0xFFB7791F),
    amber = Color(0xFFF5A524),
    danger = Color(0xFFC62828),
    activeTint = Color(0xFFFFD9EC),
    setupTint = Color(0xFFFFE9C7),
    setupTitle = Color(0xFF9A5B00),
    link = Color(0xFF9A5B00),
)

internal val LocalSpace = staticCompositionLocalOf { NightSpace }

/** The colours of the current theme, by role, for the screen. */
internal object Space {
    val Night: Color @Composable @ReadOnlyComposable get() = LocalSpace.current.background
    val Panel: Color @Composable @ReadOnlyComposable get() = LocalSpace.current.panel
    val Text: Color @Composable @ReadOnlyComposable get() = LocalSpace.current.text
    val Starlight: Color @Composable @ReadOnlyComposable get() = LocalSpace.current.textBody
    val Muted: Color @Composable @ReadOnlyComposable get() = LocalSpace.current.muted
    val Faint: Color @Composable @ReadOnlyComposable get() = LocalSpace.current.faint
    val Edge: Color @Composable @ReadOnlyComposable get() = LocalSpace.current.edge
    val Ink: Color @Composable @ReadOnlyComposable get() = LocalSpace.current.ink

    val Violet: Color @Composable @ReadOnlyComposable get() = LocalSpace.current.violet
    val VioletDeep: Color @Composable @ReadOnlyComposable get() = LocalSpace.current.violetDeep
    val Lavender: Color @Composable @ReadOnlyComposable get() = LocalSpace.current.lavender
    val Cyan: Color @Composable @ReadOnlyComposable get() = LocalSpace.current.cyan
    val Magenta: Color @Composable @ReadOnlyComposable get() = LocalSpace.current.magenta
    val Gold: Color @Composable @ReadOnlyComposable get() = LocalSpace.current.gold
    val Amber: Color @Composable @ReadOnlyComposable get() = LocalSpace.current.amber
    val Danger: Color @Composable @ReadOnlyComposable get() = LocalSpace.current.danger
}

private fun colorSchemeOf(palette: SpacePalette) = with(palette) {
    val scheme = if (dark) darkColorScheme() else lightColorScheme()
    scheme.copy(
        primary = violet,
        onPrimary = Color.White,
        primaryContainer = panel,
        onPrimaryContainer = textBody,
        secondary = cyan,
        onSecondary = ink,
        tertiary = gold,
        onTertiary = ink,
        background = background,
        onBackground = textBody,
        surface = background,
        onSurface = textBody,
        onSurfaceVariant = muted,
        surfaceContainer = panelBottom,
        surfaceContainerHigh = panel,
        outline = edge.copy(alpha = 0.22f),
        error = danger,
        onError = if (dark) ink else Color.White,
    )
}

/** Google Sans where the device has it (Pixel); elsewhere this resolves to the system font. */
private val Brand = FontFamily(Typeface.create("google-sans", Typeface.NORMAL))

private val AltroveTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontFamily = Brand),
        headlineMedium = headlineMedium.copy(fontFamily = Brand),
        headlineSmall = headlineSmall.copy(fontFamily = Brand),
        titleLarge = titleLarge.copy(fontFamily = Brand),
        titleMedium = titleMedium.copy(fontFamily = Brand),
        labelLarge = labelLarge.copy(fontFamily = Brand),
    )
}

@Composable
fun AltroveTheme(dark: Boolean, content: @Composable () -> Unit) {
    val palette = if (dark) NightSpace else WhiteSpace
    CompositionLocalProvider(LocalSpace provides palette) {
        MaterialTheme(colorScheme = colorSchemeOf(palette), typography = AltroveTypography) {
            CompositionLocalProvider(LocalContentColor provides palette.textBody, content = content)
        }
    }
}
