package com.lamario.t9fold.system

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.lamario.t9fold.PhoneKey
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class KeySound(val label: String) {
    TONES("Key tones"),
    CLICKS("Clicks"),
    SILENT("Silent"),
}

/** Vibration plus the keypad sound: DTMF tones on the digits, a short beep elsewhere, or a plastic click. */
class KeyFeedback(context: Context) {
    private val vibrator: Vibrator? = context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    private val tones: ToneGenerator? = runCatching { ToneGenerator(AudioManager.STREAM_SYSTEM, 70) }.getOrNull()
    private val click: AudioTrack? = runCatching { buildClick() }.getOrNull()

    fun play(key: PhoneKey, sound: KeySound, vibrate: Boolean) {
        if (vibrate) runCatching { vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)) }
        when (sound) {
            KeySound.TONES -> tones?.startTone(toneFor(key), TONE_MS)
            KeySound.CLICKS -> click?.let {
                runCatching {
                    it.stop()
                    it.reloadStaticData()
                    it.play()
                }
            }
            KeySound.SILENT -> Unit
        }
    }

    fun release() {
        tones?.release()
        click?.release()
    }

    private fun toneFor(key: PhoneKey): Int = when (val c = key.char) {
        in '0'..'9' -> ToneGenerator.TONE_DTMF_0 + (c!! - '0')
        '*' -> ToneGenerator.TONE_DTMF_S
        '#' -> ToneGenerator.TONE_DTMF_P
        else -> ToneGenerator.TONE_PROP_BEEP
    }

    /** ~12 ms of a decaying 2.4 kHz ping mixed with noise: close enough to a hard plastic key. */
    private fun buildClick(): AudioTrack {
        val rate = 44_100
        val n = rate * 12 / 1000
        val random = Random(7)
        val samples = ShortArray(n) { i ->
            val t = i.toDouble() / rate
            val s = (sin(2 * PI * 2400 * t) * 0.6 + (random.nextDouble() * 2 - 1) * 0.4) * exp(-t * 900)
            (s * Short.MAX_VALUE * 0.5).toInt().toShort()
        }
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(rate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(n * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(samples, 0, n)
        return track
    }

    private companion object {
        const val TONE_MS = 110
    }
}
