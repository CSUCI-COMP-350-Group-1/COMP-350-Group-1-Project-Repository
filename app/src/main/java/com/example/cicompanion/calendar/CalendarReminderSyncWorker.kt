package com.example.cicompanion.calendar

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.cicompanion.calendar.data.repository.CalendarRepository
import kotlinx.coroutines.runBlocking

class CalendarReminderSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : Worker(appContext, params) {

    private val repository = CalendarRepository()

    override fun doWork(): Result {
        return try {
            //Load latest calendar feed in the background
            val events = runBlocking {
                repository.loadEvents(CalendarFeedConfig.CSUCI_CALENDAR_SUBSCRIBE_URL)
            }

            //Rebuild the scheduled reminder jobs from the latest feed.
            CalendarReminderScheduler.rescheduleEventReminders(
                context = applicationContext,
                events = events
            )

            Result.success()
        } catch (_: Exception) {
            //Retry later if the feed load fails.
            Result.retry()
        }
    }
}