package app.altrove

import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.drawable.toDrawable
import app.altrove.ui.AltroveTheme
import app.altrove.ui.MainScreen
import app.altrove.ui.NightSpace
import app.altrove.ui.ThemeRoot
import app.altrove.ui.WhiteSpace

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Paint the window in the theme's colour straight away, so nothing flashes before the
        // first frame when the app's theme differs from the phone's.
        val systemDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        applyWindowTheme(isDark(Prefs.theme(this), systemDark))
        syncSplashTheme(Prefs.theme(this))

        setContent {
            var mode by rememberSaveable { mutableStateOf(Prefs.theme(this)) }
            val dark = isDark(mode, isSystemInDarkTheme())
            DisposableEffect(dark) {
                applyWindowTheme(dark)
                onDispose { }
            }
            ThemeRoot(
                onApply = { chosen ->
                    mode = chosen
                    Prefs.setTheme(this, chosen)
                    syncSplashTheme(chosen)
                },
            ) { switcher ->
                AltroveTheme(dark = dark) {
                    MainScreen(themeMode = mode, onThemeChange = switcher::switch)
                }
            }
        }
    }

    private fun isDark(mode: ThemeMode, systemDark: Boolean): Boolean = when (mode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    /**
     * Android 13+: the launch splash is drawn by the system before the app runs, in the phone's
     * theme. Telling it the app's theme makes the next launch open in the right sky.
     */
    private fun syncSplashTheme(mode: ThemeMode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val theme = when (mode) {
            ThemeMode.SYSTEM -> Resources.ID_NULL // Back to the manifest theme, which follows the phone.
            ThemeMode.LIGHT -> R.style.Theme_Altrove_Day
            ThemeMode.DARK -> R.style.Theme_Altrove_Night
        }
        runCatching { splashScreen.setSplashScreenTheme(theme) }
    }

    /** System bar icons and window background to match the theme. */
    private fun applyWindowTheme(dark: Boolean) {
        val bars = if (dark) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        window.setBackgroundDrawable((if (dark) NightSpace else WhiteSpace).background.toArgb().toDrawable())
    }
}
