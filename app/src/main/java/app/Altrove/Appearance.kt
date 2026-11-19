package app.altrove

import android.annotation.SuppressLint
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList

/** Light, dark, or whatever the phone uses. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * The language of the app, chosen in its settings: an empty tag follows the phone. Android 13+
 * keeps it per app ([LocaleManager]); older versions have no per-app language, so the choice is
 * only offered from Android 13.
 */
object AppLanguage {
    val supported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /** "it", "en", or "" for the phone's language. */
    fun current(context: Context): String {
        if (!supported) return ""
        val locales = context.getSystemService(LocaleManager::class.java)?.applicationLocales ?: return ""
        return locales.toLanguageTags().substringBefore('-').substringBefore(',')
    }

    /** Applies [tag] ("" for the phone's language); Android then recreates the screen in it. */
    fun set(context: Context, tag: String) {
        if (!supported) return
        val manager = context.getSystemService(LocaleManager::class.java) ?: return
        manager.applicationLocales = if (tag.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
        // The notification channel is named once per launch: rename it now, in the new language.
        SessionNotification.ensureChannel(context)
    }
}

/**
 * A context that speaks the language chosen for Altrove.
 *
 * Android applies the per-app language to activities; the service, the widget and the
 * notification channel may still get the phone's language (say, an English phone with Altrove
 * in Italian). Everything shown outside the app goes through this.
 */
@SuppressLint("AppBundleLocaleChanges") // Distributed as a plain APK: every language is always installed.
fun Context.localized(): Context {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return this
    val locales = runCatching { getSystemService(LocaleManager::class.java)?.applicationLocales }.getOrNull()
    if (locales == null || locales.isEmpty || resources.configuration.locales == locales) return this
    val config = Configuration(resources.configuration).apply { setLocales(locales) }
    return createConfigurationContext(config)
}
