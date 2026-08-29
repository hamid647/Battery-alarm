package com.example.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

class AlarmService private constructor(private val context: Context) {

    private val attributionContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.createAttributionContext("battery_alarm")
    } else {
        context
    }

    private val audioManager = attributionContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = attributionContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        attributionContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private var currentRingtone: Ringtone? = null
    private var isPlaying = false
    private var originalVolume: Int = -1

    fun startAlarm(soundType: String, enableVibration: Boolean, customAlarmUri: String? = null) {
        if (isPlaying) return
        isPlaying = true

        Log.d("AlarmService", "Starting alarm: sound=$soundType, vibration=$enableVibration, customUri=$customAlarmUri")

        // Set volume to max for STREAM_ALARM
        try {
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
        } catch (e: Exception) {
            Log.e("AlarmService", "Failed to adjust volume", e)
        }

        // Get tone URI
        val toneUri: Uri = when (soundType) {
            "classic_alarm" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            "digital_beep" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            "chime" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            "custom" -> {
                if (!customAlarmUri.isNullOrEmpty()) {
                    try {
                        Uri.parse(customAlarmUri)
                    } catch (e: Exception) {
                        Log.e("AlarmService", "Failed to parse custom uri: $customAlarmUri, falling back", e)
                        val systemAlarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        systemAlarm ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    }
                } else {
                    val systemAlarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    systemAlarm ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                }
            }
            else -> {
                // Custom or fallback to default alarm
                val systemAlarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                systemAlarm ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }
        }

        // Play Sound
        try {
            val ringtone = RingtoneManager.getRingtone(attributionContext, toneUri)
            ringtone?.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone?.isLooping = true
            }
            ringtone?.play()
            currentRingtone = ringtone
        } catch (e: Exception) {
            Log.e("AlarmService", "Failed to play ringtone", e)
        }

        // Play Vibration
        if (enableVibration) {
            try {
                val pattern = longArrayOf(0, 500, 500, 500, 500, 500)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, 1)
                }
            } catch (e: Exception) {
                Log.e("AlarmService", "Failed to vibrate", e)
            }
        }
    }

    fun stopAlarm() {
        if (!isPlaying) return
        isPlaying = false

        Log.d("AlarmService", "Stopping alarm")

        // Stop sound
        try {
            currentRingtone?.stop()
            currentRingtone = null
        } catch (e: Exception) {
            Log.e("AlarmService", "Failed to stop ringtone", e)
        }

        // Stop vibration
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e("AlarmService", "Failed to cancel vibration", e)
        }

        // Restore original volume
        if (originalVolume != -1) {
            try {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume, 0)
            } catch (e: Exception) {
                Log.e("AlarmService", "Failed to restore volume", e)
            }
        }
    }

    fun isAlarmPlaying(): Boolean = isPlaying

    companion object {
        @Volatile
        private var INSTANCE: AlarmService? = null

        fun getInstance(context: Context): AlarmService {
            return INSTANCE ?: synchronized(this) {
                val instance = AlarmService(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
