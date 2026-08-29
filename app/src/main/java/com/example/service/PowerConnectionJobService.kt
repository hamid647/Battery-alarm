package com.example.service

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Intent
import android.os.Build
import android.util.Log

class PowerConnectionJobService : JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        Log.d("PowerConnectionJob", "Job started. Charger is plugged in. Starting BatteryMonitorService automatically.")
        BatteryStateTracker.startServiceSafely(this)
        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return false
    }
}
