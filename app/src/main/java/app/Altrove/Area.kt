package app.altrove

import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.roundToLong
import kotlin.random.Random

/**
 * An area on the map, a polygon of three or more corners, with a square grid of points laid
 * over it every [spacingMeters]. [random] picks one of the grid points inside the area, each
 * with exactly the same chance, independently of the previous picks.
 *
 * The grid starts at the first corner and runs along the first side, so a rectangle entered
 * corner by corner gets rows parallel to its sides: a 20 m square with a 1 m spacing holds
 * 21 × 21 = 441 points, borders included. A point counts as inside when it lies within the
 * polygon or less than a tenth of the spacing (at most 50 cm) away from its border, so that
 * corners typed with five or six decimals still catch the points on the far sides.
 *
 * Distances are measured on a flat map centred on the area (an equirectangular projection):
 * exact to well under a millimetre over a few kilometres, still reasonable for regions
 * hundreds of kilometres wide. The polygon may cross the 180° meridian.
 */
class AreaGrid private constructor(
    val corners: List<Coordinates>,
    val spacingMeters: Double,
    private val origin: Coordinates,
    private val metersPerDegreeLongitude: Double,
    private val ux: Double,
    private val uy: Double,
    private val xs: DoubleArray,
    private val ys: DoubleArray,
) {
    private val tolerance = minOf(spacingMeters * 0.1, 0.5)
    private val iMin = ceil((xs.min() - tolerance) / spacingMeters).toLong()
    private val iMax = floor((xs.max() + tolerance) / spacingMeters).toLong()
    private val jMin = ceil((ys.min() - tolerance) / spacingMeters).toLong()
    private val jMax = floor((ys.max() + tolerance) / spacingMeters).toLong()

    /** Surface of the area, in square metres. */
    val surfaceSquareMeters: Double = surface(xs, ys)

    /** Grid points inside the area: counted one by one when [countIsExact], else estimated. */
    val count: Long

    /** False for areas too large to count point by point: [count] is then surface / spacing². */
    val countIsExact: Boolean

    /** The average of the corners: a good place to look at the area from. */
    val center: Coordinates = toCoordinates(xs.average(), ys.average())

    init {
        val candidates = if (iMax < iMin || jMax < jMin) 0L else (iMax - iMin + 1) * (jMax - jMin + 1)
        if (candidates <= EXACT_COUNT_LIMIT) {
            var inside = 0L
            for (j in jMin..jMax) {
                val y = j * spacingMeters
                for (i in iMin..iMax) if (contains(i * spacingMeters, y)) inside++
            }
            count = inside
            countIsExact = true
        } else {
            count = (surfaceSquareMeters / (spacingMeters * spacingMeters)).roundToLong()
            countIsExact = false
        }
    }

    /** One grid point inside the area, chosen at random; null only if none fits. */
    fun random(random: Random = Random.Default): Coordinates? {
        if (count == 0L) return null
        // Rejection sampling: draw from the grid over the bounding box until a point falls
        // inside. Every point inside is accepted with the same probability, so the result is
        // uniform; with the grid aligned to the first side, a few draws are usually enough.
        repeat(MAX_ATTEMPTS) {
            val x = random.nextLong(iMin, iMax + 1) * spacingMeters
            val y = random.nextLong(jMin, jMax + 1) * spacingMeters
            if (contains(x, y)) return toCoordinates(x, y)
        }
        // A sliver across the grid: walk to a point chosen by its index instead.
        if (!countIsExact) return null
        var target = random.nextLong(count)
        for (j in jMin..jMax) {
            val y = j * spacingMeters
            for (i in iMin..iMax) {
                if (contains(i * spacingMeters, y) && target-- == 0L) return toCoordinates(i * spacingMeters, y)
            }
        }
        return null
    }

    private fun contains(x: Double, y: Double): Boolean = insidePolygon(x, y) || nearBorder(x, y)

    /** Even-odd rule: works for any polygon, convex or not, even one that crosses itself. */
    private fun insidePolygon(x: Double, y: Double): Boolean {
        var inside = false
        var previous = xs.size - 1
        for (current in xs.indices) {
            val y1 = ys[current]
            val y2 = ys[previous]
            if ((y1 > y) != (y2 > y)) {
                val crossing = xs[current] + (y - y1) * (xs[previous] - xs[current]) / (y2 - y1)
                if (x < crossing) inside = !inside
            }
            previous = current
        }
        return inside
    }

    private fun nearBorder(x: Double, y: Double): Boolean {
        val limit = tolerance * tolerance
        var previous = xs.size - 1
        for (current in xs.indices) {
            if (distanceSquared(x, y, xs[previous], ys[previous], xs[current], ys[current]) <= limit) return true
            previous = current
        }
        return false
    }

    /** From grid space (metres along the first side, and across it) back to the map. */
    private fun toCoordinates(x: Double, y: Double): Coordinates {
        val east = x * ux - y * uy
        val north = x * uy + y * ux
        val latitude = (origin.latitude + north / METERS_PER_DEGREE).coerceIn(-90.0, 90.0)
        val longitude = wrap(origin.longitude + east / metersPerDegreeLongitude)
        return Coordinates(latitude, longitude)
    }

    companion object {
        const val MIN_SPACING = 0.1
        const val MAX_SPACING = 100_000.0

        /** Metres in one degree of latitude, on a sphere with the Earth's mean radius. */
        private const val METERS_PER_DEGREE = 6_371_008.8 * PI / 180.0
        private const val EXACT_COUNT_LIMIT = 2_000_000L
        private const val MAX_ATTEMPTS = 100_000

        /**
         * The grid over [corners] (in order around the area), or null when they don't make an
         * area: fewer than three, all in one place or on one line, or a spacing out of range.
         */
        fun of(corners: List<Coordinates>, spacingMeters: Double): AreaGrid? {
            if (corners.size < 3 || spacingMeters.isNaN() || spacingMeters !in MIN_SPACING..MAX_SPACING) return null
            val origin = corners.first()
            val meanLatitude = corners.sumOf { it.latitude } / corners.size
            val metersPerDegreeLongitude = METERS_PER_DEGREE * cos(Math.toRadians(meanLatitude)).coerceAtLeast(1e-6)
            val east = DoubleArray(corners.size) { wrap(corners[it].longitude - origin.longitude) * metersPerDegreeLongitude }
            val north = DoubleArray(corners.size) { (corners[it].latitude - origin.latitude) * METERS_PER_DEGREE }

            // The grid runs along the first side that has a length.
            val next = (1 until corners.size).firstOrNull { hypot(east[it], north[it]) > 1e-6 } ?: return null
            val length = hypot(east[next], north[next])
            val ux = east[next] / length
            val uy = north[next] / length
            val xs = DoubleArray(corners.size) { east[it] * ux + north[it] * uy }
            val ys = DoubleArray(corners.size) { -east[it] * uy + north[it] * ux }
            if (surface(xs, ys) < 1e-4) return null

            return AreaGrid(corners, spacingMeters, origin, metersPerDegreeLongitude, ux, uy, xs, ys)
        }

        /** Shoelace formula: the surface of a simple polygon, whichever way it turns. */
        private fun surface(xs: DoubleArray, ys: DoubleArray): Double {
            var twice = 0.0
            var previous = xs.size - 1
            for (current in xs.indices) {
                twice += xs[previous] * ys[current] - xs[current] * ys[previous]
                previous = current
            }
            return abs(twice) / 2.0
        }

        private fun distanceSquared(px: Double, py: Double, ax: Double, ay: Double, bx: Double, by: Double): Double {
            val dx = bx - ax
            val dy = by - ay
            val lengthSquared = dx * dx + dy * dy
            val t = if (lengthSquared == 0.0) 0.0 else (((px - ax) * dx + (py - ay) * dy) / lengthSquared).coerceIn(0.0, 1.0)
            val cx = ax + t * dx - px
            val cy = ay + t * dy - py
            return cx * cx + cy * cy
        }

        /** Brings a longitude, or a difference of longitudes, back into -180..180. */
        private fun wrap(degrees: Double): Double {
            var d = (degrees + 180.0) % 360.0
            if (d < 0) d += 360.0
            return d - 180.0
        }
    }
}

/** The area as typed in the app: one corner per row, and the spacing of the grid. */
object AreaInput {
    // "latitude, longitude" with decimal points, separated by a comma, a semicolon or spaces:
    // the way Google Maps copies a place.
    private val PAIR = Regex("""([+-]?\d+(?:\.\d+)?)\s*[,;\s]\s*([+-]?\d+(?:\.\d+)?)""")
    private val NUMBER = Regex("""\d+(\.\d+)?|\.\d+""")

    /** One corner, or null if the text is not a valid place. */
    fun parseCorner(text: String): Coordinates? {
        val match = PAIR.matchEntire(text.trim().replace('−', '-')) ?: return null
        val (latitudeText, longitudeText) = match.destructured
        val latitude = Coordinates.parseLatitude(latitudeText) ?: return null
        val longitude = Coordinates.parseLongitude(longitudeText) ?: return null
        return Coordinates(latitude, longitude)
    }

    /**
     * Every "latitude, longitude" pair in [text], for pasting several corners at once (one per
     * line, or one after the other). Each comes back written the way a single corner is.
     */
    fun findPairs(text: String): List<String> =
        PAIR.findAll(text.replace('−', '-')).map { "${it.groupValues[1]}, ${it.groupValues[2]}" }.toList()

    /** The spacing in metres (a comma works as decimal separator too), or null if out of range. */
    fun parseSpacing(text: String): Double? {
        val normalized = text.trim().replace(',', '.')
        if (!NUMBER.matches(normalized)) return null
        return normalized.toDoubleOrNull()?.takeIf { it in AreaGrid.MIN_SPACING..AreaGrid.MAX_SPACING }
    }

    /** How a picked point is stored: seven decimals, about a centimetre. */
    fun format(degrees: Double): String = String.format(Locale.US, "%.7f", degrees)
}
