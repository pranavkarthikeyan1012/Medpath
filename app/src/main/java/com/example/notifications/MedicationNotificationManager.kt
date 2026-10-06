package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.MedicationItem
import com.example.data.db.MedicationEntity
import java.util.Calendar

object MedicationNotificationManager {
    const val CHANNEL_ID = "medication_reminders_channel"
    const val CHANNEL_NAME = "Medication Schedule Reminders"
    const val CHANNEL_DESC = "Notifications to remind you to take prescribed medications on schedule."

    const val ACTION_MEDICATION_ALARM = "com.example.medpath.ACTION_MEDICATION_ALARM"
    const val ACTION_MARK_TAKEN = "com.example.medpath.ACTION_MARK_TAKEN"
    const val ACTION_SNOOZE = "com.example.medpath.ACTION_SNOOZE"

    const val EXTRA_MED_ID = "extra_med_id"
    const val EXTRA_MED_NAME = "extra_med_name"
    const val EXTRA_MED_DOSAGE = "extra_med_dosage"
    const val EXTRA_MED_TIME = "extra_med_time"
    const val EXTRA_MED_INSTRUCTION = "extra_med_instruction"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleMedicationReminder(context: Context, medication: MedicationItem) {
        if (!medication.reminderEnabled) return

        createNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val (hour, minute) = MedicationEntity.parseTime(medication.scheduledTime, medication.timeSlot)

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If time has already passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, MedicationReminderReceiver::class.java).apply {
            action = ACTION_MEDICATION_ALARM
            putExtra(EXTRA_MED_ID, medication.id)
            putExtra(EXTRA_MED_NAME, medication.name)
            putExtra(EXTRA_MED_DOSAGE, medication.dosage)
            putExtra(EXTRA_MED_TIME, medication.scheduledTime)
            putExtra(EXTRA_MED_INSTRUCTION, medication.instruction)
        }

        val requestCode = medication.id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // If exact alarm permission is restricted, fallback to inexact
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun scheduleAllReminders(context: Context, medications: List<MedicationItem>) {
        medications.forEach { med ->
            scheduleMedicationReminder(context, med)
        }
    }

    fun cancelReminder(context: Context, medicationId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, MedicationReminderReceiver::class.java).apply {
            action = ACTION_MEDICATION_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medicationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun showMedicationNotification(
        context: Context,
        medicationId: String,
        name: String,
        dosage: String,
        time: String,
        instruction: String
    ) {
        createNotificationChannel(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationId = medicationId.hashCode()

        // Content Intent: Opens app
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("route", "medications")
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: Mark as Taken
        val markTakenIntent = Intent(context, MedicationReminderReceiver::class.java).apply {
            action = ACTION_MARK_TAKEN
            putExtra(EXTRA_MED_ID, medicationId)
            putExtra(EXTRA_MED_NAME, name)
        }
        val markTakenPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            markTakenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Snooze 15 Minutes
        val snoozeIntent = Intent(context, MedicationReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_MED_ID, medicationId)
            putExtra(EXTRA_MED_NAME, name)
            putExtra(EXTRA_MED_DOSAGE, dosage)
            putExtra(EXTRA_MED_TIME, time)
            putExtra(EXTRA_MED_INSTRUCTION, instruction)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("💊 Time to take $name ($dosage)")
            .setContentText("$instruction • Scheduled for $time")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Prescribed Dose: $dosage\nSchedule: $time\nInstruction: $instruction\n\nStaying adherent helps maintain glycemic & metabolic control.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "Mark Taken ✓", markTakenPendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "Snooze 15m", snoozePendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun triggerTestNotification(context: Context, medication: MedicationItem) {
        showMedicationNotification(
            context = context,
            medicationId = medication.id,
            name = medication.name,
            dosage = medication.dosage,
            time = medication.scheduledTime,
            instruction = medication.instruction
        )
    }
}
