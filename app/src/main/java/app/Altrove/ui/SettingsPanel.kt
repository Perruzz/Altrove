package app.altrove.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.altrove.AppLanguage
import app.altrove.R
import app.altrove.ThemeMode

/**
 * The settings: theme and language, a way to support the project, and the version. A glass
 * panel that drops down from the gear in the corner, over a dimmed sky.
 */
@Composable
internal fun SettingsPanel(
    open: Boolean,
    onClose: () -> Unit,
    themeMode: ThemeMode,
    onThemeChange: (mode: ThemeMode, origin: Offset, reveal: Boolean) -> Unit,
    onSupport: () -> Unit,
) {
    BackHandler(enabled = open, onBack = onClose)
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = open, enter = fadeIn(tween(220)), exit = fadeOut(tween(200))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(LocalSpace.current.scrim)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
            )
        }
        AnimatedVisibility(
            visible = open,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .align(Alignment.TopCenter),
            enter = slideInVertically(spring(dampingRatio = 0.8f, stiffness = 420f)) { -it / 3 } +
                fadeIn(tween(200)) + scaleIn(initialScale = 0.96f),
            exit = slideOutVertically(tween(200)) { -it / 4 } + fadeOut(tween(160)) + scaleOut(targetScale = 0.97f),
        ) {
            GlassCard(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                opaque = true,
            ) {
                PanelContent(onClose, themeMode, onThemeChange, onSupport)
            }
        }
    }
}

@Composable
private fun PanelContent(
    onClose: () -> Unit,
    themeMode: ThemeMode,
    onThemeChange: (mode: ThemeMode, origin: Offset, reveal: Boolean) -> Unit,
    onSupport: () -> Unit,
) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.titleLarge,
            color = Space.Text,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onClose) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.settings_close),
                tint = Space.Muted,
            )
        }
    }

    // Theme: the circle of the new theme grows from the tapped choice.
    val dark = LocalSpace.current.dark
    val systemDark = isSystemInDarkTheme()
    val modes = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)
    SettingLabel(stringResource(R.string.settings_theme))
    ChoiceRow(
        options = listOf(
            stringResource(R.string.settings_system),
            stringResource(R.string.theme_light),
            stringResource(R.string.theme_dark),
        ),
        selected = modes.indexOf(themeMode),
        onSelect = { index, origin ->
            val mode = modes[index]
            if (mode != themeMode) {
                val willBeDark = when (mode) {
                    ThemeMode.SYSTEM -> systemDark
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                }
                // A change that keeps the colours (say "system" on a dark phone) needs no wipe.
                onThemeChange(mode, origin, willBeDark != dark)
            }
        },
    )

    // Language: per app only from Android 13; earlier, Altrove follows the phone.
    if (AppLanguage.supported) {
        val tags = listOf("", "it", "en")
        var language by remember { mutableStateOf(AppLanguage.current(context)) }
        SettingLabel(stringResource(R.string.settings_language))
        ChoiceRow(
            options = listOf(stringResource(R.string.settings_system), "Italiano", "English"),
            selected = tags.indexOf(language).coerceAtLeast(0),
            onSelect = { index, _ ->
                if (tags[index] != language) {
                    language = tags[index]
                    AppLanguage.set(context, tags[index])
                }
            },
        )
    }

    HorizontalDivider(color = Space.Edge.copy(alpha = 0.12f), modifier = Modifier.padding(vertical = 4.dp))

    Text(
        text = stringResource(R.string.settings_support),
        style = MaterialTheme.typography.bodyMedium,
        color = Space.Muted,
    )
    GhostButton(
        onClick = onSupport,
        modifier = Modifier.height(46.dp),
        color = LocalSpace.current.link,
    ) {
        Icon(painterResource(R.drawable.ic_coffee), contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.support_coffee))
    }

    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
    }
    Text(
        text = stringResource(R.string.settings_version, version ?: ""),
        style = MaterialTheme.typography.bodySmall,
        color = Space.Faint,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SettingLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = Space.Muted,
        modifier = Modifier.padding(top = 4.dp),
    )
}

/** A [ModeSwitch] that shares the full width between its choices. */
@Composable
private fun ChoiceRow(options: List<String>, selected: Int, onSelect: (Int, Offset) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        ModeSwitch(
            options = options,
            selected = selected,
            onSelect = onSelect,
            segment = maxWidth / options.size,
        )
    }
}
