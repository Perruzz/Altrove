package app.altrove.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.altrove.ui.space.SpaceScene
import java.util.Locale

private val CardShape = RoundedCornerShape(28.dp)
private val FieldShape = RoundedCornerShape(18.dp)

/**
 * A translucent panel with a lit upper edge, the way glass catches light. The sky stays
 * faintly visible through it.
 *
 * @param accent colour of the lit edge.
 * @param tint colour of the pane itself.
 * @param opaque for a pane floating over the screen, which must hide what is underneath.
 * @param animateSize animates changes of height. Done here rather than by the caller, so that
 *   the size animation (which clips) sits inside the shadow and doesn't cut it.
 */
@Composable
internal fun GlassCard(
    modifier: Modifier = Modifier,
    accent: Color = LocalSpace.current.panelEdge,
    tint: Color = Space.Panel,
    opaque: Boolean = false,
    animateSize: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalSpace.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (palette.dark) {
                    Modifier
                } else {
                    // On the white sky a glass pane needs a shadow to stand out.
                    Modifier.shadow(14.dp, CardShape, clip = false, ambientColor = palette.violet, spotColor = palette.violet)
                },
            )
            .then(if (animateSize) Modifier.animateContentSize() else Modifier)
            .background(
                // Opaque enough that stars never sparkle behind text, sheer enough to show the sky.
                Brush.verticalGradient(
                    listOf(
                        tint.copy(alpha = if (opaque) 0.97f else palette.panelOpacity - 0.05f),
                        palette.panelBottom.copy(alpha = if (opaque) 0.99f else palette.panelOpacity + 0.05f),
                    ),
                ),
                CardShape,
            )
            .border(
                1.dp,
                Brush.verticalGradient(listOf(accent.copy(alpha = 0.4f), accent.copy(alpha = 0.07f))),
                CardShape,
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

/**
 * The main kind of button: a gradient pill that glows, and gives a little under the finger.
 */
@Composable
internal fun GlowButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: List<Color> = listOf(Space.Violet, Space.VioletDeep),
    contentColor: Color = Color.White,
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 500f),
        label = "press",
    )
    val glow = colors.first()
    Button(
        onClick = onClick,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (enabled) {
                    // A coloured shadow reads as a glow on a dark sky.
                    Modifier.shadow(18.dp, CircleShape, clip = false, ambientColor = glow, spotColor = glow)
                } else {
                    Modifier
                },
            )
            .background(
                if (enabled) Brush.horizontalGradient(colors) else SolidColor(Space.Edge.copy(alpha = 0.08f)),
                CircleShape,
            ),
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = contentColor,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Space.Edge.copy(alpha = 0.38f),
        ),
        contentPadding = PaddingValues(horizontal = 24.dp),
        interactionSource = interaction,
        content = content,
    )
}

/** The quiet kind of button: just an outline on the glass. */
@Composable
internal fun GhostButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = Space.Starlight,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Space.Edge.copy(alpha = 0.06f),
            contentColor = color,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Space.Edge.copy(alpha = 0.3f),
        ),
        border = BorderStroke(1.dp, color.copy(alpha = 0.34f)),
        contentPadding = PaddingValues(horizontal = 22.dp),
        content = content,
    )
}

/**
 * One coordinate. Shows which hemisphere the number falls in as soon as it makes sense.
 *
 * @param hemisphere "N", "S", "E" or "W" for a valid value, null otherwise.
 */
@Composable
internal fun CoordinateField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    example: String,
    error: String?,
    hemisphere: String?,
    imeAction: ImeAction,
    keyboardActions: KeyboardActions,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = {
            // Clearly fainter than a typed value, so an empty field never looks filled in.
            Text(example, color = Space.Starlight.copy(alpha = 0.3f))
        },
        suffix = { Text("°") },
        trailingIcon = {
            AnimatedContent(targetState = hemisphere, label = "hemisphere") { letter ->
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(
                            if (letter == null) Color.Transparent else Space.Violet.copy(alpha = 0.26f),
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (letter != null) {
                        Text(
                            text = letter,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Space.Lavender,
                        )
                    }
                }
            }
        },
        supportingText = error?.let { message -> { Text(message) } },
        isError = error != null,
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
        keyboardActions = keyboardActions,
        shape = FieldShape,
        colors = spaceFieldColors(),
    )
}

/** Text fields on the glass: a faint fill, and cyan while typing. */
@Composable
internal fun spaceFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Space.Text,
    unfocusedTextColor = Space.Starlight,
    errorTextColor = Space.Starlight,
    focusedContainerColor = Space.Edge.copy(alpha = 0.07f),
    unfocusedContainerColor = Space.Edge.copy(alpha = 0.04f),
    errorContainerColor = Space.Danger.copy(alpha = 0.07f),
    cursorColor = Space.Cyan,
    focusedBorderColor = Space.Cyan,
    unfocusedBorderColor = Space.Edge.copy(alpha = 0.2f),
    focusedLabelColor = Space.Cyan,
    unfocusedLabelColor = Space.Muted,
    focusedSuffixColor = Space.Muted,
    unfocusedSuffixColor = Space.Muted,
    errorSuffixColor = Space.Muted,
    focusedTrailingIconColor = Space.Muted,
    unfocusedTrailingIconColor = Space.Muted,
    errorTrailingIconColor = Space.Muted,
)

/**
 * Two or more choices side by side, with a glowing pill sliding under the chosen one.
 *
 * [onSelect] also gets the centre of the tapped choice, in root coordinates: the theme switch
 * starts its circular reveal from there.
 */
@Composable
internal fun ModeSwitch(
    options: List<String>,
    selected: Int,
    onSelect: (index: Int, origin: Offset) -> Unit,
    modifier: Modifier = Modifier,
    segment: Dp = 76.dp,
) {
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val offset by animateDpAsState(
        targetValue = segment * selected,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 520f),
        label = "switch",
    )
    Box(
        modifier = modifier
            .height(38.dp)
            .width(segment * options.size)
            .background(Space.Edge.copy(alpha = 0.06f), CircleShape)
            .border(1.dp, Space.Edge.copy(alpha = 0.14f), CircleShape)
            .onGloballyPositioned { coordinates = it },
    ) {
        Box(
            modifier = Modifier
                // Read during layout: the pill slides without recomposing anything.
                .offset { IntOffset(offset.roundToPx(), 0) }
                .width(segment)
                .fillMaxHeight()
                .padding(3.dp)
                .shadow(10.dp, CircleShape, clip = false, ambientColor = Space.Violet, spotColor = Space.Violet)
                .background(Brush.horizontalGradient(listOf(Space.Violet, Space.VioletDeep)), CircleShape),
        )
        Row(modifier = Modifier.selectableGroup()) {
            options.forEachIndexed { index, label ->
                val chosen = index == selected
                val color by animateColorAsState(if (chosen) Color.White else Space.Muted, label = "label")
                Box(
                    modifier = Modifier
                        .width(segment)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .selectable(
                            selected = chosen,
                            role = Role.Tab,
                            onClick = {
                                val box = coordinates
                                val origin = if (box == null || !box.isAttached) {
                                    Offset.Unspecified
                                } else {
                                    val width = box.size.width.toFloat() / options.size
                                    box.localToRoot(Offset(width * (index + 0.5f), box.size.height / 2f))
                                }
                                onSelect(index, origin)
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = label, style = MaterialTheme.typography.labelLarge, color = color)
                }
            }
        }
    }
}

/**
 * A "mm:ss" countdown whose digits roll over instead of just changing.
 */
@Composable
internal fun RollingCountdown(seconds: Long, style: TextStyle, modifier: Modifier = Modifier) {
    // Locale.ROOT: always Latin digits, like the widget and the notification.
    val text = String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60)
    Row(modifier) {
        text.forEachIndexed { index, character ->
            if (character.isDigit()) {
                AnimatedContent(
                    targetState = character,
                    transitionSpec = {
                        // Counting down: the new digit drops in from above.
                        (slideInVertically { -it / 2 } + fadeIn()) togetherWith
                            (slideOutVertically { it / 2 } + fadeOut())
                    },
                    label = "digit$index",
                ) { digit ->
                    Text(digit.toString(), style = style)
                }
            } else {
                Text(character.toString(), style = style)
            }
        }
    }
}

/**
 * The entrance of a block of the screen: it fades and rises into place, later the higher
 * its [order], in step with the stars slowing down.
 */
internal fun Modifier.entrance(scene: SpaceScene, order: Int): Modifier = graphicsLayer {
    val start = 0.2f + order * 0.09f
    val p = ((scene.intro.value - start) / 0.4f).coerceIn(0f, 1f)
    val eased = 1f - (1f - p) * (1f - p) * (1f - p)
    alpha = eased
    translationY = (1f - eased) * 40.dp.toPx()
    compositingStrategy = CompositingStrategy.ModulateAlpha
}
