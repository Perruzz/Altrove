package app.altrove

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/** The little that Altrove remembers between launches. */
object Prefs {
    private const val FILE = "altrove"
    private const val KEY_LATITUDE = "latitude"
    private const val KEY_LONGITUDE = "longitude"
    private const val KEY_SESSION_RUNNING = "session_running"
    private const val KEY_OPTIONAL_PERMISSIONS_HANDLED = "optional_permissions_handled"
    private const val KEY_AREA_MODE = "area_mode"
    private const val KEY_AREA_CORNERS = "area_corners"
    private const val KEY_AREA_SPACING = "area_spacing"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun latitudeText(context: Context): String = prefs(context).getString(KEY_LATITUDE, null).orEmpty()

    fun longitudeText(context: Context): String = prefs(context).getString(KEY_LONGITUDE, null).orEmpty()

    /** The saved destination, or null until both fields have been saved once. */
    fun destination(context: Context): Coordinates? {
        val latitude = Coordinates.parseLatitude(latitudeText(context)) ?: return null
        val longitude = Coordinates.parseLongitude(longitudeText(context)) ?: return null
        return Coordinates(latitude, longitude)
    }

    fun saveDestination(context: Context, latitude: String, longitude: String) {
        prefs(context).edit {
            putString(KEY_LATITUDE, latitude)
            putString(KEY_LONGITUDE, longitude)
        }
    }

    /**
     * True while a session is running. If the process dies mid-session the flag survives, which is
     * how the next launch knows it has mock providers to clean up (see [AltroveApp]).
     */
    fun sessionRunning(context: Context): Boolean = prefs(context).getBoolean(KEY_SESSION_RUNNING, false)

    fun setSessionRunning(context: Context, running: Boolean) {
        // Synchronous on purpose: the flag must be on disk before the session really starts.
        prefs(context).edit(commit = true) { putBoolean(KEY_SESSION_RUNNING, running) }
    }

    /** True once the user has answered (or dismissed) the optional permissions: never nag twice. */
    fun optionalPermissionsHandled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_OPTIONAL_PERMISSIONS_HANDLED, false)

    fun setOptionalPermissionsHandled(context: Context) {
        prefs(context).edit { putBoolean(KEY_OPTIONAL_PERMISSIONS_HANDLED, true) }
    }

    /** True when the destination is drawn at random from an area rather than typed. */
    fun areaMode(context: Context): Boolean = prefs(context).getBoolean(KEY_AREA_MODE, false)

    fun setAreaMode(context: Context, area: Boolean) {
        prefs(context).edit { putBoolean(KEY_AREA_MODE, area) }
    }

    /** The corners of the area as typed, one per row: four empty rows the first time. */
    fun areaCorners(context: Context): List<String> {
        val rows = prefs(context).getString(KEY_AREA_CORNERS, null)?.split('\n') ?: return List(4) { "" }
        return rows + List((3 - rows.size).coerceAtLeast(0)) { "" }
    }

    /** The spacing of the grid in metres, as typed: one metre by default. */
    fun areaSpacing(context: Context): String = prefs(context).getString(KEY_AREA_SPACING, null) ?: "1"

    fun saveArea(context: Context, corners: List<String>, spacing: String) {
        prefs(context).edit {
            putString(KEY_AREA_CORNERS, corners.joinToString("\n"))
            putString(KEY_AREA_SPACING, spacing)
        }
    }
}
