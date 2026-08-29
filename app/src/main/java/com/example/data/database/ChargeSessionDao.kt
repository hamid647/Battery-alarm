package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChargeSessionDao {
    @Query("SELECT * FROM charge_sessions ORDER BY endTime DESC")
    fun getAllSessions(): Flow<List<ChargeSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChargeSession)

    @Query("DELETE FROM charge_sessions")
    suspend fun clearAllSessions()
}
