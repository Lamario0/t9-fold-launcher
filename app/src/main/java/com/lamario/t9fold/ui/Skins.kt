package com.lamario.t9fold.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lamario.t9fold.PhoneKey

enum class EarpieceStyle { SLOT, GRILLE }

/** Everything that changes between phone styles: colours, key shapes, and a few physical details. */
class PhoneSkin(
    // Screen.
    val Lcd: Color,
    val LcdShade: Color,
    val Ink: Color,
    val lcdGrid: Boolean,
    // Body.
    val BodyLight: Color,
    val BodyDark: Color,
    val Bezel: Color,
    val BezelEdge: Color,
    /** Soft keys, rocker, and text printed on the body. */
    val Chrome: Color,
    val ChromeDark: Color,
    // Keys.
    val KeyLight: Color,
    val KeyDark: Color,
    val KeyText: Color,
    val KeySubText: Color,
    val CallLight: Color,
    val CallDark: Color,
    val CallGlyph: Color,
    val EndLight: Color,
    val EndDark: Color,
    val EndGlyph: Color,
    // Shape.
    val brand: String,
    val keyCornerPercent: Int,
    val keyGap: Dp,
    /** Share of the phone's height given to the screen. */
    val lcdWeight: Float = 0.5f,
    /** Flip phones get a hinge between the screen half and the keypad half. */
    val flip: Boolean = false,
    val earpiece: EarpieceStyle = EarpieceStyle.SLOT,
    /** Text printed on function keys instead of icons, like SND/END on the old bricks. */
    val keyLabels: Map<PhoneKey, String> = emptyMap(),
) {
    val InkFaint: Color = Ink.copy(alpha = 0.18f)
    val Body: Brush = Brush.verticalGradient(listOf(BodyLight, BodyDark))

    /** A light body needs dark status bar icons. */
    val lightBody: Boolean = BodyLight.luminance() > 0.45f

    /** Maps an icon's brightness onto the two screen colours, so app icons look printed on the screen. */
    val LcdIconFilter: ColorFilter = run {
        // Bright icon pixels take the brighter screen colour, whether that's the backlight or a red LED.
        val (dark, light) = if (Ink.luminance() < Lcd.luminance()) Ink to Lcd else Lcd to Ink
        val dr = light.red - dark.red
        val dg = light.green - dark.green
        val db = light.blue - dark.blue
        ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    0.30f * dr, 0.59f * dr, 0.11f * dr, 0f, dark.red * 255f,
                    0.30f * dg, 0.59f * dg, 0.11f * dg, 0f, dark.green * 255f,
                    0.30f * db, 0.59f * db, 0.11f * db, 0f, dark.blue * 255f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            ),
        )
    }
}

private val Candybar = PhoneSkin(
    Lcd = Color(0xFFC7F0D8), LcdShade = Color(0xFFADD9BF), Ink = Color(0xFF43523D), lcdGrid = true,
    BodyLight = Color(0xFF34465C), BodyDark = Color(0xFF141C27),
    Bezel = Color(0xFF0D1218), BezelEdge = Color(0xFF2B343F),
    Chrome = Color(0xFF9AA7B4), ChromeDark = Color(0xFF5E6B78),
    KeyLight = Color(0xFFF0F3F6), KeyDark = Color(0xFFADB7C1),
    KeyText = Color(0xFF1B2632), KeySubText = Color(0xFF52606D),
    CallLight = Color(0xFF63C871), CallDark = Color(0xFF2B7A37), CallGlyph = Color.White,
    EndLight = Color(0xFFE76A60), EndDark = Color(0xFF992A23), EndGlyph = Color.White,
    brand = "T9 · FOLD",
    keyCornerPercent = 42,
    keyGap = 12.dp,
)

/** Thin anodised-metal flip: blue colour screen, flat etched keypad glowing blue. */
private fun metalFlip(bodyLight: Color, bodyDark: Color, chrome: Color, chromeDark: Color) = PhoneSkin(
    Lcd = Color(0xFFDDEEFF), LcdShade = Color(0xFFA6C8F0), Ink = Color(0xFF0B2752), lcdGrid = false,
    BodyLight = bodyLight, BodyDark = bodyDark,
    Bezel = Color(0xFF0E1116), BezelEdge = Color(0xFF3A3F47),
    Chrome = chrome, ChromeDark = chromeDark,
    KeyLight = Color(0xFF30353D), KeyDark = Color(0xFF15181D),
    KeyText = Color(0xFF7CC6FF), KeySubText = Color(0xFF4C8FD0),
    CallLight = Color(0xFF30353D), CallDark = Color(0xFF15181D), CallGlyph = Color(0xFF5BE07A),
    EndLight = Color(0xFF30353D), EndDark = Color(0xFF15181D), EndGlyph = Color(0xFFFF5E55),
    brand = "FLIP",
    keyCornerPercent = 14,
    keyGap = 4.dp,
    flip = true,
)

private val RazrSilver = metalFlip(
    bodyLight = Color(0xFFD3D8DE), bodyDark = Color(0xFF7D848E),
    chrome = Color(0xFFE6EAEE), chromeDark = Color(0xFF4F5761),
)

private val RazrPink = metalFlip(
    bodyLight = Color(0xFFF2B5D3), bodyDark = Color(0xFFB04F7E),
    chrome = Color(0xFFF8D7E7), chromeDark = Color(0xFF6E2A4B),
)

/** Glossy pearl clamshell: bright sky-blue screen, oval silver keys with blue lettering. */
private val Clamshell = PhoneSkin(
    Lcd = Color(0xFFEAF6FF), LcdShade = Color(0xFFBCDDF8), Ink = Color(0xFF103567), lcdGrid = false,
    BodyLight = Color(0xFFEFF2F6), BodyDark = Color(0xFFA0A9B5),
    Bezel = Color(0xFF0A0C11), BezelEdge = Color(0xFF353A43),
    Chrome = Color(0xFFDCE2EA), ChromeDark = Color(0xFF59636F),
    KeyLight = Color(0xFFFAFBFC), KeyDark = Color(0xFFC1C9D3),
    KeyText = Color(0xFF1C2939), KeySubText = Color(0xFF2B76C8),
    CallLight = Color(0xFF6CCB78), CallDark = Color(0xFF2F8A3C), CallGlyph = Color.White,
    EndLight = Color(0xFFE46C63), EndDark = Color(0xFFA02E26), EndGlyph = Color.White,
    brand = "T9 FLIP",
    keyCornerPercent = 50,
    keyGap = 10.dp,
    flip = true,
)

/** 1980s brick: charcoal plastic, red LED readout, chunky rubber keys with printed function names. */
private val Brick = PhoneSkin(
    Lcd = Color(0xFF1C0604), LcdShade = Color(0xFF0C0201), Ink = Color(0xFFFF4B3A), lcdGrid = false,
    BodyLight = Color(0xFF3B3B3D), BodyDark = Color(0xFF151516),
    Bezel = Color(0xFF050505), BezelEdge = Color(0xFF2C2C2C),
    Chrome = Color(0xFFA19F9A), ChromeDark = Color(0xFF55534F),
    KeyLight = Color(0xFFDAD6CC), KeyDark = Color(0xFFA6A196),
    KeyText = Color(0xFF181818), KeySubText = Color(0xFF55524C),
    CallLight = Color(0xFFDAD6CC), CallDark = Color(0xFFA6A196), CallGlyph = Color(0xFF237A35),
    EndLight = Color(0xFFDAD6CC), EndDark = Color(0xFFA6A196), EndGlyph = Color(0xFFB3241B),
    brand = "MOBILE",
    keyCornerPercent = 12,
    keyGap = 10.dp,
    lcdWeight = 0.4f,
    earpiece = EarpieceStyle.GRILLE,
    keyLabels = mapOf(
        PhoneKey.SOFT_LEFT to "FCN",
        PhoneKey.SOFT_RIGHT to "CLR",
        PhoneKey.CALL to "SND",
        PhoneKey.END to "END",
    ),
)

enum class PhoneStyle(val label: String, val skin: PhoneSkin) {
    CANDYBAR("Candybar", Candybar),
    RAZR_SILVER("Razr silver", RazrSilver),
    RAZR_PINK("Razr pink", RazrPink),
    CLAMSHELL("Clamshell", Clamshell),
    BRICK("80s brick", Brick),
}

val LocalSkin = staticCompositionLocalOf { PhoneStyle.CANDYBAR.skin }

/** The current phone style's colours and shapes. */
val Retro: PhoneSkin
    @Composable
    @ReadOnlyComposable
    get() = LocalSkin.current
