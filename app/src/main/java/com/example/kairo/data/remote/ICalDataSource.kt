package com.example.kairo.data.remote

import com.example.kairo.domain.model.CalendarEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
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

    private fun parseICal(icalString: String): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        val lines = icalString.lines()
        var inEvent = false
        var title = ""
        var startTime = 0L
        var endTime = 0L
        var location: String? = null
        var description: String? = null

        for (line in lines) {
            val cleanLine = line.trim()
            when {
                cleanLine.startsWith("BEGIN:VEVENT") -> {
                    inEvent = true
                    title = "Event"
                    startTime = 0L
                    endTime = 0L
                    location = null
                    description = null
                }
                cleanLine.startsWith("END:VEVENT") -> {
                    if (inEvent && startTime > 0) {
                        if (endTime == 0L) endTime = startTime + 3600000 // default 1h
                        val deterministicId = kotlin.math.abs((title + startTime).hashCode().toLong())
                        events.add(CalendarEvent(deterministicId, title, startTime, endTime, location, description))
                    }
                    inEvent = false
                }
                inEvent && cleanLine.startsWith("SUMMARY:") -> title = cleanLine.substringAfter("SUMMARY:")
                inEvent && cleanLine.startsWith("DTSTART") -> startTime = parseICalDate(cleanLine.substringAfter(":"))
                inEvent && cleanLine.startsWith("DTEND") -> endTime = parseICalDate(cleanLine.substringAfter(":"))
                inEvent && cleanLine.startsWith("LOCATION:") -> location = cleanLine.substringAfter("LOCATION:")
                inEvent && cleanLine.startsWith("DESCRIPTION:") -> description = cleanLine.substringAfter("DESCRIPTION:")
            }
        }
        return events
    }

    private fun parseICalDate(dateStr: String): Long {
        val cleanStr = dateStr.trim()
        try {
            val format = if (cleanStr.endsWith("Z")) {
                SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            } else if (cleanStr.contains("T")) {
                SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US).apply { timeZone = TimeZone.getDefault() }
            } else {
                SimpleDateFormat("yyyyMMdd", Locale.US).apply { timeZone = TimeZone.getDefault() }
            }
            return format.parse(cleanStr)?.time ?: 0L
        } catch (e: Exception) {
            return 0L
        }
    }
}
