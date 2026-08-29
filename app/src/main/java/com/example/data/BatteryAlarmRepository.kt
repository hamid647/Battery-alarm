package com.example.data

import com.example.data.database.ChargeSession
import com.example.data.database.ChargeSessionDao
import com.example.data.database.SettingsDao
import com.example.data.database.SettingsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class BatteryAlarmRepository(
    private val settingsDao: SettingsDao,
    private val chargeSessionDao: ChargeSessionDao
) {
    val settingsFlow: Flow<SettingsEntity> = settingsDao.getSettingsFlow()
        .map { it ?: SettingsEntity() }

    val allSessions: Flow<List<ChargeSession>> = chargeSessionDao.getAllSessions()

    suspend fun getSettings(): SettingsEntity = withContext(Dispatchers.IO) {
        settingsDao.getSettings() ?: SettingsEntity()
    }

    suspend fun saveSettings(settings: SettingsEntity) = withContext(Dispatchers.IO) {
        settingsDao.saveSettings(settings)
    }

    suspend fun addSession(session: ChargeSession) = withContext(Dispatchers.IO) {
        chargeSessionDao.insertSession(session)
    }

    suspend fun clearSessions() = withContext(Dispatchers.IO) {
        chargeSessionDao.clearAllSessions()
    }
}
