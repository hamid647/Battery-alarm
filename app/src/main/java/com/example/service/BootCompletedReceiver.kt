package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootCompletedReceiver", "Device reboot completed. Scheduling PowerConnectionJob and starting service if plugged.")
            
            // Re-arm power connection JobService on boot
            BatteryStateTracker.schedulePowerConnectionJob(context)
            
            BatteryStateTracker.startServiceSafely(context)
        }
    }
}
