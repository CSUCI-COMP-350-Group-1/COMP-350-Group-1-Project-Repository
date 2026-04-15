package com.example.cicompanion.calendar

import android.content.Context
import android.content.SharedPreferences
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.example.cicompanion.calendar.model.CalendarEvent
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

object CalendarReminderScheduler {

    private const val IMMEDIATE_SYNC_WORK_NAME = "calendar_reminder_sync_now"
    private const val PERIODIC_SYNC_WORK_NAME = "calendar_reminder_sync_periodic"
    private const val WORK_NAME_PREFIX = "calendar_reminder_"

    private const val PREFS_NAME = "calendar_reminder_scheduler_prefs"
    private const val KEY_SCHEDULED_WORK_NAMES = "scheduled_work_names"

    private val reminderTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    //Called once at startup to keep calendar reminders current.
    fun start(context: Context) {
        enqueueImmediateSync(context)
        enqueuePeriodicSync(context)
    }

    //Rebuild all reminder work from the latest event list.
    fun rescheduleEventReminders(
        context: Context,
        events: List<CalendarEvent>
    ) {
        val workManager = WorkManager.getInstance(context)
        val desiredWorkNames = mutableSetOf<String>()

        val schedulableEvents = events.filter(::shouldScheduleReminder)

        schedulableEvents.forEach { event ->
            val workName = buildReminderWorkName(event)
            desiredWorkNames.add(workName)

            workManager.enqueueUniqueWork(
                workName,
                ExistingWorkPolicy.REPLACE,
                buildReminderWorkRequest(event)
            )
        }

        cancelRemovedReminderWork(
            context = context,
            workManager = workManager,
            desiredWorkNames = desiredWorkNames
        )

        saveScheduledWorkNames(context, desiredWorkNames)
    }

    private fun enqueueImmediateSync(context: Context) {
        val request = OneTimeWorkRequest.Builder(CalendarReminderSyncWorker::class.java)
            .setConstraints(buildNetworkConstraints())
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_SYNC_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun enqueuePeriodicSync(context: Context) {
        val request = PeriodicWorkRequest.Builder(
            CalendarReminderSyncWorker::class.java,
            12,
            TimeUnit.HOURS
        )
            .setConstraints(buildNetworkConstraints())
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun buildNetworkConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }

    private fun shouldScheduleReminder(event: CalendarEvent): Boolean {
        val now = ZonedDateTime.now(event.start.zone)
        val reminderTime = buildReminderTriggerTime(event)

        return event.endExclusive.isAfter(now) && reminderTime.isAfter(now)
    }

    private fun buildReminderWorkRequest(event: CalendarEvent): OneTimeWorkRequest {
        val reminderTime = buildReminderTriggerTime(event)
        val delayMillis = Duration
            .between(ZonedDateTime.now(reminderTime.zone), reminderTime)
            .toMillis()
            .coerceAtLeast(0L)

        val inputData = Data.Builder()
            .putString(
                CalendarReminderWorker.KEY_EVENT_TITLE,
                formatEventTextForDisplay(event.title)
            )
            .putString(
                CalendarReminderWorker.KEY_EVENT_LOCATION,
                event.location?.let(::formatEventTextForDisplay).orEmpty()
            )
            .putString(
                CalendarReminderWorker.KEY_EVENT_TIME_LABEL,
                buildNotificationTimeLabel(event)
            )
            .build()

        return OneTimeWorkRequest.Builder(CalendarReminderWorker::class.java)
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()
    }

    private fun buildReminderTriggerTime(event: CalendarEvent): ZonedDateTime {
        return if (event.isAllDay) {
            //Avoid midnight notifications for all-day events
            //Just want to see what this may do
            event.start.toLocalDate()
                .minusDays(1)
                .atTime(9, 0)
                .atZone(event.start.zone)
        } else {
            event.start.minusDays(1)
        }
    }

    //Test notification
    //Comment out above for test
    /*private fun buildReminderTriggerTime(event: CalendarEvent): ZonedDateTime {
        // TEST ONLY: fire reminder 1 minute from now
        return ZonedDateTime.now(event.start.zone).plusMinutes(1)
    }*/

    private fun buildNotificationTimeLabel(event: CalendarEvent): String {
        return if (event.isAllDay) {
            "All day tomorrow"
        } else {
            "Starts tomorrow at ${event.start.format(reminderTimeFormatter)}"
        }
    }

    private fun buildReminderWorkName(event: CalendarEvent): String {
        return WORK_NAME_PREFIX + event.id.hashCode()
    }

    private fun cancelRemovedReminderWork(
        context: Context,
        workManager: WorkManager,
        desiredWorkNames: Set<String>
    ) {
        val existingWorkNames = loadScheduledWorkNames(context)
        val workNamesToCancel = existingWorkNames - desiredWorkNames

        workNamesToCancel.forEach { workName ->
            workManager.cancelUniqueWork(workName)
        }
    }

    private fun loadScheduledWorkNames(context: Context): Set<String> {
        return preferences(context)
            .getStringSet(KEY_SCHEDULED_WORK_NAMES, emptySet())
            ?.toSet()
            ?: emptySet()
    }

    private fun saveScheduledWorkNames(
        context: Context,
        workNames: Set<String>
    ) {
        preferences(context)
            .edit()
            .putStringSet(KEY_SCHEDULED_WORK_NAMES, workNames)
            .apply()
    }

    private fun preferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}