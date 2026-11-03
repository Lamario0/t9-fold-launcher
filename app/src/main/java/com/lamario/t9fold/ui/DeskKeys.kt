package com.lamario.t9fold.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lamario.t9fold.LauncherState
import com.lamario.t9fold.PhoneKey
import kotlinx.coroutines.delay

/** Keys per column; two columns. */
const val DESK_KEY_ROWS = 8

private val LedOn = Color(0xFF57E86B)
private val LedFlash = Color(0xFFFFB338)
private val LedSet = Color(0xFF2E6B37)
private val LedOff = Color(0xFF1C211D)
private val Paper = Color(0xFFF3EDD3)
private val PaperInk = Color(0xFF26231C)

/**
 * The right half of the unfolded screen: an office phone's programmable keys. Tap a key to use it,
 * hold it to choose what it does (the choice is made on the phone's LCD on the left).
 */
@Composable
fun DeskKeysPanel(state: LauncherState, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .background(Retro.Body)
            .swipeDownToExpand(state::expandNotifications)
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 22.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            PixelText("SPEED KEYS", Modifier.weight(1f), size = 18.dp, color = Retro.Chrome)
            PixelText("HOLD TO PROGRAM", size = 14.dp, color = Retro.ChromeDark)
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(Retro.Bezel.copy(alpha = 0.35f))
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            for (column in 0 until 2) {
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (row in 0 until DESK_KEY_ROWS) {
                        DeskKeyRow(
                            state = state,
                            index = column * DESK_KEY_ROWS + row,
                            // Buttons sit on the outer edges, labels toward the middle.
                            buttonFirst = column == 0,
                            modifier = Modifier.fillMaxWidth().weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeskKeyRow(state: LauncherState, index: Int, buttonFirst: Boolean, modifier: Modifier) {
    val label = state.deskKeyLabel(index)
    var flashing by remember { mutableStateOf(false) }
    LaunchedEffect(flashing) {
        if (flashing) {
            delay(250)
            flashing = false
        }
    }
    val led = when {
        state.deskKeyLit(index) -> LedOn
        flashing -> LedFlash
        label != null -> LedSet
        else -> LedOff
    }
    val onPress = {
        flashing = true
        state.feedback(PhoneKey.UP)
    }

    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val button = @Composable {
            PlasticKey(
                light = Retro.KeyLight,
                dark = Retro.KeyDark,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxHeight(0.62f).width(46.dp),
                onPress = onPress,
                onClick = { state.pressDeskKey(index) },
                onLongClick = { state.setUpDeskKey(index) },
            ) {}
        }
        val light = @Composable {
            Box(Modifier.size(9.dp).clip(CircleShape).background(led).border(1.dp, Color.Black.copy(alpha = 0.4f), CircleShape))
        }
        val paper = @Composable {
            PaperLabel(
                text = label,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(0.78f)
                    .pointerInput(index) {
                        detectTapGestures(
                            onPress = { onPress() },
                            onTap = { state.pressDeskKey(index) },
                            onLongPress = { state.setUpDeskKey(index) },
                        )
                    },
            )
        }
        if (buttonFirst) {
            button(); light(); paper()
        } else {
            paper(); light(); button()
        }
    }
}

/** A typed paper label under a clear plastic window. */
@Composable
private fun PaperLabel(text: String?, modifier: Modifier) {
    val fontSize = with(LocalDensity.current) { 14.dp.toSp() }
    Box(
        modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Paper)
            .border(1.dp, Color.Black.copy(alpha = 0.25f), RoundedCornerShape(3.dp))
            .drawWithContent {
                drawContent()
                // Glare on the plastic window.
                drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent)))
            }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicText(
            text = text ?: "· · ·",
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize,
                color = if (text == null) PaperInk.copy(alpha = 0.3f) else PaperInk,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
