package com.example.service

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object BatteryStateTracker {
    private val _batteryLevel = MutableStateFlow(0)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _isCharging = MutableStateFlow(false)
    val isCharging: StateFlow<Boolean> = _isCharging.asStateFlow()

    private val _isAlarmFired = MutableStateFlow(false)
    val isAlarmFired: StateFlow<Boolean> = _isAlarmFired.asStateFlow()

    private val _etaMinutes = MutableStateFlow<Int?>(null)
    val etaMinutes: StateFlow<Int?> = _etaMinutes.asStateFlow()

    private val _isMonitoringActive = MutableStateFlow(false)
    val isMonitoringActive: StateFlow<Boolean> = _isMonitoringActive.asStateFlow()

    fun schedulePowerConnectionJob(context: Context) {
        try {
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val componentName = ComponentName(context, PowerConnectionJobService::class.java)
            
            val jobInfo = JobInfo.Builder(1001, componentName)
                .setRequiresCharging(true)
                .setPersisted(true) // Persists across device reboots
                .build()
                
            val result = jobScheduler.schedule(jobInfo)
            if (result == JobScheduler.RESULT_SUCCESS) {
                Log.d("BatteryStateTracker", "PowerConnectionJobService scheduled successfully")
            } else {
                Log.e("BatteryStateTracker", "PowerConnectionJobService scheduling failed")
            }
        } catch (e: Exception) {
            Log.e("BatteryStateTracker", "Failed to schedule PowerConnectionJobService", e)
        }
    }

    fun startServiceSafely(context: Context) {
        val serviceIntent = Intent(context, BatteryMonitorService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            Log.d("BatteryStateTracker", "BatteryMonitorService started directly")
        } catch (e: Exception) {
            Log.e("BatteryStateTracker", "Failed to start BatteryMonitorService from background: ${e.message}", e)
        }
    }

    fun initialize(context: Context) {
        try {
            // 1. Try BatteryManager first (API 21+)
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val levelProp = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            if (levelProp != null && levelProp > 0) {
                updateBatteryLevel(levelProp)
            }

            // 2. Also query from ACTION_BATTERY_CHANGED sticky broadcast to get precise level and charging status
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, intentFilter)
            if (batteryStatus != null) {
                val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val percent = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
                if (percent != -1) {
                    updateBatteryLevel(percent)
                }

                val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                updateChargingStatus(isCharging)
            }
            Log.d("BatteryStateTracker", "Initialized: level=${batteryLevel.value}%, charging=${isCharging.value}")
        } catch (e: Exception) {
            Log.e("BatteryStateTracker", "Error initializing battery status", e)
        }
    }

    fun updateBatteryLevel(level: Int) {
        _batteryLevel.value = level
    }

    fun updateChargingStatus(charging: Boolean) {
        _isCharging.value = charging
    }

    fun setAlarmFired(fired: Boolean) {
        _isAlarmFired.value = fired
    }

    fun updateEta(minutes: Int?) {
        _etaMinutes.value = minutes
    }

    fun setMonitoringActive(active: Boolean) {
        _isMonitoringActive.value = active
    }
}
