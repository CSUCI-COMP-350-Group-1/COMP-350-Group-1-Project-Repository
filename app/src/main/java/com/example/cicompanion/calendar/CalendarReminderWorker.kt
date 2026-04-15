package com.example.cicompanion.calendar

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.cicompanion.notifications.AppNotificationManager

class CalendarReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : Worker(appContext, params) {

    override fun doWork(): Result {
        val title = inputData.getString(KEY_EVENT_TITLE) ?: return Result.failure()
        val location = inputData.getString(KEY_EVENT_LOCATION).orEmpty()
        val timeLabel = inputData.getString(KEY_EVENT_TIME_LABEL).orEmpty()

        // Show the local notification when this worker fires.
        AppNotificationManager.showCalendarReminderNotification(
            context = applicationContext,
            title = title,
            location = location,
            timeLabel = timeLabel
        )

        return Result.success()
    }

    companion object {
        // Input keys used by the scheduler.
        const val KEY_EVENT_TITLE = "key_event_title"
        const val KEY_EVENT_LOCATION = "key_event_location"
        const val KEY_EVENT_TIME_LABEL = "key_event_time_label"
    }
}