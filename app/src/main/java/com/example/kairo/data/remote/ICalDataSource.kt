package com.example.kairo.data.remote

import com.example.kairo.domain.model.CalendarEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class ICalDataSource(private val client: OkHttpClient = OkHttpClient()) {

    suspend fun fetchEvents(url: String): List<CalendarEvent> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext emptyList()
            parseICal(body)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun parseICal(icalString: String): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        
        // Unfold folded lines (RFC 5545 line folding)
        val unfoldedLines = mutableListOf<String>()
        for (line in icalString.lines()) {
            if ((line.startsWith(" ") || line.startsWith("\t")) && unfoldedLines.isNotEmpty()) {
                val last = unfoldedLines.removeAt(unfoldedLines.size - 1)
                unfoldedLines.add(last + line.substring(1))
            } else {
                unfoldedLines.add(line)
            }
        }

        var inEvent = false
        var title = ""
        var startTime = 0L
        var endTime = 0L
        var location: String? = null
        var description: String? = null
        var rrule: String? = null

        for (line in unfoldedLines) {
            val cleanLine = line.trim()
            if (cleanLine.equals("BEGIN:VEVENT", ignoreCase = true)) {
                inEvent = true
                title = "Event"
                startTime = 0L
                endTime = 0L
                location = null
                description = null
                rrule = null
                continue
            }

            if (cleanLine.equals("END:VEVENT", ignoreCase = true)) {
                if (inEvent && startTime > 0) {
                    if (endTime == 0L || endTime < startTime) {
                        endTime = startTime + (60 * 60 * 1000L) // Default 1 hour
                    }

                    if (rrule != null) {
                        // Expand recurring event instances for up to 1 year
                        val instances = expandRRule(title, startTime, endTime, location, description, rrule)
                        events.addAll(instances)
                    } else {
                        val deterministicId = kotlin.math.abs((title + startTime).hashCode().toLong())
                        events.add(CalendarEvent(deterministicId, title, startTime, endTime, location, description))
                    }
                }
                inEvent = false
                continue
            }

            if (!inEvent) continue

            val colonIdx = cleanLine.indexOf(':')
            if (colonIdx == -1) continue

            val keyPart = cleanLine.substring(0, colonIdx).uppercase()
            val valuePart = cleanLine.substring(colonIdx + 1).trim()

            when {
                keyPart == "SUMMARY" || keyPart.startsWith("SUMMARY;") -> title = valuePart
                keyPart == "DTSTART" || keyPart.startsWith("DTSTART;") -> startTime = parseICalDate(valuePart)
                keyPart == "DTEND" || keyPart.startsWith("DTEND;") -> endTime = parseICalDate(valuePart)
                keyPart == "LOCATION" || keyPart.startsWith("LOCATION;") -> location = valuePart
                keyPart == "DESCRIPTION" || keyPart.startsWith("DESCRIPTION;") -> description = valuePart
                keyPart == "RRULE" || keyPart.startsWith("RRULE;") -> rrule = valuePart
            }
        }

        return events
    }

    private fun expandRRule(
        title: String,
        startTime: Long,
        endTime: Long,
        location: String?,
        description: String?,
        rrule: String
    ): List<CalendarEvent> {
        val instances = mutableListOf<CalendarEvent>()
        val duration = endTime - startTime

        // Parse RRULE parameters
        val ruleMap = rrule.split(";").mapNotNull {
            val parts = it.split("=")
            if (parts.size == 2) parts[0].uppercase() to parts[1].uppercase() else null
        }.toMap()

        val freq = ruleMap["FREQ"] ?: "WEEKLY"
        val untilStr = ruleMap["UNTIL"]
        val untilTime = if (untilStr != null) parseICalDate(untilStr) else (startTime + (365L * 24 * 60 * 60 * 1000L)) // 1 year limit
        val count = ruleMap["COUNT"]?.toIntOrNull() ?: 100

        val cal = Calendar.getInstance().apply { timeInMillis = startTime }
        var generatedCount = 0

        while (cal.timeInMillis <= untilTime && generatedCount < count) {
            val currStart = cal.timeInMillis
            val currEnd = currStart + duration
            val deterministicId = kotlin.math.abs((title + currStart).hashCode().toLong())

            instances.add(CalendarEvent(deterministicId, title, currStart, currEnd, location, description))
            generatedCount++

            when (freq) {
                "DAILY" -> cal.add(Calendar.DAY_OF_YEAR, 1)
                "WEEKLY" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                "MONTHLY" -> cal.add(Calendar.MONTH, 1)
                "YEARLY" -> cal.add(Calendar.YEAR, 1)
                else -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            }
        }

        return instances
    }

    private fun parseICalDate(dateStr: String): Long {
        val cleanStr = dateStr.trim()
        val formats = arrayOf(
            SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
            SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US).apply { timeZone = TimeZone.getDefault() },
            SimpleDateFormat("yyyyMMdd", Locale.US).apply { timeZone = TimeZone.getDefault() }
        )

        for (format in formats) {
            try {
                val date = format.parse(cleanStr)
                if (date != null) return date.time
            } catch (e: Exception) {
                // Try next format
            }
        }
        return 0L
    }
}
