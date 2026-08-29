package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ChargeSession::class, SettingsEntity::class], version = 2, exportSchema = false)
abstract class BatteryAlarmDatabase : RoomDatabase() {
    abstract fun chargeSessionDao(): ChargeSessionDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: BatteryAlarmDatabase? = null

        fun getDatabase(context: Context): BatteryAlarmDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BatteryAlarmDatabase::class.java,
                    "battery_alarm_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
