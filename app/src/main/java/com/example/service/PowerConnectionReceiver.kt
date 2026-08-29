package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class PowerConnectionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("PowerConnectionReceiver", "Received action: $action")

        when (action) {
            Intent.ACTION_POWER_CONNECTED -> {
                Log.d("PowerConnectionReceiver", "Power connected. Starting Battery Monitor Service safely.")
                BatteryStateTracker.startServiceSafely(context)
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                Log.d("PowerConnectionReceiver", "Power disconnected. Stopping Battery Monitor Service.")
                val serviceIntent = Intent(context, BatteryMonitorService::class.java).apply {
                    this.action = "ACTION_STOP_SERVICE"
                }
                try {
                    context.startService(serviceIntent)
                } catch (e: Exception) {
                    Log.e("PowerConnectionReceiver", "Failed to stop BatteryMonitorService on power disconnected", e)
                }
            }
        }
    }
}
