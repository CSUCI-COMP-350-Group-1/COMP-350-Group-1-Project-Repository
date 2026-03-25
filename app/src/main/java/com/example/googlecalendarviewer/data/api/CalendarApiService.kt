package com.example.googlecalendarviewer.data.api

import android.net.Uri
import com.example.googlecalendarviewer.model.CalendarEvent
import com.example.googlecalendarviewer.model.CalendarSummary
import com.example.googlecalendarviewer.model.EventDraft
import com.example.googlecalendarviewer.model.EventsPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class CalendarApiService {

    suspend fun listCalendars(accessToken: String): List<CalendarSummary> =
        withContext(Dispatchers.IO) {
            val url = "https://www.googleapis.com/calendar/v3/users/me/calendarList"
            val connection = openConnection(url, accessToken, "GET")

            try {
                val (code, body) = readResponse(connection)

                if (code == 401) {
                    throw IllegalStateException("Authorization expired. Connect Google Calendar again.")
                }
                if (code !in 200..299) {
                    throw IllegalStateException("Failed to load calendar list: $body")
                }

                parseCalendars(body)
            } finally {
                connection.disconnect()
            }
        }

    suspend fun listEvents(
        accessToken: String,
        calendarId: String,
        from: ZonedDateTime,
        until: ZonedDateTime
    ): EventsPage = withContext(Dispatchers.IO) {
        val endpoint =
            "https://www.googleapis.com/calendar/v3/calendars/${Uri.encode(calendarId)}/events"

        val url = Uri.parse(endpoint)
            .buildUpon()
            .appendQueryParameter("singleEvents", "true")
            .appendQueryParameter("orderBy", "startTime")
            .appendQueryParameter("timeMin", from.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
            .appendQueryParameter("timeMax", until.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
            .appendQueryParameter("timeZone", ZoneId.systemDefault().id)
            .build()
            .toString()

        val connection = openConnection(url, accessToken, "GET")

        try {
            val (code, body) = readResponse(connection)

            if (code == 401) {
                throw IllegalStateException("Authorization expired. Connect Google Calendar again.")
            }
            if (code !in 200..299) {
                throw IllegalStateException("Failed to load events: $body")
            }

            parseEventsPage(body)
        } finally {
            connection.disconnect()
        }
    }

    suspend fun insertEvent(
        accessToken: String,
        calendarId: String,
        draft: EventDraft
    ): CalendarEvent = withContext(Dispatchers.IO) {
        val endpoint =
            "https://www.googleapis.com/calendar/v3/calendars/${Uri.encode(calendarId)}/events"

        val zone = ZoneId.systemDefault()
        val payload = JSONObject().apply {
            put("summary", draft.title)

            if (draft.description.isNotBlank()) {
                put("description", draft.description)
            }
            if (draft.location.isNotBlank()) {
                put("location", draft.location)
            }

            if (draft.isAllDay) {
                put(
                    "start",
                    JSONObject().apply {
                        put("date", draft.date.toString())
                    }
                )
                put(
                    "end",
                    JSONObject().apply {
                        put("date", draft.date.plusDays(1).toString())
                    }
                )
            } else {
                val start = draft.date.atTime(draft.startTime).atZone(zone)
                val end = draft.date.atTime(draft.endTime).atZone(zone)

                put(
                    "start",
                    JSONObject().apply {
                        put("dateTime", start.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                        put("timeZone", zone.id)
                    }
                )
                put(
                    "end",
                    JSONObject().apply {
                        put("dateTime", end.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                        put("timeZone", zone.id)
                    }
                )
            }
        }

        val connection = openConnection(endpoint, accessToken, "POST").apply {
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }

        try {
            BufferedWriter(OutputStreamWriter(connection.outputStream)).use { writer ->
                writer.write(payload.toString())
            }

            val (code, body) = readResponse(connection)

            if (code == 401) {
                throw IllegalStateException("Authorization expired. Connect Google Calendar again.")
            }
            if (code !in 200..299) {
                throw IllegalStateException("Failed to create event: $body")
            }

            parseEvent(JSONObject(body))
                ?: throw IllegalStateException("The API returned an event that could not be parsed.")
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(
        url: String,
        accessToken: String,
        method: String
    ): HttpURLConnection {
        return (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Accept", "application/json")
        }
    }

    private fun readResponse(connection: HttpURLConnection): Pair<Int, String> {
        val code = connection.responseCode
        val stream = if (code in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }

        val body = stream?.readText().orEmpty()
        return code to body
    }

    private fun parseCalendars(json: String): List<CalendarSummary> {
        val root = JSONObject(json)
        val items = root.optJSONArray("items") ?: JSONArray()

        return buildList {
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                add(
                    CalendarSummary(
                        id = item.optString("id"),
                        title = item.optString("summary").ifBlank { "(Untitled calendar)" },
                        isPrimary = item.optBoolean("primary", false)
                    )
                )
            }
        }
    }

    private fun parseEventsPage(json: String): EventsPage {
        val root = JSONObject(json)
        val items = root.optJSONArray("items") ?: JSONArray()
        val parsedEvents = buildList {
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                parseEvent(item)?.let(::add)
            }
        }

        return EventsPage(
            items = parsedEvents,
            nextSyncToken = root.optString("nextSyncToken").takeIf { it.isNotBlank() }
        )
    }

    private fun parseEvent(item: JSONObject): CalendarEvent? {
        if (item.optString("status") == "cancelled") return null

        val startObject = item.optJSONObject("start") ?: return null
        val endObject = item.optJSONObject("end") ?: return null

        val startDateTime = startObject.optString("dateTime").takeIf { it.isNotBlank() }
        val endDateTime = endObject.optString("dateTime").takeIf { it.isNotBlank() }

        val startDate = startObject.optString("date").takeIf { it.isNotBlank() }
        val endDate = endObject.optString("date").takeIf { it.isNotBlank() }

        val title = item.optString("summary").ifBlank { "(No title)" }
        val description = item.optString("description").takeIf { it.isNotBlank() }
        val location = item.optString("location").takeIf { it.isNotBlank() }
        val htmlLink = item.optString("htmlLink").takeIf { it.isNotBlank() }

        return when {
            startDateTime != null && endDateTime != null -> {
                CalendarEvent(
                    id = item.optString("id"),
                    calendarId = item.optString("organizer"),
                    title = title,
                    description = description,
                    location = location,
                    htmlLink = htmlLink,
                    start = OffsetDateTime.parse(startDateTime).toZonedDateTime(),
                    endExclusive = OffsetDateTime.parse(endDateTime).toZonedDateTime(),
                    isAllDay = false
                )
            }

            startDate != null && endDate != null -> {
                val zone = ZoneId.systemDefault()
                CalendarEvent(
                    id = item.optString("id"),
                    calendarId = item.optString("organizer"),
                    title = title,
                    description = description,
                    location = location,
                    htmlLink = htmlLink,
                    start = LocalDate.parse(startDate).atStartOfDay(zone),
                    endExclusive = LocalDate.parse(endDate).atStartOfDay(zone),
                    isAllDay = true
                )
            }

            else -> null
        }
    }

    private fun InputStream.readText(): String =
        BufferedReader(InputStreamReader(this)).use { it.readText() }
}