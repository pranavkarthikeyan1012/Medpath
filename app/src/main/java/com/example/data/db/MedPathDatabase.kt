package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [MedicationEntity::class], version = 1, exportSchema = false)
abstract class MedPathDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao

    companion object {
        @Volatile
        private var INSTANCE: MedPathDatabase? = null

        fun getDatabase(context: Context): MedPathDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MedPathDatabase::class.java,
                    "medpath_database.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialMedications(database.medicationDao())
                    }
                }
            }

            suspend fun populateInitialMedications(dao: MedicationDao) {
                dao.insertAll(initialMedications)
            }
        }

        val initialMedications = listOf(
            MedicationEntity(
                id = "med_1",
                name = "Metformin Hydrochloride",
                dosage = "500 mg",
                frequency = "Twice daily",
                timeSlot = "Morning",
                scheduledTime = "08:00 AM",
                hour = 8,
                minute = 0,
                instruction = "Take with or immediately after breakfast",
                purpose = "Type 2 Diabetes glucose regulation",
                pillAppearance = "White oval tablet with '500' imprint",
                pillsRemaining = 18,
                refillDays = 9,
                isTaken = true,
                takenTimestamp = "08:14 AM",
                reminderEnabled = true
            ),
            MedicationEntity(
                id = "med_2",
                name = "Glimepiride",
                dosage = "1 mg",
                frequency = "Once daily",
                timeSlot = "Morning",
                scheduledTime = "08:00 AM",
                hour = 8,
                minute = 0,
                instruction = "Take with first main meal of the day",
                purpose = "Stimulates insulin secretion",
                pillAppearance = "Small pink oblong tablet",
                pillsRemaining = 8,
                refillDays = 4,
                isTaken = true,
                takenTimestamp = "08:15 AM",
                reminderEnabled = true
            ),
            MedicationEntity(
                id = "med_4",
                name = "Vitamin D3 + Calcium",
                dosage = "60,000 IU / 500 mg",
                frequency = "After lunch",
                timeSlot = "Afternoon",
                scheduledTime = "01:30 PM",
                hour = 13,
                minute = 30,
                instruction = "Take after lunch with plain water",
                purpose = "Bone health & metabolic support",
                pillAppearance = "Yellow softgel capsule",
                pillsRemaining = 14,
                refillDays = 14,
                isTaken = false,
                takenTimestamp = null,
                reminderEnabled = true
            ),
            MedicationEntity(
                id = "med_3",
                name = "Atorvastatin",
                dosage = "10 mg",
                frequency = "Once daily at bedtime",
                timeSlot = "Night",
                scheduledTime = "09:30 PM",
                hour = 21,
                minute = 30,
                instruction = "Take at bedtime with a glass of water",
                purpose = "Cholesterol & cardiovascular risk reduction",
                pillAppearance = "White round film-coated tablet",
                pillsRemaining = 24,
                refillDays = 24,
                isTaken = false,
                takenTimestamp = null,
                reminderEnabled = true
            )
        )
    }
}
