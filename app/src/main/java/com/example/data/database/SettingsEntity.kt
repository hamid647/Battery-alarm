package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val thresholdPercent: Int = 90,
    val alarmSound: String = "digital_beep", // digital_beep, classic_alarm, siren, chime, custom
    val vibrationEnabled: Boolean = true,
    val snoozeDurationMinutes: Int = 5,
    val overchargeProtectionEnabled: Boolean = true,
    val overchargeIntervalMinutes: Int = 10,
    val customAlarmUri: String? = null,
    val customAlarmName: String? = null
)
