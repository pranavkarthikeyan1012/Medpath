package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.example.data.HealthDataManager
import com.example.data.db.MedicationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MedicationReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val medId = intent.getStringExtra(MedicationNotificationManager.EXTRA_MED_ID) ?: ""
        val medName = intent.getStringExtra(MedicationNotificationManager.EXTRA_MED_NAME) ?: "Medication"
        val medDosage = intent.getStringExtra(MedicationNotificationManager.EXTRA_MED_DOSAGE) ?: ""
        val medTime = intent.getStringExtra(MedicationNotificationManager.EXTRA_MED_TIME) ?: ""
        val medInstruction = intent.getStringExtra(MedicationNotificationManager.EXTRA_MED_INSTRUCTION) ?: ""

        val repository = MedicationRepository(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (action) {
            MedicationNotificationManager.ACTION_MEDICATION_ALARM -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val med = repository.getMedicationById(medId)
                    // Only alert if reminder is enabled and not already taken
                    if (med == null || (!med.isTaken && med.reminderEnabled)) {
                        MedicationNotificationManager.showMedicationNotification(
                            context = context,
                            medicationId = medId,
                            name = med?.name ?: medName,
                            dosage = med?.dosage ?: medDosage,
                            time = med?.scheduledTime ?: medTime,
                            instruction = med?.instruction ?: medInstruction
                        )
                    }
                    // Reschedule for next day
                    if (med != null) {
                        MedicationNotificationManager.scheduleMedicationReminder(context, med)
                    }
                }
            }

            MedicationNotificationManager.ACTION_MARK_TAKEN -> {
                // Dismiss notification
                notificationManager.cancel(medId.hashCode())

                // Update in Room Database and in-memory HealthDataManager
                HealthDataManager.toggleMedicationTaken(medId)
                CoroutineScope(Dispatchers.IO).launch {
                    repository.toggleTaken(medId, true, "Today at scheduled time")
                }

                Toast.makeText(context, "✓ Marked $medName as taken", Toast.LENGTH_SHORT).show()
            }

            MedicationNotificationManager.ACTION_SNOOZE -> {
                // Dismiss notification
                notificationManager.cancel(medId.hashCode())

                // Set snooze alarm 15 minutes from now
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                if (alarmManager != null) {
                    val snoozeCalendarTime = System.currentTimeMillis() + 15 * 60 * 1000

                    val snoozeIntent = Intent(context, MedicationReminderReceiver::class.java).apply {
                        this.action = MedicationNotificationManager.ACTION_MEDICATION_ALARM
                        putExtra(MedicationNotificationManager.EXTRA_MED_ID, medId)
                        putExtra(MedicationNotificationManager.EXTRA_MED_NAME, medName)
                        putExtra(MedicationNotificationManager.EXTRA_MED_DOSAGE, medDosage)
                        putExtra(MedicationNotificationManager.EXTRA_MED_TIME, medTime)
                        putExtra(MedicationNotificationManager.EXTRA_MED_INSTRUCTION, medInstruction)
                    }

                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        medId.hashCode() + 999,
                        snoozeIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                snoozeCalendarTime,
                                pendingIntent
                            )
                        } else {
                            alarmManager.setExact(
                                AlarmManager.RTC_WAKEUP,
                                snoozeCalendarTime,
                                pendingIntent
                            )
                        }
                        Toast.makeText(context, "Snoozed $medName for 15 minutes", Toast.LENGTH_SHORT).show()
                    } catch (e: SecurityException) {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, snoozeCalendarTime, pendingIntent)
                    }
                }
            }

            Intent.ACTION_BOOT_COMPLETED -> {
                // Reschedule all reminders from Room Database upon reboot
                CoroutineScope(Dispatchers.IO).launch {
                    val scheduledMeds = repository.getAllMedications()
                    MedicationNotificationManager.scheduleAllReminders(context, scheduledMeds)
                }
            }
        }
    }
}
