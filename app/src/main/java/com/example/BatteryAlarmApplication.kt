package com.example

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.example.service.BatteryStateTracker
import com.example.service.PowerConnectionReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class BatteryAlarmApplication : Application() {

    companion object {
        lateinit var applicationScope: CoroutineScope
            private set
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("BatteryAlarmApplication", "onCreate")
        applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        // Initialize state tracker with initial current battery/charging states synchronously on application startup
        BatteryStateTracker.initialize(this)

        // Schedule JobService to detect charger plugged in when the app is completely closed
        BatteryStateTracker.schedulePowerConnectionJob(this)

        // Register PowerConnectionReceiver dynamically to support Android 8.0+ devices
        // where manifest-declared receivers for non-exempt broadcasts are restricted.
        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
            }
            registerReceiver(PowerConnectionReceiver(), filter)
            Log.d("BatteryAlarmApplication", "PowerConnectionReceiver dynamically registered successfully.")
        } catch (e: Exception) {
            Log.e("BatteryAlarmApplication", "Failed to register PowerConnectionReceiver dynamically", e)
        }
    }
}
