package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.MedicationItem

@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val dosage: String,
    val frequency: String,
    val timeSlot: String, // "Morning", "Afternoon", "Evening", "Night"
    val scheduledTime: String,
    val hour: Int,
    val minute: Int,
    val instruction: String,
    val purpose: String,
    val pillAppearance: String,
    val pillsRemaining: Int,
    val refillDays: Int,
    val isTaken: Boolean = false,
    val takenTimestamp: String? = null,
    val reminderEnabled: Boolean = true
) {
    fun toMedicationItem(): MedicationItem = MedicationItem(
        id = id,
        name = name,
        dosage = dosage,
        frequency = frequency,
        timeSlot = timeSlot,
        scheduledTime = scheduledTime,
        instruction = instruction,
        purpose = purpose,
        pillAppearance = pillAppearance,
        pillsRemaining = pillsRemaining,
        refillDays = refillDays,
        isTaken = isTaken,
        takenTimestamp = takenTimestamp,
        reminderEnabled = reminderEnabled
    )

    companion object {
        fun fromMedicationItem(item: MedicationItem): MedicationEntity {
            val (h, m) = parseTime(item.scheduledTime, item.timeSlot)
            return MedicationEntity(
                id = item.id,
                name = item.name,
                dosage = item.dosage,
                frequency = item.frequency,
                timeSlot = item.timeSlot,
                scheduledTime = item.scheduledTime,
                hour = h,
                minute = m,
                instruction = item.instruction,
                purpose = item.purpose,
                pillAppearance = item.pillAppearance,
                pillsRemaining = item.pillsRemaining,
                refillDays = item.refillDays,
                isTaken = item.isTaken,
                takenTimestamp = item.takenTimestamp,
                reminderEnabled = item.reminderEnabled
            )
        }

        fun parseTime(timeStr: String, slot: String): Pair<Int, Int> {
            return try {
                val cleaned = timeStr.trim().uppercase()
                val isPm = cleaned.contains("PM")
                val isAm = cleaned.contains("AM")
                val numbersPart = cleaned.replace("AM", "").replace("PM", "").trim()
                val parts = numbersPart.split(":")
                var hour = parts[0].toIntOrNull() ?: 8
                val min = if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0
                if (isPm && hour < 12) hour += 12
                if (isAm && hour == 12) hour = 0
                Pair(hour, min)
            } catch (e: Exception) {
                when (slot.lowercase()) {
                    "morning" -> Pair(8, 0)
                    "afternoon" -> Pair(13, 30)
                    "evening" -> Pair(18, 0)
                    "night" -> Pair(21, 30)
                    else -> Pair(8, 0)
                }
            }
        }
    }
}
