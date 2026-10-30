package com.lamario.t9fold.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lamario.t9fold.R

object Retro {
    // The classic green monochrome LCD.
    val Lcd = Color(0xFFC7F0D8)
    val LcdShade = Color(0xFFADD9BF)
    val Ink = Color(0xFF43523D)
    val InkFaint = Color(0x2E43523D)

    // Navy plastic body.
    val BodyLight = Color(0xFF34465C)
    val BodyDark = Color(0xFF141C27)
    val Bezel = Color(0xFF0D1218)
    val BezelEdge = Color(0xFF2B343F)
    val Chrome = Color(0xFF9AA7B4)
    val ChromeDark = Color(0xFF5E6B78)

    // Keys.
    val KeyLight = Color(0xFFF0F3F6)
    val KeyDark = Color(0xFFADB7C1)
    val KeyText = Color(0xFF1B2632)
    val KeySubText = Color(0xFF52606D)
    val CallLight = Color(0xFF63C871)
    val CallDark = Color(0xFF2B7A37)
    val EndLight = Color(0xFFE76A60)
    val EndDark = Color(0xFF992A23)

    val Body: Brush = Brush.verticalGradient(listOf(BodyLight, BodyDark))

    /** Maps an icon's brightness onto the two LCD colours, so app icons look printed on the screen. */
    val LcdIconFilter: ColorFilter = run {
        fun ch(c: Float) = c * 255f
        val dr = Lcd.red - Ink.red
        val dg = Lcd.green - Ink.green
        val db = Lcd.blue - Ink.blue
        ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    0.30f * dr, 0.59f * dr, 0.11f * dr, 0f, ch(Ink.red),
                    0.30f * dg, 0.59f * dg, 0.11f * dg, 0f, ch(Ink.green),
                    0.30f * db, 0.59f * db, 0.11f * db, 0f, ch(Ink.blue),
                    0f, 0f, 0f, 1f, 0f,
                ),
            ),
        )
    }
}

val PixelFont = FontFamily(Font(R.font.vt323_regular))

/** Pixel-font text, sized in dp so a large system font size can't break the phone layout. */
@Composable
fun PixelText(
    text: String,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    color: Color = Retro.Ink,
    align: TextAlign = TextAlign.Start,
    maxLines: Int = 1,
) = PixelText(AnnotatedString(text), modifier, size, color, align, maxLines)

@Composable
fun PixelText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    color: Color = Retro.Ink,
    align: TextAlign = TextAlign.Start,
    maxLines: Int = 1,
) {
    val fontSize = with(LocalDensity.current) { size.toSp() }
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            fontFamily = PixelFont,
            fontSize = fontSize,
            lineHeight = fontSize,
            color = color,
            textAlign = align,
        ),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Plain sans text for the printing on plastic keys. */
@Composable
fun KeyText(text: String, size: Dp, color: Color, modifier: Modifier = Modifier, bold: Boolean = true) {
    val fontSize = with(LocalDensity.current) { size.toSp() }
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            fontSize = fontSize,
            lineHeight = fontSize,
            color = color,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
        ),
        maxLines = 1,
    )
}

/** A recessed LCD: dark bezel, green glass, and a faint pixel grid over the content. */
@Composable
fun LcdPanel(
    modifier: Modifier = Modifier,
    bezel: Dp = 9.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(Retro.BezelEdge, Retro.Bezel)))
            .padding(bezel),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(6.dp))
                .background(Brush.verticalGradient(listOf(Retro.Lcd, Retro.LcdShade)))
                .pixelGrid()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            content = content,
        )
    }
}

private fun Modifier.pixelGrid(): Modifier = drawWithCache {
    val step = 3.dp.toPx()
    val color = Color(0x0D000000)
    onDrawWithContent {
        drawContent()
        var x = 0f
        while (x < size.width) {
            drawLine(color, Offset(x, 0f), Offset(x, size.height), 1f)
            x += step
        }
        var y = 0f
        while (y < size.height) {
            drawLine(color, Offset(0f, y), Offset(size.width, y), 1f)
            y += step
        }
    }
}
