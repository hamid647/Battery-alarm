package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.BatteryAlarmApplication
import com.example.data.BatteryAlarmRepository
import com.example.data.database.BatteryAlarmDatabase
import com.example.data.database.ChargeSession
import com.example.data.database.SettingsEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class BatteryMonitorService : Service() {

    private val job = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + job)

    private lateinit var repository: BatteryAlarmRepository
    private lateinit var alarmService: AlarmService
    private var isReceiverRegistered = false

    private val isAlarmTriggered = AtomicBoolean(false)
    private var lastVibrationState = true
    private var lastSoundType = "digital_beep"
    private var lastCustomAlarmUri: String? = null

    // Session logging variables
    private var sessionStartPercent: Int? = null
    private var sessionStartTime: Long? = null

    // For debouncing connect/disconnect changes
    private var lastChargingState = false
    private var lastStateChangeTime = 0L
    private val DEBOUNCE_DELAY_MS = 3000L

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                handleBatteryUpdate(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("BatteryMonitorService", "onCreate")
        val db = BatteryAlarmDatabase.getDatabase(this)
        repository = BatteryAlarmRepository(db.settingsDao(), db.chargeSessionDao())
        alarmService = AlarmService.getInstance(this)

        createNotificationChannels()
        BatteryStateTracker.setMonitoringActive(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("BatteryMonitorService", "onStartCommand action=${intent?.action}")

        // Handle possible actions like "STOP_SERVICE", "STOP_ALARM", "SNOOZE_ALARM"
        when (intent?.action) {
            "ACTION_STOP_SERVICE" -> {
                stopMonitoringService()
                return START_NOT_STICKY
            }
            "ACTION_STOP_ALARM" -> {
                dismissAlarm()
            }
            "ACTION_SNOOZE_ALARM" -> {
                snoozeAlarm()
            }
            else -> {
                startForegroundWithNotification()
                registerBatteryReceiver()
            }
        }

        return START_STICKY
    }

    private fun registerBatteryReceiver() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            registerReceiver(batteryReceiver, filter)
            isReceiverRegistered = true
            Log.d("BatteryMonitorService", "Battery broadcast receiver registered")
        }
    }

    private fun startForegroundWithNotification() {
        val notification = buildMonitoringNotification(0, false)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Using specialUse foreground service type
                val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE
                }
                startForeground(NOTIFICATION_ID, notification, serviceType)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            Log.d("BatteryMonitorService", "startForeground successful")
        } catch (e: Exception) {
            Log.e("BatteryMonitorService", "Failed to startForeground due to OS restrictions", e)
        }
    }

    private fun handleBatteryUpdate(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percent = if (level >= 0 && scale > 0) (level * 100 / scale) else -1

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        processBatteryState(percent, isCharging)
    }

    private fun processBatteryState(percent: Int, isCharging: Boolean) {
        if (percent == -1) return

        val currentTime = System.currentTimeMillis()
        
        // Debounce charging status changes to prevent rapid connect/disconnect triggering
        var debouncedCharging = isCharging
        if (isCharging != lastChargingState) {
            if (currentTime - lastStateChangeTime < DEBOUNCE_DELAY_MS) {
                debouncedCharging = lastChargingState
            } else {
                lastChargingState = isCharging
                lastStateChangeTime = currentTime
            }
        }

        BatteryStateTracker.updateBatteryLevel(percent)
        BatteryStateTracker.updateChargingStatus(debouncedCharging)

        // Log session start or end
        handleSessionTracking(percent, debouncedCharging)

        // Read settings and verify constraints
        serviceScope.launch {
            val settings = repository.getSettings()
            lastVibrationState = settings.vibrationEnabled
            lastSoundType = settings.alarmSound
            lastCustomAlarmUri = settings.customAlarmUri

            val targetPercent = settings.thresholdPercent

            // Calculate ETA to target percent
            val etaMinutes = if (debouncedCharging && percent < targetPercent) {
                // Approximate 1 minute per 1% charge as default, or adapt slightly
                targetPercent - percent
            } else {
                null
            }
            BatteryStateTracker.updateEta(etaMinutes)

            // Update Foreground Service permanent notification
            updateForegroundNotification(percent, debouncedCharging, targetPercent)

            // Trigger alarm if criteria are met
            if (percent >= targetPercent && debouncedCharging) {
                if (!isAlarmTriggered.get() && !BatteryStateTracker.isAlarmFired.value) {
                    triggerAlarmAlert(percent, targetPercent)
                }
            } else {
                // Auto-cancel alarm and stop monitoring if unplugged
                if (!debouncedCharging) {
                    Log.d("BatteryMonitorService", "Charger unplugged. Stopping monitoring service automatically.")
                    stopMonitoringService()
                }
            }
        }
    }

    private fun handleSessionTracking(percent: Int, isCharging: Boolean) {
        val currentTime = System.currentTimeMillis()
        if (isCharging) {
            if (sessionStartPercent == null) {
                sessionStartPercent = percent
                sessionStartTime = currentTime
                Log.d("BatteryMonitorService", "Session started: %=$percent, time=$currentTime")
            }
        } else {
            if (sessionStartPercent != null && sessionStartTime != null) {
                val start = sessionStartPercent!!
                val startTime = sessionStartTime!!
                val duration = (currentTime - startTime) / 1000

                // Log session if it actually changed some charge level or lasted some time
                if (percent != start || duration > 5) {
                    BatteryAlarmApplication.applicationScope.launch {
                        repository.addSession(
                            ChargeSession(
                                startTime = startTime,
                                endTime = currentTime,
                                startPercent = start,
                                endPercent = percent,
                                durationSeconds = duration
                            )
                        )
                        Log.d("BatteryMonitorService", "Session logged in DB: start=$start, end=$percent, dur=$duration s")
                    }
                }
                sessionStartPercent = null
                sessionStartTime = null
            }
        }
    }

    private fun triggerAlarmAlert(currentPercent: Int, targetPercent: Int) {
        isAlarmTriggered.set(true)
        BatteryStateTracker.setAlarmFired(true)
        Log.d("BatteryMonitorService", "ALARM FIRED! Battery reached $currentPercent% (target: $targetPercent%)")

        // 1. Play looping sound + vibration
        alarmService.startAlarm(lastSoundType, lastVibrationState, lastCustomAlarmUri)

        // 2. Show high-priority heads-up notification with Full Screen Intent
        showAlarmNotification(currentPercent)

        // 3. Wake up screen and launch Activity
        wakeUpDeviceAndLaunchActivity()
    }

    private fun wakeUpDeviceAndLaunchActivity() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val isScreenOn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                powerManager.isInteractive
            } else {
                @Suppress("DEPRECATION")
                powerManager.isScreenOn
            }

            if (!isScreenOn) {
                @Suppress("DEPRECATION")
                val wakeLock = powerManager.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                    "BatteryAlarm:WakeLock"
                )
                wakeLock.acquire(10000L) // 10s
            }

            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("ALARM_TRIGGERED", true)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("BatteryMonitorService", "Failed to wake screen / start activity", e)
        }
    }

    private fun updateForegroundNotification(currentPercent: Int, isCharging: Boolean, targetPercent: Int) {
        val notification = buildMonitoringNotification(currentPercent, isCharging, targetPercent)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildMonitoringNotification(
        currentPercent: Int,
        isCharging: Boolean,
        targetPercent: Int = 90
    ): Notification {
        val stopServiceIntent = Intent(this, BatteryMonitorService::class.java).apply {
            action = "ACTION_STOP_SERVICE"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopServiceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 1, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isCharging) {
            getString(R.string.monitoring_active)
        } else {
            getString(R.string.monitoring_not_charging)
        }

        val content = if (isCharging) {
            getString(R.string.monitoring_description, targetPercent) + " ($currentPercent%)"
        } else {
            getString(R.string.monitoring_not_charging_desc)
        }

        return NotificationCompat.Builder(this, MONITORING_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Monitoring",
                stopPendingIntent
            )
            .build()
    }

    private fun showAlarmNotification(percent: Int) {
        val stopAlarmIntent = Intent(this, BatteryMonitorService::class.java).apply {
            action = "ACTION_STOP_ALARM"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopAlarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeAlarmIntent = Intent(this, BatteryMonitorService::class.java).apply {
            action = "ACTION_SNOOZE_ALARM"
        }
        val snoozePendingIntent = PendingIntent.getService(
            this, 3, snoozeAlarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("ALARM_TRIGGERED", true)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 4, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, ALARM_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("Battery Limit Reached!")
            .setContentText("Your device is at $percent% charge. Unplug now to protect battery health!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_play, "Snooze", snoozePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(ALARM_NOTIFICATION_ID, builder.build())
    }

    private fun dismissAlarm() {
        Log.d("BatteryMonitorService", "Dismissing alarm")
        alarmService.stopAlarm()
        isAlarmTriggered.set(false)
        BatteryStateTracker.setAlarmFired(false)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(ALARM_NOTIFICATION_ID)
    }

    private fun snoozeAlarm() {
        Log.d("BatteryMonitorService", "Snoozing alarm")
        dismissAlarm()

        serviceScope.launch {
            val settings = repository.getSettings()
            val snoozeMinutes = settings.snoozeDurationMinutes
            Log.d("BatteryMonitorService", "Alarm snoozed for $snoozeMinutes minutes")
            
            // Wait for snooze duration in background, then enable re-alarm
            delay(snoozeMinutes * 60 * 1000L)
            
            // Re-check: if still plugged in, trigger again!
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val currentBatteryIntent = registerReceiver(null, filter)
            if (currentBatteryIntent != null) {
                handleBatteryUpdate(currentBatteryIntent)
            }
        }
    }

    private fun stopMonitoringService() {
        Log.d("BatteryMonitorService", "Stopping monitoring service")
        dismissAlarm()
        stopSelf()
    }

    private fun saveActiveSessionOnDestroy() {
        val start = sessionStartPercent
        val startTime = sessionStartTime
        if (start != null && startTime != null) {
            val currentTime = System.currentTimeMillis()
            val duration = (currentTime - startTime) / 1000
            val currentPercent = BatteryStateTracker.batteryLevel.value
            
            if (currentPercent != start || duration > 5) {
                BatteryAlarmApplication.applicationScope.launch {
                    repository.addSession(
                        ChargeSession(
                            startTime = startTime,
                            endTime = currentTime,
                            startPercent = start,
                            endPercent = currentPercent,
                            durationSeconds = duration
                        )
                    )
                    Log.d("BatteryMonitorService", "Active session saved on destroy: start=$start, end=$currentPercent")
                }
            }
        }
        sessionStartPercent = null
        sessionStartTime = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("BatteryMonitorService", "onDestroy")
        
        // Save any active session before destroying
        saveActiveSessionOnDestroy()
        
        if (isReceiverRegistered) {
            unregisterReceiver(batteryReceiver)
            isReceiverRegistered = false
        }
        dismissAlarm()
        BatteryStateTracker.setMonitoringActive(false)
        
        // Re-schedule PowerConnectionJob Service to catch the next plug-in event
        BatteryStateTracker.schedulePowerConnectionJob(this)
        
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Channel 1: Background Monitoring Service (Low Priority)
            val monitoringChannel = NotificationChannel(
                MONITORING_CHANNEL_ID,
                getString(R.string.battery_monitor_service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.battery_monitor_service_channel_description)
                setShowBadge(false)
            }

            // Channel 2: High Priority Alarm Alert (Heads up, custom sound, vibration)
            val alarmChannel = NotificationChannel(
                ALARM_CHANNEL_ID,
                getString(R.string.battery_alarm_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.battery_alarm_channel_description)
                enableVibration(true)
                enableLights(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannel(monitoringChannel)
            notificationManager.createNotificationChannel(alarmChannel)
        }
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ALARM_NOTIFICATION_ID = 1002
        
        const val MONITORING_CHANNEL_ID = "battery_monitor_service_channel"
        const val ALARM_CHANNEL_ID = "battery_alarm_channel"
    }
}
