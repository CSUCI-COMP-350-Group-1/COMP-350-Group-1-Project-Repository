package com.example.sprint1homeui.calendar.data.api

import android.net.Uri
import com.example.sprint1homeui.calendar.model.CalendarEvent
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
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class CalendarApiService {

    /** Downloads a public ICS calendar feed and turns it into app events. */
    suspend fun fetchEvents(sourceUrl: String): List<CalendarEvent> = withContext(Dispatchers.IO) {
        val candidates = buildCandidateUrls(sourceUrl)
        var lastError: Exception? = null

        for (candidate in candidates) {
            try {
                val connection = openNormalizedConnection(candidate)

                try {
                    val body = readBody(connection)
                    return@withContext parseEvents(body, sourceUrl)
                } finally {
                    connection.disconnect()
                }
            } catch (error: Exception) {
                lastError = error
            }
        }

        throw IllegalStateException("Failed to load calendar feed.", lastError)
    }

    /** Builds the possible download URLs for a subscribe link. */
    private fun buildCandidateUrls(sourceUrl: String): List<String> {
        val trimmedUrl = sourceUrl.trim()
        if (!trimmedUrl.startsWith("webcal://", ignoreCase = true)) {
            return listOf(trimmedUrl)
        }

        val path = trimmedUrl.removePrefix("webcal://")
        return listOf("https://$path", "http://$path")
    }

    /** Opens the final normalized URL connection used for the download. */
    private fun openNormalizedConnection(sourceUrl: String): HttpURLConnection {
        return (URL(sourceUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Accept", "text/calendar, text/plain, */*")
        }
    }

    /** Reads the response body and throws an error when the request fails. */
    private fun readBody(connection: HttpURLConnection): String {
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.readText().orEmpty()

        if (code !in 200..299) {
            throw IllegalStateException("Failed to load calendar feed: $body")
        }

        return body
    }

    /** Parses every VEVENT block in the ICS text into app events. */
    private fun parseEvents(icsText: String, sourceUrl: String): List<CalendarEvent> {
        val lines = unfoldLines(icsText)
        val events = mutableListOf<CalendarEvent>()
        var currentFields = mutableListOf<IcsField>()
        var insideEvent = false

        for (line in lines) {
            when (line) {
                "BEGIN:VEVENT" -> {
                    insideEvent = true
                    currentFields = mutableListOf()
                }

                "END:VEVENT" -> {
                    insideEvent = false
                    parseEvent(currentFields, sourceUrl)?.let(events::add)
                }

                else -> {
                    if (insideEvent) {
                        parseField(line)?.let(currentFields::add)
                    }
                }
            }
        }

        return events
    }

    /** Rebuilds folded ICS lines into complete single lines. */
    private fun unfoldLines(text: String): List<String> {
        val unfoldedLines = mutableListOf<String>()

        text.replace("\r\n", "\n").split("\n").forEach { rawLine ->
            if (rawLine.startsWith(" ") || rawLine.startsWith("\t")) {
                if (unfoldedLines.isNotEmpty()) {
                    val previous = unfoldedLines.removeLast()
                    unfoldedLines.add(previous + rawLine.trimStart())
                }
            } else {
                unfoldedLines.add(rawLine)
            }
        }

        return unfoldedLines
    }

    /** Splits one ICS line into a name, parameter list, and value. */
    private fun parseField(line: String): IcsField? {
        val separatorIndex = line.indexOf(':')
        if (separatorIndex == -1) return null

        val propertyPart = line.substring(0, separatorIndex)
        val valuePart = line.substring(separatorIndex + 1)
        val propertyPieces = propertyPart.split(';')
        val name = propertyPieces.firstOrNull()?.uppercase().orEmpty()
        val parameters = propertyPieces.drop(1).associate { parameter ->
            val parts = parameter.split('=', limit = 2)
            val key = parts.firstOrNull()?.uppercase().orEmpty()
            val value = parts.getOrNull(1).orEmpty()
            key to value
        }

        return IcsField(name = name, value = valuePart, parameters = parameters)
    }

    /** Converts the fields from one VEVENT block into a CalendarEvent. */
    private fun parseEvent(fields: List<IcsField>, sourceUrl: String): CalendarEvent? {
        val startField = fields.firstOrNull { it.name == "DTSTART" } ?: return null
        val endField = fields.firstOrNull { it.name == "DTEND" }

        if (findOptionalFieldValue(fields, "STATUS")?.uppercase() == "CANCELLED") {
            return null
        }

        val title = findFieldValue(fields, "SUMMARY").ifBlank { "(No title)" }
        val description = findOptionalFieldValue(fields, "DESCRIPTION")
        val location = findOptionalFieldValue(fields, "LOCATION")
        val htmlLink = findOptionalFieldValue(fields, "URL")
        val id = findOptionalFieldValue(fields, "UID") ?: buildFallbackId(startField, title)

        val start = parseDateTime(startField)
        val endExclusive = parseEndDateTime(startField, endField, start)
        val isAllDay = isAllDayField(startField)

        return CalendarEvent(
            id = id,
            calendarId = sourceUrl,
            title = title,
            description = description,
            location = location,
            htmlLink = htmlLink,
            start = start,
            endExclusive = endExclusive,
            isAllDay = isAllDay
        )
    }

    /** Returns the text for a required ICS field name. */
    private fun findFieldValue(fields: List<IcsField>, name: String): String {
        return fields.firstOrNull { it.name == name }?.value?.let(::decodeText).orEmpty()
    }

    /** Returns the text for an optional ICS field name. */
    private fun findOptionalFieldValue(fields: List<IcsField>, name: String): String? {
        return fields.firstOrNull { it.name == name }?.value?.let(::decodeText)?.takeIf { it.isNotBlank() }
    }

    /** Decodes the common escaped text sequences used inside ICS files. */
    private fun decodeText(value: String): String {
        return value
            .replace("\\N", "\n")
            .replace("\\n", "\n")
            .replace("\\,", ",")
            .replace("\\;", ";")
            .replace("\\\\", "\\")
    }

    /** Builds a simple ID when the ICS event does not include one. */
    private fun buildFallbackId(startField: IcsField, title: String): String {
        return "${startField.value}-$title"
    }

    /** Checks whether an ICS date field should be treated as all-day. */
    private fun isAllDayField(field: IcsField): Boolean {
        return field.parameters["VALUE"] == "DATE" || field.value.length == 8
    }

    /** Parses an ICS start field into a ZonedDateTime. */
    private fun parseDateTime(field: IcsField): ZonedDateTime {
        return if (isAllDayField(field)) {
            LocalDate.parse(field.value, DateTimeFormatter.BASIC_ISO_DATE)
                .atStartOfDay(resolveZone(field))
        } else {
            parseTimedDateTime(field)
        }
    }

    /** Parses an ICS end field or creates a fallback end time when it is missing. */
    private fun parseEndDateTime(
        startField: IcsField,
        endField: IcsField?,
        start: ZonedDateTime
    ): ZonedDateTime {
        if (endField != null) {
            return parseDateTime(endField)
        }

        return if (isAllDayField(startField)) {
            start.plusDays(1)
        } else {
            start.plusHours(1)
        }
    }

    /** Parses a timed ICS field that may be UTC, local, or tied to a named timezone. */
    private fun parseTimedDateTime(field: IcsField): ZonedDateTime {
        val value = field.value
        val zone = resolveZone(field)

        return when {
            value.endsWith("Z") -> parseUtcDateTime(value.removeSuffix("Z"), zone)
            value.length == 15 -> LocalDateTime.parse(value, LOCAL_DATE_TIME_FORMATTER).atZone(zone)
            value.length == 13 -> LocalDateTime.parse(value, SHORT_LOCAL_DATE_TIME_FORMATTER).atZone(zone)
            else -> throw IllegalStateException("Unsupported ICS date format: $value")
        }
    }

    /** Parses a UTC ICS date-time string and converts it to the display timezone. */
    private fun parseUtcDateTime(value: String, zone: ZoneId): ZonedDateTime {
        val utcDateTime = when (value.length) {
            15 -> LocalDateTime.parse(value, LOCAL_DATE_TIME_FORMATTER)
            13 -> LocalDateTime.parse(value, SHORT_LOCAL_DATE_TIME_FORMATTER)
            else -> throw IllegalStateException("Unsupported UTC ICS date format: $value")
        }

        return utcDateTime.atZone(ZoneId.of("UTC")).withZoneSameInstant(zone)
    }

    /** Resolves the timezone attached to a field or falls back to the device timezone. */
    private fun resolveZone(field: IcsField): ZoneId {
        val timeZoneId = field.parameters["TZID"]
            ?.let(::cleanTimeZoneId)
            ?.takeIf { it.isNotBlank() }

        return if (timeZoneId == null) ZoneId.systemDefault() else ZoneId.of(timeZoneId)
    }

    /** Normalizes a timezone string before it is passed to ZoneId. */
    private fun cleanTimeZoneId(timeZoneId: String): String {
        return timeZoneId.trim().removePrefix("/").removeSurrounding("\"")
    }

    /** Reads an input stream as plain text. */
    private fun InputStream.readText(): String {
        return BufferedReader(InputStreamReader(this)).use { it.readText() }
    }

    private data class IcsField(
        val name: String,
        val value: String,
        val parameters: Map<String, String>
    )

    private companion object {
        val LOCAL_DATE_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")

        val SHORT_LOCAL_DATE_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm")
    }
}
