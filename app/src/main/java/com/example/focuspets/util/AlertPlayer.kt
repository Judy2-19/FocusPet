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
 * 专注 / 锁机结束时的提醒播放器。
 *
 * 提醒方式由设置决定（二者择其一）：
 * - 仅震动：短促的两段震动，不发出声音。
 * - 默认铃声：系统默认通知提示音。
 * - 自定义音频：用户上传的音频文件（持久授权的 URI）。
 * - 无：什么都不做。
 */
object AlertPlayer {

    /** 播放结束提醒（依据当前设置）。耗时操作放在调用方线程之外的 IO/主线程均可，这里都很轻量。 */
    fun play(context: Context) {
        when (SettingsManager.getAlertMode(context)) {
            SettingsManager.ALERT_NONE -> { /* 不提醒 */ }
            SettingsManager.ALERT_VIBRATE -> vibrate(context)
            SettingsManager.ALERT_SOUND_DEFAULT -> playRingtone(context, null)
            SettingsManager.ALERT_SOUND_CUSTOM -> {
                val uriStr = SettingsManager.getAlertRingtoneUri(context)
                val uri = uriStr?.let { runCatching { Uri.parse(it) }.getOrNull() }
                playRingtone(context, uri)   // 自定义 URI 解析失败则回退到系统默认音
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
                // 短-停-短 的两段震动，明确但不过分
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
