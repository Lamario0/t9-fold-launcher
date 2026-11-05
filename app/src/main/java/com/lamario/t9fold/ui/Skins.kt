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

/** Which real phone's face to draw: decides the controls under the screen and the small details. */
enum class BodyLayout {
    /** Candybar: earpiece slots, green LCD, one big Navi key between C and the scroll key. */
    NOKIA_3310,

    /** Thin metal flip: black glass screen, etched keypad plate, round navigation pad. */
    RAZR,

    /** Rounded clamshell: framed colour screen, square pad with a blue centre key, curved key rows. */
    SAMSUNG_FLIP,

    /** 1980s brick: antenna, speaker cup, red LED readout, square keys and Rcl/Sto/Fcn function keys. */
    BRICK,
}

enum class KeyLetters { BESIDE, SUPERSCRIPT, BELOW }

/** Everything that changes between phone styles. */
class PhoneSkin(
    val layout: BodyLayout,
    // Screen.
    val Lcd: Color,
    val LcdShade: Color,
    val Ink: Color,
    val lcdGrid: Boolean,
    /** The highlighted row; colour screens use a bright bar instead of inverting. */
    val Highlight: Color = Ink,
    val HighlightInk: Color = Lcd,
    /** Colour screens put the title and soft-key labels on solid bars. */
    val HeaderBar: Color? = null,
    val HeaderInk: Color = Ink,
    val SoftBar: Color? = null,
    val SoftInk: Color = Ink,
    val Bezel: Color,
    val BezelEdge: Color,
    val bezelWidth: Dp = 9.dp,
    // Body.
    val BodyLight: Color,
    val BodyDark: Color,
    /** Text and trim printed on the body. */
    val Chrome: Color,
    val ChromeDark: Color,
    /** A dark panel the screen and keys sit in, inside the body (the brick's face). */
    val FacePlate: Color? = null,
    /** A metal plate behind the keypad (the Razr's etched plate). */
    val Plate: Color? = null,
    // Keys.
    val KeyLight: Color,
    val KeyDark: Color,
    val KeyText: Color,
    val KeySubText: Color,
    /** Navigation keys: rocker, pad, soft keys. */
    val NavLight: Color,
    val NavDark: Color,
    /** The centre/Navi key. */
    val Accent: Color,
    val AccentDark: Color,
    val CallLight: Color = NavLight,
    val CallDark: Color = NavDark,
    val CallGlyph: Color,
    val EndGlyph: Color,
    val keyCornerPercent: Int,
    val keyGap: Dp,
    /** Smile-shaped key rows: outer keys raised by this much and tilted by [keyTilt] degrees. */
    val keyCurve: Dp = 0.dp,
    val keyTilt: Float = 0f,
    val letters: KeyLetters = KeyLetters.BESIDE,
    /** Share of the phone's height given to the screen. */
    val lcdWeight: Float = 0.5f,
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

/** Navy body, silver smile under the screen, yellow-green LCD, wide oval keys. */
private val Nokia3310 = PhoneSkin(
    layout = BodyLayout.NOKIA_3310,
    Lcd = Color(0xFFC3DBA6), LcdShade = Color(0xFFA9C68B), Ink = Color(0xFF2C3A24), lcdGrid = true,
    Bezel = Color(0xFF141C29), BezelEdge = Color(0xFF34445E), bezelWidth = 10.dp,
    BodyLight = Color(0xFF314D78), BodyDark = Color(0xFF15243C),
    Chrome = Color(0xFFD3D8DF), ChromeDark = Color(0xFF7E8896),
    KeyLight = Color(0xFFF1F2F4), KeyDark = Color(0xFFB8BEC7),
    KeyText = Color(0xFF1B2B48), KeySubText = Color(0xFF4F5E77),
    NavLight = Color(0xFFDCE0E6), NavDark = Color(0xFF9AA2AE),
    Accent = Color(0xFF4A6DA3), AccentDark = Color(0xFF1F3559),
    CallGlyph = Color(0xFF2E8A3E), EndGlyph = Color(0xFFB8322A),
    keyCornerPercent = 50,
    keyGap = 12.dp,
    keyCurve = 6.dp,
    keyTilt = 6f,
    lcdWeight = 0.46f,
)

/** Anodised metal flip: black glass around a colour screen, flat keys etched into a metal plate. */
private fun razr(
    bodyLight: Color,
    bodyDark: Color,
    plate: Color,
    keyLight: Color,
    keyDark: Color,
    chrome: Color,
    chromeDark: Color,
    keyText: Color,
    theme: Color,
    themeBright: Color,
) = PhoneSkin(
    layout = BodyLayout.RAZR,
    Lcd = Color(0xFFF5F7FB), LcdShade = Color(0xFFDDE4EE), Ink = Color(0xFF14213A), lcdGrid = false,
    Highlight = themeBright, HighlightInk = Color.White,
    HeaderBar = theme, HeaderInk = Color.White,
    SoftBar = Color(0xFF151A22), SoftInk = Color.White,
    Bezel = Color(0xFF050608), BezelEdge = Color(0xFF2C3036), bezelWidth = 16.dp,
    BodyLight = bodyLight, BodyDark = bodyDark,
    Chrome = chrome, ChromeDark = chromeDark,
    Plate = plate,
    KeyLight = keyLight, KeyDark = keyDark,
    KeyText = keyText, KeySubText = keyText.copy(alpha = 0.75f),
    NavLight = keyLight, NavDark = keyDark,
    Accent = Color(0xFF6CC27A), AccentDark = Color(0xFF2E7A3C),
    CallGlyph = Color(0xFF2FA046), EndGlyph = Color(0xFFE0582E),
    keyCornerPercent = 12,
    keyGap = 3.dp,
    letters = KeyLetters.SUPERSCRIPT,
    lcdWeight = 0.47f,
)

private val RazrSilver = razr(
    bodyLight = Color(0xFFE6E9EC), bodyDark = Color(0xFFA6ACB4),
    plate = Color(0xFF8D949D),
    keyLight = Color(0xFFDDE1E6), keyDark = Color(0xFFA9AFB8),
    chrome = Color(0xFFF4F5F7), chromeDark = Color(0xFF59616B),
    keyText = Color(0xFF15181C),
    theme = Color(0xFF1C4C9C), themeBright = Color(0xFF2F6FD6),
)

private val RazrPink = razr(
    bodyLight = Color(0xFFF5BCD6), bodyDark = Color(0xFFBB5A84),
    plate = Color(0xFFA9557B),
    keyLight = Color(0xFFF6CFE0), keyDark = Color(0xFFCB8BA8),
    chrome = Color(0xFFFBE0EC), chromeDark = Color(0xFF7A2D52),
    keyText = Color(0xFF2B0F1E),
    theme = Color(0xFFB8336F), themeBright = Color(0xFFD9558E),
)

/** Silver clamshell: framed colour screen with a blue title bar, pill keys with blue printing. */
private val SamsungFlip = PhoneSkin(
    layout = BodyLayout.SAMSUNG_FLIP,
    Lcd = Color(0xFFF7FAFF), LcdShade = Color(0xFFDCE6F6), Ink = Color(0xFF12244A), lcdGrid = false,
    Highlight = Color(0xFF3C74DA), HighlightInk = Color.White,
    HeaderBar = Color(0xFF2F63C6), HeaderInk = Color.White,
    SoftBar = Color(0xFF0E1B33), SoftInk = Color.White,
    Bezel = Color(0xFF1B1F25), BezelEdge = Color(0xFFB4BAC2), bezelWidth = 7.dp,
    BodyLight = Color(0xFFE9EBEE), BodyDark = Color(0xFFA2A8B0),
    Chrome = Color(0xFFF6F7F9), ChromeDark = Color(0xFF656C77),
    KeyLight = Color(0xFFF3F4F6), KeyDark = Color(0xFFBEC3CA),
    KeyText = Color(0xFF213FA6), KeySubText = Color(0xFF2E55C2),
    NavLight = Color(0xFFE0E3E7), NavDark = Color(0xFFA0A6AF),
    Accent = Color(0xFF3557C9), AccentDark = Color(0xFF15288A),
    CallGlyph = Color(0xFF2E8A3E), EndGlyph = Color(0xFFB8322A),
    keyCornerPercent = 50,
    keyGap = 9.dp,
    keyCurve = 8.dp,
    keyTilt = 7f,
    lcdWeight = 0.45f,
)

/** Cream brick with a dark face: red LED readout, square keys, function keys with a red Snd. */
private val Brick = PhoneSkin(
    layout = BodyLayout.BRICK,
    Lcd = Color(0xFF1A0504), LcdShade = Color(0xFF0B0201), Ink = Color(0xFFFF4B3A), lcdGrid = false,
    Bezel = Color(0xFF050505), BezelEdge = Color(0xFF2A2A2A), bezelWidth = 8.dp,
    BodyLight = Color(0xFFEDE8DC), BodyDark = Color(0xFFBDB5A3),
    Chrome = Color(0xFFD7D2C6), ChromeDark = Color(0xFF8F887A),
    FacePlate = Color(0xFF2C2D30),
    KeyLight = Color(0xFFF3F1EB), KeyDark = Color(0xFFC6C2B8),
    KeyText = Color(0xFF1B1B1B), KeySubText = Color(0xFF77736A),
    NavLight = Color(0xFFF3F1EB), NavDark = Color(0xFFC6C2B8),
    Accent = Color(0xFFE2463A), AccentDark = Color(0xFFA3241B),
    CallLight = Color(0xFFE2463A), CallDark = Color(0xFFA3241B),
    CallGlyph = Color.White, EndGlyph = Color(0xFF1B1B1B),
    keyCornerPercent = 14,
    keyGap = 8.dp,
    letters = KeyLetters.BELOW,
    lcdWeight = 0.3f,
)

enum class PhoneStyle(val label: String, val skin: PhoneSkin) {
    NOKIA_3310("Nokia 3310", Nokia3310),
    RAZR_SILVER("Razr silver", RazrSilver),
    RAZR_PINK("Razr pink", RazrPink),
    CLAMSHELL("Samsung flip", SamsungFlip),
    BRICK("Motorola brick", Brick),
}

val LocalSkin = staticCompositionLocalOf { PhoneStyle.NOKIA_3310.skin }

/** The current phone style's colours and shapes. */
val Retro: PhoneSkin
    @Composable
    @ReadOnlyComposable
    get() = LocalSkin.current
