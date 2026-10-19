package com.example.kairo.data.local

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.CalendarContract
import com.example.kairo.domain.model.CalendarEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CalendarProviderDataSource(private val context: Context) {

    suspend fun getEvents(startTime: Long, endTime: Long): List<CalendarEvent> = withContext(Dispatchers.IO) {
        val events = mutableListOf<CalendarEvent>()
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.DESCRIPTION
        )

        val selection = "((${CalendarContract.Events.DTSTART} >= ?) AND (${CalendarContract.Events.DTSTART} <= ?))"
        val selectionArgs = arrayOf(startTime.toString(), endTime.toString())

        val contentResolver: ContentResolver = context.contentResolver
        val uri = CalendarContract.Events.CONTENT_URI

        try {
            val cursor: Cursor? = contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Events.DTSTART} ASC"
            )

            cursor?.use {
                val idIndex = it.getColumnIndexOrThrow(CalendarContract.Events._ID)
                val titleIndex = it.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
                val startIndex = it.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
                val endIndex = it.getColumnIndexOrThrow(CalendarContract.Events.DTEND)
                val locationIndex = it.getColumnIndexOrThrow(CalendarContract.Events.EVENT_LOCATION)
                val descIndex = it.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION)

                while (it.moveToNext()) {
                    val id = it.getLong(idIndex)
                    val title = it.getString(titleIndex) ?: "No Title"
                    val eventStart = it.getLong(startIndex)
                    val eventEnd = it.getLong(endIndex)
                    val location = it.getString(locationIndex)
                    val description = it.getString(descIndex)

                    events.add(CalendarEvent(id, title, eventStart, eventEnd, location, description))
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            // Permissions should be handled at the UI layer
        }

        events
    }
}
