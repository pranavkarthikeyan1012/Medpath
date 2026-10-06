package com.example.data.db

import android.content.Context
import com.example.data.MedicationItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MedicationRepository(context: Context) {
    private val database = MedPathDatabase.getDatabase(context)
    private val medicationDao = database.medicationDao()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            val existing = medicationDao.getAllMedications()
            if (existing.isEmpty()) {
                medicationDao.insertAll(MedPathDatabase.initialMedications)
            }
        }
    }

    val allMedicationsFlow: Flow<List<MedicationItem>> = medicationDao.getAllMedicationsFlow()
        .map { entities -> entities.map { it.toMedicationItem() } }

    suspend fun getAllMedications(): List<MedicationItem> = withContext(Dispatchers.IO) {
        val list = medicationDao.getAllMedications()
        if (list.isEmpty()) {
            medicationDao.insertAll(MedPathDatabase.initialMedications)
            MedPathDatabase.initialMedications.map { it.toMedicationItem() }
        } else {
            list.map { it.toMedicationItem() }
        }
    }

    suspend fun getScheduledReminderMedications(): List<MedicationEntity> = withContext(Dispatchers.IO) {
        medicationDao.getActiveReminderMedications()
    }

    suspend fun getMedicationById(id: String): MedicationItem? = withContext(Dispatchers.IO) {
        medicationDao.getMedicationById(id)?.toMedicationItem()
    }

    suspend fun insertMedication(item: MedicationItem) = withContext(Dispatchers.IO) {
        medicationDao.insertMedication(MedicationEntity.fromMedicationItem(item))
    }

    suspend fun toggleTaken(id: String, isTaken: Boolean, timestamp: String?) = withContext(Dispatchers.IO) {
        medicationDao.updateTakenStatus(id, isTaken, timestamp)
    }

    suspend fun toggleReminder(id: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        medicationDao.updateReminderEnabled(id, enabled)
    }

    suspend fun deleteMedication(id: String) = withContext(Dispatchers.IO) {
        medicationDao.deleteMedicationById(id)
    }
}
