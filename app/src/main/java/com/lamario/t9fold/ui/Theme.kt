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
    bezel: Dp = Retro.bezelWidth,
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
                .then(if (Retro.lcdGrid) Modifier.pixelGrid() else Modifier)
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
