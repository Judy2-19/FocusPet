package com.example.focuspets.util

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.focuspets.model.SettingsManager

/**
 * Focus/lock completion alerts chosen in Settings: two short vibration pulses, default notification
 * sound, custom persistently granted audio URI, or no alert.
 */
object AlertPlayer {

    /** Play the configured completion alert; these operations are lightweight and can be invoked from IO or main. */
    fun play(context: Context) {
        when (SettingsManager.getAlertMode(context)) {
            SettingsManager.ALERT_NONE -> { /* No alert.*/ }
            SettingsManager.ALERT_VIBRATE -> vibrate(context)
            SettingsManager.ALERT_SOUND_DEFAULT -> playRingtone(context, null)
            SettingsManager.ALERT_SOUND_CUSTOM -> {
                val uriStr = SettingsManager.getAlertRingtoneUri(context)
                val uri = uriStr?.let { runCatching { Uri.parse(it) }.getOrNull() }
                playRingtone(context, uri)   // Fall back to the default sound if the custom URI cannot be resolved.
            }
        }
    }

    private fun vibrate(context: Context) {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(VibratorManager::class.java)
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Vibrator::class.java)
            } ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Short-pause-short vibration pattern: noticeable but restrained.
                vibrator.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 260, 180, 260), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 260, 180, 260), -1)
            }
        }
    }

    private fun playRingtone(context: Context, uri: Uri?) {
        runCatching {
            val soundUri = uri ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, soundUri) ?: return
            ringtone.play()
        }
    }
}
