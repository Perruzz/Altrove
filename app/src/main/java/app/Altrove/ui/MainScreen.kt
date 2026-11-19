package app.altrove.ui

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.altrove.AltroveWidgetProvider
import app.altrove.AreaGrid
import app.altrove.AreaInput
import app.altrove.Coordinates
import app.altrove.MockLocationService
import app.altrove.Prefs
import app.altrove.R
import app.altrove.Session
import app.altrove.Setup
import app.altrove.ThemeMode
import app.altrove.ui.space.Globe
import app.altrove.ui.space.SpaceBackground
import app.altrove.ui.space.SpaceScene
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private const val MAX_FIELD_LENGTH = 20
private const val MAX_CORNER_LENGTH = 40
private const val BUY_ME_A_COFFEE = "https://buymeacoffee.com/peruzz"

/** The grid computed for a given set of corners and spacing (null if they make no area). */
private data class AreaResult(val key: Pair<List<Coordinates>, Double>?, val grid: AreaGrid?)

/**
 * The one screen of the app: a sky, the Earth turned towards the destination, and under it
 * the destination itself: a point typed in two fields, or an area to draw a point from.
 */
@Composable
fun MainScreen(themeMode: ThemeMode, onThemeChange: (mode: ThemeMode, origin: Offset, reveal: Boolean) -> Unit) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val session by Session.active.collectAsStateWithLifecycle()
    val widgetPlaced by AltroveWidgetProvider.placed.collectAsStateWithLifecycle()

    var isMockApp by remember { mutableStateOf(Setup.isMockLocationApp(context)) }
    // Saveable: a language change recreates the screen, and the panel should still be open.
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val openSupport: () -> Unit = { runCatching { uriHandler.openUri(BUY_ME_A_COFFEE) } }
    var missingPermissions by remember { mutableStateOf(Setup.missingOptionalPermissions(context)) }
    var permissionsHandled by remember { mutableStateOf(Prefs.optionalPermissionsHandled(context)) }
    var destination by remember { mutableStateOf(Prefs.destination(context)) }

    // The two fields live here, not in their card: the globe previews what is typed in them,
    // and a tap on the globe fills them in.
    var latitude by rememberSaveable { mutableStateOf(Prefs.latitudeText(context)) }
    var longitude by rememberSaveable { mutableStateOf(Prefs.longitudeText(context)) }
    var justSaved by remember { mutableStateOf(false) }

    val parsedLatitude = Coordinates.parseLatitude(latitude)
    val parsedLongitude = Coordinates.parseLongitude(longitude)
    val typed = if (parsedLatitude != null && parsedLongitude != null) {
        Coordinates(parsedLatitude, parsedLongitude)
    } else {
        null
    }
    // What is in the fields, when it is a valid place that has not been saved yet.
    val draft = typed?.takeIf { it != destination }

    // --- The area mode: corners, spacing, and the grid of points laid over them -------------

    var areaMode by rememberSaveable { mutableStateOf(Prefs.areaMode(context)) }
    val corners = rememberSaveable(saver = listSaver(save = { it.toList() }, restore = { it.toMutableStateList() })) {
        Prefs.areaCorners(context).toMutableStateList()
    }
    var spacing by rememberSaveable { mutableStateOf(Prefs.areaSpacing(context)) }
    // While a point is being drawn: the candidate on show, and the steps so far (they spin the die).
    var rolling by remember { mutableStateOf<Coordinates?>(null) }
    var rollTurns by remember { mutableIntStateOf(0) }

    val parsedCorners = corners.map { AreaInput.parseCorner(it) }
    val invalidCorners = corners.indices.filter { corners[it].isNotBlank() && parsedCorners[it] == null }.toSet()
    val validCorners = parsedCorners.filterNotNull()
    val spacingMeters = AreaInput.parseSpacing(spacing)
    val areaInputProblem = when {
        invalidCorners.isNotEmpty() -> stringResource(R.string.area_bad_corner, invalidCorners.min() + 1)
        validCorners.size < 3 -> stringResource(R.string.area_need_corners)
        spacingMeters == null -> stringResource(R.string.area_bad_spacing)
        else -> null
    }
    val areaKey = if (areaInputProblem == null && spacingMeters != null) validCorners to spacingMeters else null
    // Counting the points can take a moment for large areas: off the main thread, and only once
    // the typing pauses.
    val areaResult by produceState(AreaResult(null, null), areaKey) {
        val key = areaKey ?: return@produceState
        delay(150)
        value = AreaResult(key, withContext(Dispatchers.Default) { AreaGrid.of(key.first, key.second) })
    }
    val grid = areaResult.grid?.takeIf { areaResult.key == areaKey }
    val areaSummary = when {
        areaInputProblem != null -> AreaSummary.Problem(areaInputProblem)
        // Still counting: keep showing the previous count rather than flicker.
        areaResult.key != areaKey -> areaResult.grid?.takeIf { it.count > 0 }
            ?.let { AreaSummary.Points(it.count, it.countIsExact, spacingMeters ?: it.spacingMeters) }
            ?: AreaSummary.Counting
        grid == null -> AreaSummary.Problem(stringResource(R.string.area_flat))
        grid.count == 0L -> AreaSummary.Problem(stringResource(R.string.area_empty))
        else -> AreaSummary.Points(grid.count, grid.countIsExact, grid.spacingMeters)
    }

    // The area is kept as typed, valid or not, so that it is there the next time.
    val cornersSnapshot = corners.toList()
    LaunchedEffect(cornersSnapshot, spacing) {
        delay(400)
        Prefs.saveArea(context, cornersSnapshot, spacing)
    }

    // Back from Settings or from the launcher: re-read whatever may have changed out there.
    LifecycleResumeEffect(Unit) {
        isMockApp = Setup.isMockLocationApp(context)
        missingPermissions = Setup.missingOptionalPermissions(context)
        AltroveWidgetProvider.refresh(context)
        onPauseOrDispose { }
    }

    val permissionRequest = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        // They are optional and were asked once: whatever the answers, don't ask again.
        missingPermissions = Setup.missingOptionalPermissions(context)
        Prefs.setOptionalPermissionsHandled(context)
        permissionsHandled = true
    }
    val permissionsPrompt = if (permissionsHandled) null else optionalPermissionsPrompt(missingPermissions)

    // --- The scene and its choreography -------------------------------------------------

    val scene = remember {
        SpaceScene(calm = !ValueAnimator.areAnimatorsEnabled()).also { it.globe.target = destination }
    }
    val active = session != null
    val mood by animateFloatAsState(if (active) 1f else 0f, tween(900), label = "mood")

    // The globe looks at what is being typed (or drawn); failing that, at where you are, or
    // will be, or at the area.
    SideEffect {
        scene.globe.target = if (areaMode) {
            rolling ?: session?.destination ?: areaResult.grid?.center ?: destination
        } else {
            draft ?: session?.destination ?: destination
        }
    }

    var introPlayed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(scene) {
        launch { scene.run() }
        if (scene.calm || introPlayed) {
            scene.skipIntro()
        } else {
            introPlayed = true
            scene.playIntro()
        }
    }

    // A session starting or ending while the screen is open, whoever started it.
    var shownActive by remember { mutableStateOf(active) }
    LaunchedEffect(active) {
        if (active == shownActive) return@LaunchedEffect
        shownActive = active
        if (active) scene.playLaunch() else scene.playReturn()
    }

    LaunchedEffect(justSaved) {
        if (justSaved) {
            delay(2200)
            justSaved = false
        }
    }

    fun edit(text: String, assign: (String) -> Unit) {
        val pair = Coordinates.splitPair(text)
        if (pair != null) {
            // A whole "lat, lng" was pasted: spread it over the two fields.
            latitude = pair.first
            longitude = pair.second
        } else {
            assign(text.filter { it.isDigit() || it in ".,+-−" }.take(MAX_FIELD_LENGTH))
        }
        justSaved = false
    }

    /** Makes [place] the destination, with the save flourish on the globe. */
    fun commit(place: Coordinates, latitudeText: String, longitudeText: String) {
        Prefs.saveDestination(context, latitudeText, longitudeText)
        latitude = latitudeText
        longitude = longitudeText
        destination = place
        if (Session.isActive) {
            // Move the running session too, and fix the text in its notification.
            Session.retarget(place)
            context.startService(MockLocationService.intent(context, MockLocationService.ACTION_REFRESH))
        }
        AltroveWidgetProvider.refresh(context)
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        scope.launch { scene.playSave() }
    }

    fun save() {
        val place = draft ?: return
        commit(place, Coordinates.normalize(latitude), Coordinates.normalize(longitude))
        justSaved = true
        focusManager.clearFocus()
    }

    /** Draws a point of the area, like a slot machine: a few quick candidates, then the one. */
    fun pickRandom() {
        val area = grid ?: return
        if (rolling != null) return
        focusManager.clearFocus()
        scope.launch {
            if (!scene.calm) {
                for (step in 0 until 9) {
                    rolling = area.random() ?: break
                    rollTurns++
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    delay(45L + step * step * 6L)
                }
            }
            val pick = area.random()
            rollTurns++
            rolling = null
            if (pick != null) commit(pick, AreaInput.format(pick.latitude), AreaInput.format(pick.longitude))
        }
    }

    fun editCorner(index: Int, text: String) {
        val previous = corners.getOrNull(index) ?: return
        val pasted = AreaInput.findPairs(text)
        if (text.length - previous.length > 1 && pasted.size >= 2) {
            // Several corners pasted at once: one per row, adding rows as needed.
            pasted.forEachIndexed { offset, pair ->
                val row = index + offset
                if (row < corners.size) corners[row] = pair else if (corners.size < MAX_CORNERS) corners.add(pair)
            }
        } else {
            corners[index] = text.filter { it.isDigit() || it in ".,;+-− " }.take(MAX_CORNER_LENGTH)
        }
    }

    // --- Layout ---------------------------------------------------------------------------

    // While typing, the globe shrinks so that it stays in view above the keyboard.
    val typing = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val globeSize by animateDpAsState(
        targetValue = if (typing) 168.dp else 304.dp,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 280f),
        label = "globe",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Space.Night),
    ) {
        SpaceBackground(scene = scene, active = { mood }, modifier = Modifier.fillMaxSize())

        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Header(
                    compact = typing,
                    onSettings = { settingsOpen = true },
                    modifier = Modifier.entrance(scene, order = 0),
                )

                // The canvas is larger than the room it takes: its margin (halo, orbit, ring)
                // is allowed to overlap what is above and below.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(globeSize - 36.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Globe(
                        scene = scene,
                        destination = destination,
                        draft = if (areaMode) rolling else draft,
                        area = validCorners.takeIf { areaMode && it.size >= 3 },
                        session = session,
                        active = { mood },
                        onPick = { place ->
                            if (areaMode) {
                                // In area mode a tap adds a corner: handy to sketch a large area.
                                val pair = String.format(Locale.US, "%.4f, %.4f", place.latitude, place.longitude)
                                val blank = corners.indexOfFirst { it.isBlank() }
                                when {
                                    blank >= 0 -> corners[blank] = pair
                                    corners.size < MAX_CORNERS -> corners.add(pair)
                                }
                            } else {
                                latitude = String.format(Locale.US, "%.4f", place.latitude)
                                longitude = String.format(Locale.US, "%.4f", place.longitude)
                                justSaved = false
                            }
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        modifier = Modifier
                            .requiredSize(globeSize)
                            .graphicsLayer {
                                // It arrives first, growing out of the distance.
                                val p = (scene.intro.value / 0.6f).coerceIn(0f, 1f)
                                val eased = 1f - (1f - p) * (1f - p) * (1f - p)
                                alpha = eased
                                scaleX = 0.7f + 0.3f * eased
                                scaleY = 0.7f + 0.3f * eased
                                compositingStrategy = CompositingStrategy.ModulateAlpha
                            },
                    )
                }

                if (!isMockApp || permissionsPrompt != null) {
                    SetupCard(
                        showMockStep = !isMockApp,
                        permissionsPrompt = permissionsPrompt,
                        onAllowPermissions = { permissionRequest.launch(missingPermissions.toTypedArray()) },
                        onSkipPermissions = {
                            Prefs.setOptionalPermissionsHandled(context)
                            permissionsHandled = true
                        },
                        modifier = Modifier.entrance(scene, order = 1),
                    )
                }

                DestinationCard(
                    areaMode = areaMode,
                    onModeChange = { area ->
                        areaMode = area
                        Prefs.setAreaMode(context, area)
                        focusManager.clearFocus()
                    },
                    point = {
                        PointEditor(
                            latitude = latitude,
                            longitude = longitude,
                            onLatitudeChange = { text -> edit(text) { latitude = it } },
                            onLongitudeChange = { text -> edit(text) { longitude = it } },
                            parsedLatitude = parsedLatitude,
                            parsedLongitude = parsedLongitude,
                            canSave = draft != null,
                            justSaved = justSaved,
                            onSave = { save() },
                        )
                    },
                    area = {
                        AreaEditor(
                            corners = corners,
                            invalidCorners = invalidCorners,
                            onCornerChange = { index, text -> editCorner(index, text) },
                            onAddCorner = { if (corners.size < MAX_CORNERS) corners.add("") },
                            onRemoveCorner = { index -> if (corners.size > 3) corners.removeAt(index) },
                            spacing = spacing,
                            spacingValid = spacingMeters != null,
                            onSpacingChange = { spacing = it },
                            summary = areaSummary,
                            canPick = grid != null && grid.count > 0,
                            rolling = rolling != null,
                            rollTurns = rollTurns,
                            shown = rolling ?: destination,
                            onPick = { pickRandom() },
                        )
                    },
                    modifier = Modifier.entrance(scene, order = 2),
                )

                SessionCard(
                    session = session,
                    destination = destination,
                    isMockApp = isMockApp,
                    onStart = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        ContextCompat.startForegroundService(
                            context,
                            MockLocationService.intent(context, MockLocationService.ACTION_START),
                        )
                    },
                    onStop = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        context.startService(
                            MockLocationService.intent(context, MockLocationService.ACTION_STOP),
                        )
                    },
                    modifier = Modifier.entrance(scene, order = 3),
                )

                if (!widgetPlaced && AltroveWidgetProvider.canRequestPin(context)) {
                    GhostButton(
                        onClick = { AltroveWidgetProvider.requestPin(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .entrance(scene, order = 4),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_widgets),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.add_widget))
                    }
                }

                Text(
                    text = stringResource(R.string.footnote),
                    style = MaterialTheme.typography.bodySmall,
                    color = Space.Faint,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .entrance(scene, order = 4),
                )

                // Opens Buy Me a Coffee in the browser: the app itself still never goes online.
                GhostButton(
                    onClick = openSupport,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 8.dp)
                        .height(46.dp)
                        .entrance(scene, order = 5),
                    color = LocalSpace.current.link,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_coffee),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.support_coffee))
                }
            }
        }

        SettingsPanel(
            open = settingsOpen,
            onClose = { settingsOpen = false },
            themeMode = themeMode,
            onThemeChange = onThemeChange,
            onSupport = openSupport,
        )
    }
}

@Composable
private fun Header(compact: Boolean, onSettings: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    Brush.linearGradient(listOf(Color(0xFF9D8FFF), Color(0xFF4A3DD6))),
                    RoundedCornerShape(15.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_pin),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Space.Text,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onSettings, modifier = Modifier.size(40.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings),
                        contentDescription = stringResource(R.string.settings_title),
                        tint = Space.Muted,
                    )
                }
            }
            // Dropped while typing, to leave the room to the globe and the fields.
            AnimatedVisibility(visible = !compact) {
                Text(
                    text = stringResource(R.string.tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Space.Muted,
                )
            }
        }
    }
}

/** The text inviting to grant whichever optional permissions are missing, or null if none is. */
@SuppressLint("InlinedApi") // POST_NOTIFICATIONS is only compared as a string here.
@Composable
private fun optionalPermissionsPrompt(missing: List<String>): String? {
    val location = Manifest.permission.ACCESS_FINE_LOCATION in missing
    val notifications = Manifest.permission.POST_NOTIFICATIONS in missing
    return when {
        location && notifications -> stringResource(R.string.setup_permissions_both)
        location -> stringResource(R.string.setup_permissions_location)
        notifications -> stringResource(R.string.setup_permissions_notifications)
        else -> null
    }
}

@Composable
private fun SetupCard(
    showMockStep: Boolean,
    permissionsPrompt: String?,
    onAllowPermissions: () -> Unit,
    onSkipPermissions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val amber = listOf(Space.Amber, Color(0xFFFF9D5C))
    val palette = LocalSpace.current
    GlassCard(
        modifier = modifier,
        accent = Space.Amber,
        tint = palette.setupTint,
        animateSize = true,
    ) {
        Text(
            text = stringResource(R.string.setup_title),
            style = MaterialTheme.typography.titleMedium,
            color = palette.setupTitle,
        )

        if (showMockStep) {
            Text(
                text = stringResource(R.string.setup_mock_body),
                style = MaterialTheme.typography.bodyMedium,
                color = Space.Starlight,
            )
            GlowButton(
                onClick = { Setup.openDeveloperOptions(context) },
                modifier = Modifier.height(46.dp),
                colors = amber,
                contentColor = Space.Ink,
            ) {
                Text(stringResource(R.string.setup_mock_action), fontWeight = FontWeight.SemiBold)
            }
            // Apps can't reliably tell whether Developer options are on (Android 17 hides
            // it), so the way to enable them is always offered, as a quiet footnote.
            val hint = stringResource(R.string.setup_developer_hint)
            val link = stringResource(R.string.setup_developer_link)
            Text(
                text = buildAnnotatedString {
                    append(hint)
                    append(" ")
                    withLink(
                        LinkAnnotation.Clickable(
                            tag = "about-phone",
                            styles = TextLinkStyles(
                                SpanStyle(
                                    color = palette.setupTitle,
                                    fontWeight = FontWeight.Medium,
                                    textDecoration = TextDecoration.Underline,
                                ),
                            ),
                            linkInteractionListener = { Setup.openAboutPhone(context) },
                        ),
                    ) {
                        append(link)
                    }
                    append(".")
                },
                style = MaterialTheme.typography.bodySmall,
                color = Space.Muted,
            )
        }

        if (showMockStep && permissionsPrompt != null) {
            HorizontalDivider(color = Space.Edge.copy(alpha = 0.12f))
        }

        if (permissionsPrompt != null) {
            Text(
                text = permissionsPrompt,
                style = MaterialTheme.typography.bodyMedium,
                color = Space.Starlight,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlowButton(
                    onClick = onAllowPermissions,
                    modifier = Modifier.height(46.dp),
                    colors = amber,
                    contentColor = Space.Ink,
                ) {
                    Text(stringResource(R.string.setup_permissions_action), fontWeight = FontWeight.SemiBold)
                }
                GhostButton(onClick = onSkipPermissions, modifier = Modifier.height(46.dp)) {
                    Text(stringResource(R.string.setup_permissions_skip))
                }
            }
        }
    }
}

/** The destination: a point or an area, chosen with the switch in the corner. */
@Composable
private fun DestinationCard(
    areaMode: Boolean,
    onModeChange: (Boolean) -> Unit,
    point: @Composable () -> Unit,
    area: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier, animateSize = true) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.destination_title),
                style = MaterialTheme.typography.titleMedium,
                color = Space.Text,
                modifier = Modifier.weight(1f),
            )
            ModeSwitch(
                options = listOf(stringResource(R.string.mode_point), stringResource(R.string.mode_area)),
                selected = if (areaMode) 1 else 0,
                onSelect = { index, _ -> onModeChange(index == 1) },
            )
        }
        AnimatedContent(
            targetState = areaMode,
            transitionSpec = {
                (fadeIn(tween(220, delayMillis = 80)) togetherWith fadeOut(tween(80))).using(null)
            },
            label = "mode",
        ) { isArea ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isArea) area() else point()
            }
        }
    }
}

/** A single point: latitude and longitude, typed or pasted, and saved with the button. */
@Composable
private fun PointEditor(
    latitude: String,
    longitude: String,
    onLatitudeChange: (String) -> Unit,
    onLongitudeChange: (String) -> Unit,
    parsedLatitude: Double?,
    parsedLongitude: Double?,
    canSave: Boolean,
    justSaved: Boolean,
    onSave: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    CoordinateField(
        value = latitude,
        onValueChange = onLatitudeChange,
        label = stringResource(R.string.latitude),
        example = stringResource(R.string.latitude_example),
        error = stringResource(R.string.latitude_error)
            .takeIf { latitude.isNotBlank() && parsedLatitude == null },
        hemisphere = parsedLatitude?.let { if (it < 0) "S" else "N" },
        imeAction = ImeAction.Next,
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
    )
    CoordinateField(
        value = longitude,
        onValueChange = onLongitudeChange,
        label = stringResource(R.string.longitude),
        example = stringResource(R.string.longitude_example),
        error = stringResource(R.string.longitude_error)
            .takeIf { longitude.isNotBlank() && parsedLongitude == null },
        hemisphere = parsedLongitude?.let { if (it < 0) "W" else "E" },
        imeAction = ImeAction.Done,
        keyboardActions = KeyboardActions(
            onDone = { if (canSave) onSave() else focusManager.clearFocus() },
        ),
    )

    Text(
        text = stringResource(R.string.destination_tip),
        style = MaterialTheme.typography.bodySmall,
        color = Space.Muted,
    )

    GlowButton(
        onClick = onSave,
        // Stays lit for a moment after saving, to show the "Saved" confirmation.
        enabled = canSave || justSaved,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        colors = if (justSaved) listOf(Color(0xFF3CCB9B), Color(0xFF1FA6B8)) else listOf(Space.Violet, Space.VioletDeep),
    ) {
        AnimatedContent(targetState = justSaved, label = "save") { saved ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (saved) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = stringResource(if (saved) R.string.saved else R.string.save),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: Session.Active?,
    destination: Coordinates?,
    isMockApp: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val active = session != null
    val palette = LocalSpace.current
    val accent by animateColorAsState(if (active) palette.magenta else palette.panelEdge, tween(700), label = "accent")
    val tint by animateColorAsState(if (active) palette.activeTint else palette.panel, tween(700), label = "tint")

    // The size animation clips to the card, not to the row: the glow of the button needs room.
    GlassCard(modifier = modifier, accent = accent, tint = tint, animateSize = true) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(
                        if (active) R.string.status_active_title else R.string.status_idle_title,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = Space.Text,
                )
                if (session != null) {
                    RollingCountdown(
                        seconds = secondsLeft(session),
                        style = MaterialTheme.typography.displaySmall.copy(
                            color = Space.Text,
                            fontFeatureSettings = "tnum",
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                    Text(
                        text = stringResource(R.string.status_active_body, session.destination.format()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Space.Starlight.copy(alpha = 0.86f),
                    )
                } else {
                    Text(
                        text = stringResource(
                            when {
                                !isMockApp -> R.string.status_idle_body_setup
                                destination == null -> R.string.status_idle_body_no_destination
                                else -> R.string.status_idle_body
                            },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Space.Muted,
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            if (active) {
                GhostButton(onClick = onStop, modifier = Modifier.height(48.dp), color = Space.Text) {
                    Text(stringResource(R.string.stop), fontWeight = FontWeight.SemiBold)
                }
            } else {
                GlowButton(
                    onClick = onStart,
                    modifier = Modifier.height(48.dp),
                    enabled = isMockApp && destination != null,
                    colors = listOf(Space.Cyan, Color(0xFF7C8CFF)),
                    contentColor = Space.Ink,
                ) {
                    Text(stringResource(R.string.start), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** Whole seconds left in the session, ticking down in step with the widget and the notification. */
@Composable
private fun secondsLeft(session: Session.Active): Long {
    fun now() = ((session.endElapsedRealtime - SystemClock.elapsedRealtime()) / 1000).coerceAtLeast(0)

    var seconds by remember(session.endElapsedRealtime) { mutableLongStateOf(now()) }
    LaunchedEffect(session.endElapsedRealtime) {
        while (true) {
            val remaining = session.endElapsedRealtime - SystemClock.elapsedRealtime()
            seconds = (remaining / 1000).coerceAtLeast(0)
            if (remaining <= 0) break
            // Sleep until just past the next whole second.
            delay(remaining % 1000 + 1)
        }
    }
    return seconds
}
