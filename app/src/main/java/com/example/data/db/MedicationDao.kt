package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications ORDER BY hour ASC, minute ASC")
    fun getAllMedicationsFlow(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications ORDER BY hour ASC, minute ASC")
    suspend fun getAllMedications(): List<MedicationEntity>

    @Query("SELECT * FROM medications WHERE id = :id LIMIT 1")
    suspend fun getMedicationById(id: String): MedicationEntity?

    @Query("SELECT * FROM medications WHERE reminderEnabled = 1")
    suspend fun getActiveReminderMedications(): List<MedicationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: MedicationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(medications: List<MedicationEntity>)

    @Update
    suspend fun updateMedication(medication: MedicationEntity)

    @Query("UPDATE medications SET isTaken = :isTaken, takenTimestamp = :timestamp WHERE id = :id")
    suspend fun updateTakenStatus(id: String, isTaken: Boolean, timestamp: String?)

    @Query("UPDATE medications SET reminderEnabled = :enabled WHERE id = :id")
    suspend fun updateReminderEnabled(id: String, enabled: Boolean)

    @Query("DELETE FROM medications WHERE id = :id")
    suspend fun deleteMedicationById(id: String)

    @Query("DELETE FROM medications")
    suspend fun clearAll()
}
