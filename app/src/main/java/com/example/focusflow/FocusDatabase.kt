package com.example.focusflow

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.Calendar

data class FocusSession(
    val id: Long,
    val startTime: Long,
    val endTime: Long,
    val durationMillis: Long
)

class FocusDatabase(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                start_time INTEGER NOT NULL,
                end_time INTEGER NOT NULL,
                duration INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE")
        onCreate(db)
    }

    fun insert(startTime: Long, endTime: Long, duration: Long) {
        writableDatabase.use { db ->
            val values = ContentValues().apply {
                put("start_time", startTime)
                put("end_time", endTime)
                put("duration", duration)
            }
            db.insert(TABLE, null, values)
        }
    }

    fun allSessions(): List<FocusSession> {
        val result = mutableListOf<FocusSession>()
        readableDatabase.use { db ->
            db.query(TABLE, null, null, null, null, null, "start_time DESC").use { cursor ->
                while (cursor.moveToNext()) {
                    result.add(cursor.toSession())
                }
            }
        }
        return result
    }

    fun dailyTotals(days: Int): Map<String, Long> {
        val result = linkedMapOf<String, Long>()
        for (offset in days - 1 downTo 0) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -offset) }
            result[dateKeyOf(cal.timeInMillis)] = 0L
        }
        readableDatabase.use { db ->
            db.query(TABLE, arrayOf("start_time", "duration"), null, null, null, null, "start_time ASC")
                .use { cursor ->
                    while (cursor.moveToNext()) {
                        val key = dateKeyOf(cursor.getLong(0))
                        if (result.containsKey(key)) {
                            result[key] = result[key]!! + cursor.getLong(1)
                        }
                    }
                }
        }
        return result
    }

    fun totalMillis(): Long {
        readableDatabase.use { db ->
            db.rawQuery("SELECT COALESCE(SUM(duration),0) FROM $TABLE", null).use { cursor ->
                return if (cursor.moveToFirst()) cursor.getLong(0) else 0L
            }
        }
    }

    private fun Cursor.toSession() = FocusSession(
        id = getLong(getColumnIndexOrThrow("_id")),
        startTime = getLong(getColumnIndexOrThrow("start_time")),
        endTime = getLong(getColumnIndexOrThrow("end_time")),
        durationMillis = getLong(getColumnIndexOrThrow("duration"))
    )

    companion object {
        private const val DB_NAME = "focusflow.db"
        private const val DB_VERSION = 1
        private const val TABLE = "sessions"

        fun dateKeyOf(timeMillis: Long): String {
            val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
            return "%04d-%02d-%02d".format(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )
        }
    }
}
