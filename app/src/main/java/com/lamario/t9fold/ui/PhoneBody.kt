package com.lamario.t9fold.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lamario.t9fold.LauncherState
import com.lamario.t9fold.PhoneKey
import com.lamario.t9fold.search.T9
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

/**
 * The phone: earpiece, screen, navigation keys, keypad, drawn after the selected phone style.
 * [mirrored] flips the left/right placement of every key, as if the faceplate were lying face down.
 */
@Composable
fun PhoneBody(state: LauncherState, mirrored: Boolean, modifier: Modifier = Modifier) {
    val skin = Retro
    Column(
        modifier
            .fillMaxSize()
            .background(skin.Body)
            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 22.dp),
    ) {
        val face = skin.FacePlate
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .then(
                    if (face != null) {
                        Modifier
                            .clip(RoundedCornerShape(26.dp))
                            .background(Brush.verticalGradient(listOf(face, lerp(face, Color.Black, 0.35f))))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    } else {
                        Modifier
                    },
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TopDetails(skin.layout)
            Spacer(Modifier.height(6.dp))
            LcdPanel(Modifier.fillMaxWidth().weight(skin.lcdWeight)) {
                LcdContent(state, mirrored)
            }
            when (skin.layout) {
                BodyLayout.RAZR -> RazrHinge()
                BodyLayout.SAMSUNG_FLIP -> SamsungHinge()
                else -> Spacer(Modifier.height(12.dp))
            }
            val plate = skin.Plate
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f - skin.lcdWeight)
                    .then(
                        if (plate != null) {
                            Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.verticalGradient(listOf(lerp(plate, Color.White, 0.25f), plate)))
                                .padding(8.dp)
                        } else {
                            Modifier
                        },
                    )
                    .swipeDownToExpand(state::expandNotifications),
            ) {
                when (skin.layout) {
                    BodyLayout.NOKIA_3310 -> {
                        NokiaNav(state, mirrored, Modifier.fillMaxWidth().weight(0.27f))
                        Spacer(Modifier.height(10.dp))
                        Keypad(state, mirrored, Modifier.fillMaxWidth().weight(0.73f))
                    }
                    BodyLayout.RAZR -> {
                        RazrNav(state, mirrored, Modifier.fillMaxWidth().weight(0.34f))
                        Spacer(Modifier.height(6.dp))
                        Keypad(state, mirrored, Modifier.fillMaxWidth().weight(0.66f))
                    }
                    BodyLayout.SAMSUNG_FLIP -> {
                        SamsungNav(state, mirrored, Modifier.fillMaxWidth().weight(0.34f))
                        Spacer(Modifier.height(10.dp))
                        Keypad(state, mirrored, Modifier.fillMaxWidth().weight(0.66f))
                    }
                    BodyLayout.BRICK -> {
                        Keypad(state, mirrored, Modifier.fillMaxWidth().weight(0.56f))
                        Spacer(Modifier.height(12.dp))
                        BrickFunctionKeys(state, mirrored, Modifier.fillMaxWidth().weight(0.44f))
                    }
                }
            }
            BottomDetails(skin.layout)
        }
    }
}

// ---- Body details ------------------------------------------------------------------------------

@Composable
private fun TopDetails(layout: BodyLayout) {
    val skin = Retro
    when (layout) {
        // A short stack of earpiece slots.
        BodyLayout.NOKIA_3310 -> Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(4) { Slot(18.dp, 3.dp, skin.Bezel) }
        }
        // Slots either side of a round emblem, screws in the corners.
        BodyLayout.RAZR -> Row(Modifier.fillMaxWidth().height(22.dp), verticalAlignment = Alignment.CenterVertically) {
            Screw()
            Spacer(Modifier.weight(1f))
            repeat(3) {
                Slot(16.dp, 4.dp, skin.Bezel)
                Spacer(Modifier.width(4.dp))
            }
            Box(Modifier.size(16.dp).border(1.5.dp, skin.ChromeDark, CircleShape))
            repeat(3) {
                Spacer(Modifier.width(4.dp))
                Slot(16.dp, 4.dp, skin.Bezel)
            }
            Spacer(Modifier.weight(1f))
            Screw()
        }
        // An oval earpiece with a slot.
        BodyLayout.SAMSUNG_FLIP -> Box(
            Modifier
                .width(78.dp)
                .height(16.dp)
                .border(2.dp, skin.ChromeDark, RoundedCornerShape(50)),
            contentAlignment = Alignment.Center,
        ) {
            Slot(40.dp, 4.dp, skin.Bezel)
        }
        // Antenna, speaker cup, and the BATT / ROAM lights.
        BodyLayout.BRICK -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .width(14.dp)
                    .height(22.dp)
                    .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF0E0E0E), Color(0xFF3C3C3C), Color(0xFF0E0E0E)))),
            )
            SpeakerCup(Modifier.width(120.dp).height(44.dp))
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Led(Color(0xFF4BE36A))
                Spacer(Modifier.width(4.dp))
                PixelText("BATT", size = 13.dp, color = skin.Chrome)
                Spacer(Modifier.width(14.dp))
                Led(Color(0xFFFF4B3A))
                Spacer(Modifier.width(4.dp))
                PixelText("ROAM", size = 13.dp, color = skin.Chrome)
            }
        }
    }
}

@Composable
private fun BottomDetails(layout: BodyLayout) {
    val skin = Retro
    when (layout) {
        // Speaker grille in the chin.
        BodyLayout.RAZR -> Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(8) { Slot(3.dp, 14.dp, skin.Bezel.copy(alpha = 0.7f)) }
        }
        BodyLayout.SAMSUNG_FLIP -> Row(Modifier.fillMaxWidth().padding(top = 6.dp, start = 10.dp, end = 10.dp)) {
            Screw()
            Spacer(Modifier.weight(1f))
            Screw()
        }
        // Microphone slot.
        BodyLayout.BRICK -> Box(Modifier.padding(top = 8.dp)) { Slot(36.dp, 5.dp, Color.Black) }
        BodyLayout.NOKIA_3310 -> Unit
    }
}

@Composable
private fun Slot(width: Dp, height: Dp, color: Color) {
    Box(Modifier.width(width).height(height).clip(RoundedCornerShape(height / 2)).background(color))
}

@Composable
private fun Screw() {
    val skin = Retro
    Box(
        Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(skin.Chrome, skin.ChromeDark)))
            .border(1.dp, Color.Black.copy(alpha = 0.3f), CircleShape),
    )
}

@Composable
private fun Led(color: Color) {
    Box(Modifier.size(7.dp).clip(CircleShape).background(color).border(1.dp, Color.Black.copy(alpha = 0.5f), CircleShape))
}

/** The brick's earpiece: a dark cup with a grid of holes. */
@Composable
private fun SpeakerCup(modifier: Modifier) {
    Canvas(
        modifier
            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 30.dp, bottomEnd = 30.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF2E2E30), Color(0xFF141415)))),
    ) {
        val step = 7.dp.toPx()
        val r = 1.4.dp.toPx()
        val cx = size.width / 2
        val cy = size.height / 2
        var y = cy - step * 2
        while (y <= cy + step * 2) {
            var x = cx - step * 4
            while (x <= cx + step * 4) {
                drawCircle(Color.Black, r, Offset(x, y))
                x += step
            }
            y += step
        }
    }
}

/** The Razr's wide hinge bar with its centre notch. */
@Composable
private fun RazrHinge() {
    val skin = Retro
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .height(24.dp)
            .shadow(3.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.verticalGradient(listOf(skin.Chrome, skin.BodyDark, skin.Chrome))),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.width(70.dp).height(7.dp).clip(RoundedCornerShape(4.dp)).background(skin.BodyDark.copy(alpha = 0.7f)))
    }
}

/** The clamshell's barrel hinge with a round bumper at each end. */
@Composable
private fun SamsungHinge() {
    val skin = Retro
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(26.dp)) {
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.8f)
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Brush.verticalGradient(listOf(skin.BodyDark, skin.Chrome, skin.BodyDark))),
        )
        for (align in listOf(Alignment.CenterStart, Alignment.CenterEnd)) {
            Box(
                Modifier
                    .align(align)
                    .padding(horizontal = 8.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(skin.Chrome, skin.ChromeDark)))
                    .border(1.dp, Color.Black.copy(alpha = 0.25f), CircleShape),
            )
        }
    }
}

// ---- Navigation clusters -----------------------------------------------------------------------

/** C key, the big Navi key on a silver smile, and the up/down scroll key. */
@Composable
private fun NokiaNav(state: LauncherState, mirrored: Boolean, modifier: Modifier) {
    val skin = Retro
    val smileTop = skin.Chrome
    val smileBottom = skin.ChromeDark
    Box(modifier) {
        Canvas(Modifier.matchParentSize()) {
            drawArc(
                brush = Brush.verticalGradient(listOf(smileTop, smileBottom)),
                startAngle = 15f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(-size.width * 0.06f, -size.height * 1.15f),
                size = Size(size.width * 1.12f, size.height * 1.95f),
                style = Stroke(size.height * 0.3f, cap = StrokeCap.Round),
            )
        }
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            val clear = @Composable {
                RetroKey(
                    state, PhoneKey.SOFT_RIGHT, skin.NavLight, skin.NavDark, RoundedCornerShape(50),
                    Modifier.weight(0.75f).fillMaxHeight(0.4f),
                    onLongClick = { state.onLongKey(PhoneKey.SOFT_RIGHT) },
                ) { KeyText("C", 16.dp, skin.KeyText) }
            }
            val navi = @Composable {
                RetroKey(
                    state, PhoneKey.NAVI, skin.Accent, skin.AccentDark, RoundedCornerShape(50),
                    Modifier.weight(1.5f).fillMaxHeight(0.52f),
                    onLongClick = { state.onLongKey(PhoneKey.NAVI) },
                ) {
                    Box(Modifier.width(30.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.35f)))
                }
            }
            val scroll = @Composable {
                Column(Modifier.weight(0.75f).fillMaxHeight(0.62f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    RetroKey(
                        state, PhoneKey.UP, skin.NavLight, skin.NavDark,
                        RoundedCornerShape(topStartPercent = 50, topEndPercent = 50, bottomStartPercent = 15, bottomEndPercent = 15),
                        Modifier.fillMaxWidth().weight(1f),
                    ) { Arrow(up = true) }
                    RetroKey(
                        state, PhoneKey.DOWN, skin.NavLight, skin.NavDark,
                        RoundedCornerShape(topStartPercent = 15, topEndPercent = 15, bottomStartPercent = 50, bottomEndPercent = 50),
                        Modifier.fillMaxWidth().weight(1f),
                    ) { Arrow(up = false) }
                }
            }
            val gap = @Composable { Spacer(Modifier.width(12.dp)) }
            if (mirrored) {
                scroll(); gap(); navi(); gap(); clear()
            } else {
                clear(); gap(); navi(); gap(); scroll()
            }
        }
    }
}

/** Two soft keys and a menu key, then web and send keys, the round pad, and message and end keys. */
@Composable
private fun RazrNav(state: LauncherState, mirrored: Boolean, modifier: Modifier) {
    val skin = Retro
    val flat = RoundedCornerShape(6.dp)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth().weight(0.24f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val soft = @Composable { key: PhoneKey ->
                RetroKey(
                    state, key, skin.KeyLight, skin.KeyDark, flat, Modifier.weight(1f).fillMaxHeight(),
                    onLongClick = if (key == PhoneKey.SOFT_RIGHT) ({ state.onLongKey(key) }) else null,
                ) { Box(Modifier.size(5.dp).clip(CircleShape).background(skin.KeyText.copy(alpha = 0.6f))) }
            }
            soft(if (mirrored) PhoneKey.SOFT_RIGHT else PhoneKey.SOFT_LEFT)
            RetroKey(state, PhoneKey.MENU, skin.KeyLight, skin.KeyDark, flat, Modifier.weight(0.8f).fillMaxHeight()) {
                Box(Modifier.width(16.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(skin.Accent))
            }
            soft(if (mirrored) PhoneKey.SOFT_LEFT else PhoneKey.SOFT_RIGHT)
        }
        Row(
            Modifier.fillMaxWidth().weight(0.76f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val left = @Composable {
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    RetroKey(state, PhoneKey.APPS, skin.KeyLight, skin.KeyDark, flat, Modifier.fillMaxWidth().weight(1f)) {
                        GlobeGlyph(skin.KeyText)
                    }
                    RetroKey(state, PhoneKey.CALL, skin.KeyLight, skin.KeyDark, flat, Modifier.fillMaxWidth().weight(1f)) {
                        Handset(hungUp = false, color = skin.CallGlyph, modifier = Modifier.size(24.dp))
                    }
                }
            }
            val right = @Composable {
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    RetroKey(state, PhoneKey.RECENTS, skin.KeyLight, skin.KeyDark, flat, Modifier.fillMaxWidth().weight(1f)) {
                        EnvelopeGlyph(skin.KeyText)
                    }
                    RetroKey(state, PhoneKey.END, skin.KeyLight, skin.KeyDark, flat, Modifier.fillMaxWidth().weight(1f)) {
                        Handset(hungUp = true, color = skin.EndGlyph, modifier = Modifier.size(24.dp))
                    }
                }
            }
            if (mirrored) right() else left()
            NavPad(state, CircleShape, Modifier.weight(1.15f).aspectRatio(1f, matchHeightConstraintsFirst = true))
            if (mirrored) left() else right()
        }
    }
}

/** Soft keys either side of a square pad with a blue centre key; then send, C, and end. */
@Composable
private fun SamsungNav(state: LauncherState, mirrored: Boolean, modifier: Modifier) {
    val skin = Retro
    val pill = RoundedCornerShape(50)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier.fillMaxWidth().weight(0.62f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val soft = @Composable { key: PhoneKey ->
                RetroKey(
                    state, key, skin.NavLight, skin.NavDark, RoundedCornerShape(40), Modifier.weight(0.7f).fillMaxHeight(0.8f),
                    onLongClick = if (key == PhoneKey.SOFT_RIGHT) ({ state.onLongKey(key) }) else null,
                ) { ThreeDots(skin.KeySubText) }
            }
            soft(if (mirrored) PhoneKey.SOFT_RIGHT else PhoneKey.SOFT_LEFT)
            NavPad(state, RoundedCornerShape(28), Modifier.weight(1f).aspectRatio(1.15f, matchHeightConstraintsFirst = true))
            soft(if (mirrored) PhoneKey.SOFT_LEFT else PhoneKey.SOFT_RIGHT)
        }
        Row(Modifier.fillMaxWidth().weight(0.38f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val send = @Composable {
                RetroKey(state, PhoneKey.CALL, skin.NavLight, skin.NavDark, pill, Modifier.weight(1f).fillMaxHeight()) {
                    Handset(hungUp = false, color = skin.CallGlyph, modifier = Modifier.size(22.dp))
                }
            }
            val end = @Composable {
                RetroKey(state, PhoneKey.END, skin.NavLight, skin.NavDark, pill, Modifier.weight(1f).fillMaxHeight()) {
                    Handset(hungUp = true, color = skin.EndGlyph, modifier = Modifier.size(22.dp))
                }
            }
            if (mirrored) end() else send()
            RetroKey(
                state, PhoneKey.SOFT_RIGHT, skin.NavLight, skin.NavDark, pill, Modifier.weight(0.8f).fillMaxHeight(),
                onLongClick = { state.onLongKey(PhoneKey.SOFT_RIGHT) },
            ) { KeyText("C", 16.dp, skin.KeyText) }
            if (mirrored) send() else end()
        }
    }
}

/** A navigation pad: press the top or bottom half to scroll, the centre to select. */
@Composable
private fun NavPad(state: LauncherState, shape: Shape, modifier: Modifier) {
    val skin = Retro
    val round = shape == CircleShape
    Box(
        modifier
            .shadow(4.dp, shape)
            .clip(shape)
            .background(Brush.radialGradient(listOf(skin.NavLight, skin.NavDark)))
            .border(1.dp, Color.Black.copy(alpha = 0.25f), shape),
    ) {
        Column(Modifier.fillMaxSize()) {
            KeyArea(state, PhoneKey.UP, Modifier.fillMaxWidth().weight(1f)) {
                Box(Modifier.align(Alignment.TopCenter).padding(top = 6.dp)) { Arrow(up = true) }
            }
            KeyArea(state, PhoneKey.DOWN, Modifier.fillMaxWidth().weight(1f)) {
                Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)) { Arrow(up = false) }
            }
        }
        // Accent marks at the four edges, like the clamshell's blue ticks.
        if (!round) {
            Box(Modifier.align(Alignment.CenterStart).padding(start = 5.dp).width(3.dp).height(14.dp).background(skin.Accent))
            Box(Modifier.align(Alignment.CenterEnd).padding(end = 5.dp).width(3.dp).height(14.dp).background(skin.Accent))
        }
        RetroKey(
            state, PhoneKey.NAVI,
            light = if (round) skin.NavLight else skin.Accent,
            dark = if (round) skin.NavDark else skin.AccentDark,
            shape = if (round) CircleShape else RoundedCornerShape(35),
            modifier = Modifier.align(Alignment.Center).fillMaxSize(0.42f),
            onLongClick = { state.onLongKey(PhoneKey.NAVI) },
        ) {
            if (round) {
                Box(Modifier.fillMaxSize(0.62f).border(2.5.dp, skin.Accent, CircleShape))
            } else {
                KeyText("i", 18.dp, Color.White)
            }
        }
    }
}

private class BrickKey(val label: String, val key: PhoneKey, val red: Boolean = false)

/** The brick's function keys, with the scroll keys in the middle column. */
private val BRICK_FUNCTION_ROWS = listOf(
    listOf(BrickKey("Clr", PhoneKey.SOFT_RIGHT), BrickKey("Menu", PhoneKey.MENU), BrickKey("Snd", PhoneKey.CALL, red = true)),
    listOf(BrickKey("Vol", PhoneKey.VOLUME), BrickKey("▲", PhoneKey.UP), BrickKey("Fcn", PhoneKey.SOFT_LEFT)),
    listOf(BrickKey("Rcl", PhoneKey.RECENTS), BrickKey("▼", PhoneKey.DOWN), BrickKey("Notif", PhoneKey.NOTIFICATIONS)),
)

@Composable
private fun BrickFunctionKeys(state: LauncherState, mirrored: Boolean, modifier: Modifier) {
    val skin = Retro
    val gap = skin.keyGap
    Column(modifier, verticalArrangement = Arrangement.spacedBy(gap)) {
        for (row in BRICK_FUNCTION_ROWS) {
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(gap * 1.5f)) {
                for (k in if (mirrored) row.reversed() else row) {
                    RetroKey(
                        state, k.key,
                        light = if (k.red) skin.CallLight else skin.KeyLight,
                        dark = if (k.red) skin.CallDark else skin.KeyDark,
                        shape = RoundedCornerShape(percent = skin.keyCornerPercent),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onLongClick = if (k.key == PhoneKey.SOFT_RIGHT) ({ state.onLongKey(k.key) }) else null,
                    ) {
                        val arrow = k.key == PhoneKey.UP || k.key == PhoneKey.DOWN
                        KeyText(
                            k.label,
                            if (arrow) 20.dp else 15.dp,
                            if (k.red) skin.CallGlyph else skin.KeyText,
                            bold = arrow,
                        )
                    }
                }
            }
        }
    }
}

// ---- Glyphs ------------------------------------------------------------------------------------

@Composable
private fun Arrow(up: Boolean) {
    val color = Retro.KeyText.copy(alpha = 0.7f)
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

@Composable
private fun GlobeGlyph(color: Color) {
    Canvas(Modifier.size(20.dp)) {
        val s = Stroke(1.6.dp.toPx())
        drawCircle(color, size.minDimension / 2 - s.width, style = s)
        drawOval(color, Offset(size.width * 0.3f, s.width), Size(size.width * 0.4f, size.height - s.width * 2), style = s)
        drawLine(color, Offset(s.width, size.height / 2), Offset(size.width - s.width, size.height / 2), s.width)
    }
}

@Composable
private fun EnvelopeGlyph(color: Color) {
    Canvas(Modifier.size(22.dp, 16.dp)) {
        val w = 1.6.dp.toPx()
        drawRect(color, Offset(w / 2, w / 2), Size(size.width - w, size.height - w), style = Stroke(w))
        drawLine(color, Offset(w, w), Offset(size.width / 2, size.height * 0.6f), w)
        drawLine(color, Offset(size.width - w, w), Offset(size.width / 2, size.height * 0.6f), w)
    }
}

@Composable
private fun ThreeDots(color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(3) { Box(Modifier.size(4.dp).clip(CircleShape).background(color)) }
    }
}

// ---- Keypad ------------------------------------------------------------------------------------

private val KEY_ROWS = listOf("123", "456", "789", "*0#")

@Composable
private fun Keypad(state: LauncherState, mirrored: Boolean, modifier: Modifier) {
    val skin = Retro
    val gap = skin.keyGap
    Column(modifier, verticalArrangement = Arrangement.spacedBy(gap * 0.75f)) {
        for (row in KEY_ROWS) {
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(gap)) {
                (if (mirrored) row.reversed() else row).forEachIndexed { i, c ->
                    // -1, 0, 1 from left to right: outer keys rise and tilt into a smile.
                    val pos = i - 1
                    DigitKey(
                        state,
                        c,
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .graphicsLayer {
                                translationY = -abs(pos) * skin.keyCurve.toPx()
                                rotationZ = -pos * skin.keyTilt
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun DigitKey(state: LauncherState, c: Char, modifier: Modifier) {
    val skin = Retro
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
        light = skin.KeyLight,
        dark = skin.KeyDark,
        shape = RoundedCornerShape(percent = skin.keyCornerPercent),
        modifier = modifier,
        onLongClick = if (isFavoriteKey) ({ state.onLongKey(key) }) else null,
    ) {
        when (skin.letters) {
            KeyLetters.BESIDE -> Row(verticalAlignment = Alignment.CenterVertically) {
                KeyText(c.toString(), size = 22.dp, color = skin.KeyText)
                Spacer(Modifier.width(6.dp))
                KeyText(sub, size = 10.dp, color = skin.KeySubText, bold = false)
            }
            KeyLetters.SUPERSCRIPT -> Row {
                KeyText(c.toString(), size = 22.dp, color = skin.KeyText)
                Spacer(Modifier.width(2.dp))
                KeyText(sub, size = 9.dp, color = skin.KeySubText, bold = false)
            }
            KeyLetters.BELOW -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                KeyText(c.toString(), size = 20.dp, color = skin.KeyText)
                KeyText(sub, size = 8.dp, color = skin.KeySubText, bold = false)
            }
        }
    }
}

// ---- Keys --------------------------------------------------------------------------------------

/**
 * A phone key. Sound/vibration fire on touch-down like a real keypad; the action fires on release.
 * Up/down act on touch-down instead and keep going, faster and faster, while held.
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
    PlasticFace(light, dark, shape, pressed, modifier.phoneKeyInput(state, key, onLongClick) { pressed = it }, content)
}

/** A plastic button that sinks while held. */
@Composable
fun PlasticKey(
    light: Color,
    dark: Color,
    shape: Shape,
    modifier: Modifier = Modifier,
    onPress: () -> Unit,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    PlasticFace(light, dark, shape, pressed, modifier.keyInput(onPress, onClick, onLongClick) { pressed = it }, content)
}

@Composable
private fun PlasticFace(
    light: Color,
    dark: Color,
    shape: Shape,
    pressed: Boolean,
    modifier: Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val elevation: Dp = if (pressed) 1.dp else 4.dp
    Box(
        modifier
            .graphicsLayer { translationY = if (pressed) 2.dp.toPx() else 0f }
            .shadow(elevation, shape)
            .clip(shape)
            .background(Brush.verticalGradient(if (pressed) listOf(dark, light) else listOf(light, dark)))
            .border(1.dp, Color.Black.copy(alpha = 0.22f), shape),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** An invisible press area inside a larger control (the halves of a navigation pad). */
@Composable
private fun KeyArea(state: LauncherState, key: PhoneKey, modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier
            .phoneKeyInput(state, key, null) { pressed = it }
            .background(if (pressed) Color.Black.copy(alpha = 0.12f) else Color.Transparent),
        content = content,
    )
}

/** Wires a phone key to the launcher, with hold-to-repeat on up/down. */
@Composable
private fun Modifier.phoneKeyInput(
    state: LauncherState,
    key: PhoneKey,
    onLongClick: (() -> Unit)?,
    onPressedChange: (Boolean) -> Unit,
): Modifier {
    val step = when (key) {
        PhoneKey.UP -> -1
        PhoneKey.DOWN -> 1
        else -> 0
    }
    return keyInput(
        onPress = {
            state.feedback(key)
            if (step != 0) state.onKey(key)
        },
        onClick = { if (step == 0) state.onKey(key) },
        onLongClick = onLongClick,
        onRepeat = if (step != 0) ({ state.scrollRepeat(step) }) else null,
        onPressedChange = onPressedChange,
    )
}

/**
 * Press handling shared by every key. With [onRepeat], holding the key repeats it after a short
 * pause, speeding up the longer it's held; tap and long-press are then ignored.
 */
@Composable
private fun Modifier.keyInput(
    onPress: () -> Unit,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    onRepeat: (() -> Unit)? = null,
    onPressedChange: (Boolean) -> Unit,
): Modifier {
    val scope = rememberCoroutineScope()
    val press by rememberUpdatedState(onPress)
    val click by rememberUpdatedState(onClick)
    val longClick by rememberUpdatedState(onLongClick)
    val repeat by rememberUpdatedState(onRepeat)
    val pressedChange by rememberUpdatedState(onPressedChange)
    return pointerInput(Unit) {
        detectTapGestures(
            onPress = {
                pressedChange(true)
                press()
                var repeating: Job? = null
                repeat?.let { step ->
                    repeating = scope.launch {
                        delay(REPEAT_START_MS)
                        var interval = REPEAT_FIRST_INTERVAL_MS
                        while (true) {
                            step()
                            delay(interval)
                            interval = max(REPEAT_FASTEST_INTERVAL_MS, (interval * REPEAT_SPEEDUP).toLong())
                        }
                    }
                }
                tryAwaitRelease()
                repeating?.cancel()
                pressedChange(false)
            },
            onLongPress = { if (repeat == null) longClick?.invoke() ?: click() },
            onTap = { if (repeat == null) click() },
        )
    }
}

private const val REPEAT_START_MS = 380L
private const val REPEAT_FIRST_INTERVAL_MS = 150L
private const val REPEAT_FASTEST_INTERVAL_MS = 35L
private const val REPEAT_SPEEDUP = 0.88f
