package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BatteryAlarmRepository
import com.example.data.database.BatteryAlarmDatabase
import com.example.data.database.ChargeSession
import com.example.data.database.SettingsEntity
import com.example.service.BatteryMonitorService
import com.example.service.BatteryStateTracker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class BatteryAlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context = application.applicationContext
    private val repository: BatteryAlarmRepository

    val settings: StateFlow<SettingsEntity>
    val chargeSessions: StateFlow<List<ChargeSession>>

    // Expose battery states from tracking singleton
    val batteryLevel: StateFlow<Int> = BatteryStateTracker.batteryLevel
    val isCharging: StateFlow<Boolean> = BatteryStateTracker.isCharging
    val isAlarmFired: StateFlow<Boolean> = BatteryStateTracker.isAlarmFired
    val etaMinutes: StateFlow<Int?> = BatteryStateTracker.etaMinutes
    val isMonitoringActive: StateFlow<Boolean> = BatteryStateTracker.isMonitoringActive

    init {
        // Sync real current battery level and charging status immediately on view model initialization
        BatteryStateTracker.initialize(context)

        val db = BatteryAlarmDatabase.getDatabase(context)
        repository = BatteryAlarmRepository(db.settingsDao(), db.chargeSessionDao())

        settings = repository.settingsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsEntity()
        )

        chargeSessions = repository.allSessions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun updateThreshold(percent: Int) {
        viewModelScope.launch {
            val current = settings.value
            repository.saveSettings(current.copy(thresholdPercent = percent))
            // If service is running, restart it to apply new threshold
            if (isMonitoringActive.value) {
                startMonitoringService()
            }
        }
    }

    fun updateAlarmSound(soundType: String) {
        viewModelScope.launch {
            val current = settings.value
            repository.saveSettings(current.copy(alarmSound = soundType))
        }
    }

    fun setCustomAlarmSound(uri: Uri) {
        viewModelScope.launch {
            try {
                val contentResolver = context.contentResolver
                var fileName = "custom_alarm_tone.mp3"
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        val displayName = cursor.getString(nameIndex)
                        if (!displayName.isNullOrEmpty()) {
                            fileName = displayName
                        }
                    }
                }

                // Copy to app local files directory to ensure background service accessibility
                val destFile = File(context.filesDir, "custom_sound_${System.currentTimeMillis()}_$fileName")
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(destFile).use { output ->
                        inputStream.copyTo(output)
                    }
                }

                val savedUri = Uri.fromFile(destFile).toString()
                val current = settings.value
                repository.saveSettings(current.copy(
                    alarmSound = "custom",
                    customAlarmUri = savedUri,
                    customAlarmName = fileName
                ))
            } catch (e: Exception) {
                Log.e("BatteryAlarmViewModel", "Failed to set custom alarm sound", e)
            }
        }
    }

    fun updateVibration(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            repository.saveSettings(current.copy(vibrationEnabled = enabled))
        }
    }

    fun updateSnoozeDuration(minutes: Int) {
        viewModelScope.launch {
            val current = settings.value
            repository.saveSettings(current.copy(snoozeDurationMinutes = minutes))
        }
    }

    fun updateOverchargeProtection(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            repository.saveSettings(current.copy(overchargeProtectionEnabled = enabled))
        }
    }

    fun startMonitoringService() {
        Log.d("BatteryAlarmViewModel", "Requesting to start BatteryMonitorService")
        val intent = Intent(context, BatteryMonitorService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.e("BatteryAlarmViewModel", "Failed to start monitoring service", e)
        }
    }

    fun stopMonitoringService() {
        Log.d("BatteryAlarmViewModel", "Requesting to stop BatteryMonitorService")
        val intent = Intent(context, BatteryMonitorService::class.java).apply {
            action = "ACTION_STOP_SERVICE"
        }
        context.startService(intent)
    }

    fun dismissActiveAlarm() {
        Log.d("BatteryAlarmViewModel", "Requesting to dismiss active alarm")
        val intent = Intent(context, BatteryMonitorService::class.java).apply {
            action = "ACTION_STOP_ALARM"
        }
        context.startService(intent)
    }

    fun snoozeActiveAlarm() {
        Log.d("BatteryAlarmViewModel", "Requesting to snooze active alarm")
        val intent = Intent(context, BatteryMonitorService::class.java).apply {
            action = "ACTION_SNOOZE_ALARM"
        }
        context.startService(intent)
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearSessions()
        }
    }
}
