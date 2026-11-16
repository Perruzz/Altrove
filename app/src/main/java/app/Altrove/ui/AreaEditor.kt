package app.altrove.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.altrove.Coordinates
import app.altrove.R
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

/** At most this many corners: plenty for any outline typed by hand. */
internal const val MAX_CORNERS = 24

private val FieldShape = RoundedCornerShape(18.dp)

/** What the area editor says under its corners: how many points fit, or what is missing. */
internal sealed interface AreaSummary {
    data class Points(val count: Long, val exact: Boolean, val spacingMeters: Double) : AreaSummary
    data object Counting : AreaSummary
    data class Problem(val message: String) : AreaSummary
}

/**
 * The destination as an area: its corners, the spacing of the grid laid over it, and the button
 * that lands on one of the grid points at random.
 *
 * @param invalidCorners rows that hold something, but not a valid "latitude, longitude".
 * @param rollTurns grows by one at each step of the draw, to spin the die.
 * @param shown the point to show under the button: the one being drawn, or the destination.
 */
@Composable
internal fun AreaEditor(
    corners: List<String>,
    invalidCorners: Set<Int>,
    onCornerChange: (Int, String) -> Unit,
    onAddCorner: () -> Unit,
    onRemoveCorner: (Int) -> Unit,
    spacing: String,
    spacingValid: Boolean,
    onSpacingChange: (String) -> Unit,
    summary: AreaSummary,
    canPick: Boolean,
    rolling: Boolean,
    rollTurns: Int,
    shown: Coordinates?,
    onPick: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    // No size animation here: the card around animates already, and clipping at this level
    // would cut the glow of the button.
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        corners.forEachIndexed { index, text ->
            key(index) {
                CornerField(
                    value = text,
                    onValueChange = { onCornerChange(index, it) },
                    label = stringResource(R.string.area_corner, index + 1),
                    isError = index in invalidCorners,
                    onRemove = if (corners.size > 3) {
                        { onRemoveCorner(index) }
                    } else {
                        null
                    },
                    removeDescription = stringResource(R.string.area_remove_corner, index + 1),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                )
            }
        }

        GhostButton(
            onClick = onAddCorner,
            enabled = corners.size < MAX_CORNERS,
            modifier = Modifier.height(42.dp),
        ) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.area_add_corner))
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            OutlinedTextField(
                value = spacing,
                onValueChange = { text -> onSpacingChange(text.filter { it.isDigit() || it in ".," }.take(9)) },
                modifier = Modifier.width(124.dp),
                label = { Text(stringResource(R.string.area_spacing)) },
                suffix = { Text("m") },
                isError = !spacingValid,
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                shape = FieldShape,
                colors = spaceFieldColors(),
            )
            AreaSummaryText(summary, Modifier.weight(1f))
        }

        Text(
            text = stringResource(R.string.area_tip),
            style = MaterialTheme.typography.bodySmall,
            color = Space.Muted,
        )

        // The die turns a quarter at each step of the draw, and settles with a little bounce.
        val turn by animateFloatAsState(
            targetValue = rollTurns * 90f,
            animationSpec = spring(dampingRatio = 0.55f, stiffness = 420f),
            label = "die",
        )
        GlowButton(
            // Stays lit while the die spins; a tap in the meantime is simply ignored.
            onClick = { if (!rolling) onPick() },
            enabled = canPick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = listOf(Space.Magenta, Space.Violet),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_dice),
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = turn },
            )
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.area_pick), fontWeight = FontWeight.SemiBold)
        }

        if (shown != null) {
            Text(
                text = stringResource(
                    R.string.area_destination,
                    String.format(Locale.US, "%.6f, %.6f", shown.latitude, shown.longitude),
                ),
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                color = if (rolling) Space.Gold else Space.Starlight,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CornerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean,
    onRemove: (() -> Unit)?,
    removeDescription: String,
    keyboardActions: KeyboardActions,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = {
            Text(stringResource(R.string.area_corner_example), color = Space.Starlight.copy(alpha = 0.3f))
        },
        trailingIcon = onRemove?.let { remove ->
            {
                IconButton(onClick = remove) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = removeDescription,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        },
        isError = isError,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
        // The decimal keypad has the comma, the space and the minus a pair needs.
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        keyboardActions = keyboardActions,
        shape = FieldShape,
        colors = spaceFieldColors(),
    )
}

@Composable
private fun AreaSummaryText(summary: AreaSummary, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = summary,
        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
        contentKey = { it::class },
        label = "summary",
        modifier = modifier,
    ) { state ->
        when (state) {
            is AreaSummary.Points -> Column {
                val number = NumberFormat.getIntegerInstance().format(state.count)
                Text(
                    text = pluralStringResource(
                        if (state.exact) R.plurals.area_points else R.plurals.area_points_estimate,
                        state.count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                        number,
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                    color = Color.White,
                )
                Text(
                    text = stringResource(R.string.area_spacing_note, DecimalFormat("0.##").format(state.spacingMeters)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Space.Muted,
                )
            }

            AreaSummary.Counting -> Text(
                text = stringResource(R.string.area_counting),
                style = MaterialTheme.typography.bodyMedium,
                color = Space.Muted,
            )

            is AreaSummary.Problem -> Text(
                text = state.message,
                style = MaterialTheme.typography.bodyMedium,
                color = Space.Danger,
            )
        }
    }
}
