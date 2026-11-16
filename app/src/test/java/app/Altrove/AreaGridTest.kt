package app.altrove

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AreaGridTest {

    private val metersPerDegree = 6_371_008.8 * PI / 180.0
    private val rome = Coordinates(41.8902, 12.4922)

    /**
     * A polygon given in metres east and north of [origin], turned [degrees] counter-clockwise,
     * as map coordinates.
     */
    private fun polygon(origin: Coordinates, vararg points: Pair<Double, Double>, degrees: Double = 0.0): List<Coordinates> {
        val metersPerDegreeLongitude = metersPerDegree * cos(Math.toRadians(origin.latitude))
        val angle = Math.toRadians(degrees)
        return points.map { (x, y) ->
            val east = x * cos(angle) - y * sin(angle)
            val north = x * sin(angle) + y * cos(angle)
            Coordinates(origin.latitude + north / metersPerDegree, origin.longitude + east / metersPerDegreeLongitude)
        }
    }

    private fun square(origin: Coordinates, side: Double, degrees: Double = 0.0) =
        polygon(origin, 0.0 to 0.0, side to 0.0, side to side, 0.0 to side, degrees = degrees)

    @Test
    fun aTwentyMetreSquareHolds441PointsAMetreApart() {
        val grid = AreaGrid.of(square(rome, 20.0), 1.0)!!
        assertEquals(441, grid.count)
        assertTrue(grid.countIsExact)
        assertEquals(400.0, grid.surfaceSquareMeters, 0.01)
    }

    @Test
    fun theGridFollowsTheFirstSideOfATurnedSquare() {
        assertEquals(441, AreaGrid.of(square(rome, 20.0, degrees = 33.0), 1.0)!!.count)
    }

    @Test
    fun cornersTypedWithSixDecimalsStillGive441Points() {
        val corners = listOf(
            "41.890200, 12.492200",
            "41.890200, 12.492442",
            "41.890380, 12.492442",
            "41.890380, 12.492200",
        ).map { AreaInput.parseCorner(it)!! }
        assertEquals(441, AreaGrid.of(corners, 1.0)!!.count)
    }

    @Test
    fun cornersMayGoEitherWayRound() {
        assertEquals(441, AreaGrid.of(square(rome, 20.0).reversed(), 1.0)!!.count)
    }

    @Test
    fun aTriangleCountsItsBordersToo() {
        // Right triangle with 100 m legs: the points with i, j >= 0 and i + j <= 100.
        val triangle = polygon(rome, 0.0 to 0.0, 100.0 to 0.0, 0.0 to 100.0)
        assertEquals(101L * 102 / 2, AreaGrid.of(triangle, 1.0)!!.count)
    }

    @Test
    fun aConcavePolygonLeavesItsNotchOut() {
        // An L: a 20 m square without its 10 m top-right quarter.
        val l = polygon(rome, 0.0 to 0.0, 20.0 to 0.0, 20.0 to 10.0, 10.0 to 10.0, 10.0 to 20.0, 0.0 to 20.0)
        assertEquals(441L - 10 * 10, AreaGrid.of(l, 1.0)!!.count)
    }

    @Test
    fun everyPickIsAGridPointInsideAndAllOfThemComeUp() {
        val grid = AreaGrid.of(square(rome, 20.0), 1.0)!!
        val random = Random(2026)
        val seen = HashMap<Pair<Long, Long>, Int>()
        val metersPerDegreeLongitude = metersPerDegree * cos(Math.toRadians(rome.latitude))
        repeat(40_000) {
            val point = grid.random(random)!!
            val east = (point.longitude - rome.longitude) * metersPerDegreeLongitude
            val north = (point.latitude - rome.latitude) * metersPerDegree
            // On the grid: whole metres from the first corner, give or take a millimetre.
            val i = Math.round(east)
            val j = Math.round(north)
            assertEquals(i.toDouble(), east, 0.001)
            assertEquals(j.toDouble(), north, 0.001)
            assertTrue(i in 0..20 && j in 0..20)
            seen.merge(i to j, 1, Int::plus)
        }
        assertEquals(441, seen.size)
        // Chi-square test of uniformity: for 440 degrees of freedom it averages 440 with a
        // spread of about 30; a biased picker would land far above.
        val expected = 40_000 / 441.0
        val chiSquare = seen.values.sumOf { (it - expected) * (it - expected) / expected }
        assertTrue("chi-square $chiSquare", chiSquare < 560)
    }

    @Test
    fun aSpacingLargerThanTheAreaLeavesTheFirstCorner() {
        val grid = AreaGrid.of(square(rome, 20.0), 50.0)!!
        assertEquals(1, grid.count)
        assertEquals(rome.latitude, grid.random(Random(1))!!.latitude, 1e-9)
    }

    @Test
    fun anAreaAcrossThe180thMeridianWorks() {
        val grid = AreaGrid.of(square(Coordinates(-16.5, 179.9999), 20.0), 1.0)!!
        assertEquals(441, grid.count)
        val point = grid.random(Random(3))!!
        assertTrue(point.longitude >= 179.9999 - 1e-9 || point.longitude <= -179.999)
    }

    @Test
    fun aHugeAreaIsEstimatedAndStillPicksInside() {
        val grid = AreaGrid.of(square(rome, 100_000.0), 1.0)!!
        assertFalse(grid.countIsExact)
        // 100 km × 100 km, one point per square metre: an estimate, close to 10 billion.
        assertEquals(1e10, grid.count.toDouble(), 2e8)
        val point = grid.random(Random(4))!!
        assertTrue(point.latitude >= rome.latitude && point.latitude < rome.latitude + 0.91)
    }

    @Test
    fun cornersThatDoNotEncloseAnythingAreRejected() {
        assertNull(AreaGrid.of(square(rome, 20.0).take(2), 1.0))
        assertNull(AreaGrid.of(polygon(rome, 0.0 to 0.0, 10.0 to 0.0, 20.0 to 0.0), 1.0))
        assertNull(AreaGrid.of(listOf(rome, rome, rome), 1.0))
        assertNull(AreaGrid.of(square(rome, 20.0), 0.01))
    }

    @Test
    fun cornersAreReadTheWayGoogleMapsCopiesThem() {
        assertEquals(Coordinates(41.8902, 12.4922), AreaInput.parseCorner("41.8902, 12.4922"))
        assertEquals(Coordinates(41.8902, 12.4922), AreaInput.parseCorner(" 41.8902,12.4922 "))
        assertEquals(Coordinates(-33.85, 151.2), AreaInput.parseCorner("-33.85 151.2"))
        assertEquals(Coordinates(-33.85, 151.2), AreaInput.parseCorner("−33.85; 151.2"))
        assertNull(AreaInput.parseCorner("41.8902"))
        assertNull(AreaInput.parseCorner("91.0, 12.0"))
        assertNull(AreaInput.parseCorner("Rome"))
    }

    @Test
    fun severalPastedCornersAreSplit() {
        val pasted = "41.890200, 12.492200\n41.890200, 12.492442\n41.890380,12.492442"
        assertEquals(listOf("41.890200, 12.492200", "41.890200, 12.492442", "41.890380, 12.492442"), AreaInput.findPairs(pasted))
    }

    @Test
    fun theSpacingAcceptsCommasAndStaysInRange() {
        assertEquals(0.5, AreaInput.parseSpacing("0,5")!!, 0.0)
        assertEquals(1.0, AreaInput.parseSpacing("1")!!, 0.0)
        assertNull(AreaInput.parseSpacing("0.01"))
        assertNull(AreaInput.parseSpacing("-1"))
        assertNull(AreaInput.parseSpacing(""))
    }
}
