package com.lamario.t9fold.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lamario.t9fold.LauncherState
import com.lamario.t9fold.PhoneKey
import com.lamario.t9fold.search.T9

/**
 * The candybar phone: earpiece, LCD, navigation keys, keypad.
 * [mirrored] flips the left/right placement of every key, as if the faceplate were lying face down.
 */
@Composable
fun PhoneBody(state: LauncherState, mirrored: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .background(Retro.Body)
            .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val skin = Retro
        Earpiece()
        Spacer(Modifier.height(8.dp))
        LcdPanel(Modifier.fillMaxWidth().weight(skin.lcdWeight)) {
            LcdContent(state, mirrored)
        }
        if (skin.flip) Hinge() else Spacer(Modifier.height(12.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f - skin.lcdWeight)
                .swipeDownToExpand(state::expandNotifications),
        ) {
            NavCluster(state, mirrored, Modifier.fillMaxWidth().weight(0.3f))
            Spacer(Modifier.height(skin.keyGap.coerceAtLeast(8.dp)))
            Keypad(state, mirrored, Modifier.fillMaxWidth().weight(0.7f))
        }
    }
}

@Composable
private fun Earpiece() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        when (Retro.earpiece) {
            EarpieceStyle.SLOT -> Box(
                Modifier
                    .width(56.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Retro.Bezel),
            )
            // Rows of speaker slots, like the old bricks.
            EarpieceStyle.GRILLE -> Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(3) {
                    Box(
                        Modifier
                            .width(72.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Retro.Bezel),
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        PixelText(Retro.brand, size = 14.dp, color = Retro.Chrome)
    }
}

/** The barrel hinge between the screen half and the keypad half of a flip phone. */
@Composable
private fun Hinge() {
    val skin = Retro
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .height(14.dp)
            .shadow(3.dp, RoundedCornerShape(7.dp))
            .clip(RoundedCornerShape(7.dp))
            .background(Brush.verticalGradient(listOf(skin.BodyDark, skin.Chrome, skin.BodyDark))),
    )
}

// ---- Navigation cluster ----------------------------------------------------------------------

@Composable
private fun NavCluster(state: LauncherState, mirrored: Boolean, modifier: Modifier) {
    val left: @Composable RowScope.() -> Unit = {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SoftKey(state, PhoneKey.SOFT_LEFT)
            CallKey(state, PhoneKey.CALL, mirrored)
        }
    }
    val right: @Composable RowScope.() -> Unit = {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SoftKey(state, PhoneKey.SOFT_RIGHT)
            CallKey(state, PhoneKey.END, mirrored)
        }
    }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (mirrored) right() else left()
        Rocker(state, Modifier.weight(0.75f).fillMaxHeight())
        if (mirrored) left() else right()
    }
}

@Composable
private fun ColumnScope.SoftKey(state: LauncherState, key: PhoneKey) {
    RetroKey(
        state = state,
        key = key,
        light = Retro.Chrome,
        dark = Retro.ChromeDark,
        shape = RoundedCornerShape(50),
        modifier = Modifier.fillMaxWidth().weight(0.42f),
        onLongClick = if (key == PhoneKey.SOFT_RIGHT) ({ state.onLongKey(key) }) else null,
    ) {
        val label = Retro.keyLabels[key]
        if (label != null) {
            KeyText(label, size = 13.dp, color = Retro.KeyText)
        } else {
            Box(
                Modifier
                    .width(22.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Retro.BodyDark.copy(alpha = 0.6f)),
            )
        }
    }
}

@Composable
private fun ColumnScope.CallKey(state: LauncherState, key: PhoneKey, mirrored: Boolean) {
    val isCall = key == PhoneKey.CALL
    RetroKey(
        state = state,
        key = key,
        light = if (isCall) Retro.CallLight else Retro.EndLight,
        dark = if (isCall) Retro.CallDark else Retro.EndDark,
        shape = RoundedCornerShape(50),
        modifier = Modifier.fillMaxWidth().weight(0.58f),
    ) {
        val glyph = if (isCall) Retro.CallGlyph else Retro.EndGlyph
        val label = Retro.keyLabels[key]
        if (label != null) {
            KeyText(label, size = 15.dp, color = glyph)
        } else {
            Handset(
                hungUp = !isCall,
                color = glyph,
                modifier = Modifier
                    .size(26.dp)
                    .graphicsLayer { scaleX = if (mirrored) -1f else 1f },
            )
        }
    }
}

@Composable
private fun Rocker(state: LauncherState, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        RetroKey(
            state = state,
            key = PhoneKey.UP,
            light = Retro.Chrome,
            dark = Retro.ChromeDark,
            shape = RoundedCornerShape(topStartPercent = 50, topEndPercent = 50, bottomStartPercent = 12, bottomEndPercent = 12),
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) { Arrow(up = true) }
        RetroKey(
            state = state,
            key = PhoneKey.DOWN,
            light = Retro.Chrome,
            dark = Retro.ChromeDark,
            shape = RoundedCornerShape(topStartPercent = 12, topEndPercent = 12, bottomStartPercent = 50, bottomEndPercent = 50),
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) { Arrow(up = false) }
    }
}

@Composable
private fun Arrow(up: Boolean) {
    val color = Retro.BodyDark.copy(alpha = 0.7f)
    Canvas(Modifier.size(14.dp, 9.dp)) {
        val path = Path().apply {
            if (up) {
                moveTo(size.width / 2, 0f); lineTo(size.width, size.height); lineTo(0f, size.height)
            } else {
                moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width / 2, size.height)
            }
            close()
        }
        drawPath(path, color)
    }
}

/** A chunky handset glyph; tilted down for the end key. */
@Composable
private fun Handset(hungUp: Boolean, color: Color, modifier: Modifier) {
    Canvas(modifier.rotate(if (hungUp) 135f else 0f)) {
        val stroke = size.minDimension * 0.2f
        val inset = stroke / 2 + size.minDimension * 0.08f
        drawArc(
            color = color,
            startAngle = 120f,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(size.width - inset * 2, size.height - inset * 2),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        val r = stroke * 0.85f
        val cx = size.width / 2
        val cy = size.height / 2
        val radius = (size.width - inset * 2) / 2
        for (angle in listOf(120.0, 240.0)) {
            val rad = Math.toRadians(angle)
            drawCircle(color, r, Offset(cx + radius * kotlin.math.cos(rad).toFloat(), cy + radius * kotlin.math.sin(rad).toFloat()))
        }
    }
}

// ---- Keypad --------------------------------------------------------------------------------

private val KEY_ROWS = listOf("123", "456", "789", "*0#")

@Composable
private fun Keypad(state: LauncherState, mirrored: Boolean, modifier: Modifier) {
    val gap = Retro.keyGap
    Column(modifier, verticalArrangement = Arrangement.spacedBy(gap * 0.75f)) {
        for (row in KEY_ROWS) {
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(gap)) {
                for (c in if (mirrored) row.reversed() else row) {
                    DigitKey(state, c, Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}

@Composable
private fun DigitKey(state: LauncherState, c: Char, modifier: Modifier) {
    val key = PhoneKey.of(c)
    val isFavoriteKey = c == '1' || c == '*' || c == '#'
    val sub = when {
        c in T9.KEY_LETTERS -> T9.KEY_LETTERS.getValue(c).uppercase()
        c == '0' -> "APPS"
        else -> state.favoriteLabel(c)?.take(5)?.uppercase() ?: "FAV"
    }
    RetroKey(
        state = state,
        key = key,
        light = Retro.KeyLight,
        dark = Retro.KeyDark,
        shape = RoundedCornerShape(percent = Retro.keyCornerPercent),
        modifier = modifier,
        onLongClick = if (isFavoriteKey) ({ state.onLongKey(key) }) else null,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KeyText(c.toString(), size = 22.dp, color = Retro.KeyText)
            Spacer(Modifier.width(6.dp))
            KeyText(sub, size = 10.dp, color = Retro.KeySubText, bold = false)
        }
    }
}

/**
 * A plastic key. Sound/vibration fire on touch-down like a real keypad; the action fires on release.
 * Without a long-press action, holding the key still counts as a normal press.
 */
@Composable
fun RetroKey(
    state: LauncherState,
    key: PhoneKey,
    light: Color,
    dark: Color,
    shape: Shape,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val longClick by rememberUpdatedState(onLongClick)
    val elevation: Dp = if (pressed) 1.dp else 4.dp
    Box(
        modifier
            .graphicsLayer { translationY = if (pressed) 2.dp.toPx() else 0f }
            .shadow(elevation, shape)
            .clip(shape)
            .background(Brush.verticalGradient(if (pressed) listOf(dark, light) else listOf(light, dark)))
            .border(1.dp, Color.Black.copy(alpha = 0.22f), shape)
            .pointerInput(key) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        state.feedback(key)
                        tryAwaitRelease()
                        pressed = false
                    },
                    onLongPress = { longClick?.invoke() ?: state.onKey(key) },
                    onTap = { state.onKey(key) },
                )
            },
        contentAlignment = Alignment.Center,
        content = content,
    )
}
