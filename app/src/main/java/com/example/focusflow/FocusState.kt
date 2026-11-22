package com.example.focusflow

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.concurrent.Executors

object FocusState {
    var isFocusing by mutableStateOf(false)
    var elapsedSeconds by mutableLongStateOf(0L)
    var guardEnabled by mutableStateOf(false)
    var totalSeconds by mutableLongStateOf(0L)
    var todaySeconds by mutableLongStateOf(0L)
    var dailyTotals by mutableStateOf<Map<String, Long>>(emptyMap())
    var sessions by mutableStateOf<List<FocusSession>>(emptyList())
}

object FocusController {
    private val executor = Executors.newSingleThreadExecutor()
    private var handler: Handler? = null
    private var tick: Runnable? = null
    private var startElapsed = 0L
    private var database: FocusDatabase? = null

    fun start(context: Context) {
        if (FocusState.isFocusing) return
        context.applicationContext.startForegroundService(
            Intent(context.applicationContext, FocusService::class.java)
        )
        database = FocusDatabase(context.applicationContext)
        startElapsed = SystemClock.elapsedRealtime()
        FocusState.isFocusing = true
        FocusState.elapsedSeconds = 0L

        val h = Handler(Looper.getMainLooper())
        handler = h
        val runnable = object : Runnable {
            override fun run() {
                val seconds = (SystemClock.elapsedRealtime() - startElapsed) / 1000
                FocusState.elapsedSeconds = seconds
                FocusGuard.returnIfNeeded(context.applicationContext)
                h.postDelayed(this, 1000)
            }
        }
        tick = runnable
        h.post(runnable)

    }

    fun stop(context: Context) {
        if (!FocusState.isFocusing) return
        val appContext = context.applicationContext
        val duration = SystemClock.elapsedRealtime() - startElapsed
        tick?.let { handler?.removeCallbacks(it) }
        handler = null
        tick = null
        FocusState.isFocusing = false
        FocusState.elapsedSeconds = 0L
        try {
            appContext.stopService(Intent(appContext, FocusService::class.java))
        } catch (e: Exception) {
            // 忽略停止服务时的系统异常
        }

        val db = database
        database = null
        executor.execute {
            if (db != null) {
                try {
                    db.insert(
                        System.currentTimeMillis() - duration,
                        System.currentTimeMillis(),
                        duration
                    )
                } catch (e: Exception) {
                    // 忽略数据库写入异常
                } finally {
                    db.close()
                }
            }
            refreshStats(appContext)
        }
    }

    fun refreshStats(context: Context) {
        val appContext = context.applicationContext
        executor.execute {
            val db = FocusDatabase(appContext)
            try {
                val totals = db.dailyTotals(7)
                val today = db.dailyTotals(1).values.sum()
                val total = db.totalMillis()
                val sessions = db.allSessions()
                FocusState.dailyTotals = totals
                FocusState.todaySeconds = today / 1000
                FocusState.totalSeconds = total / 1000
                FocusState.sessions = sessions
            } catch (e: Exception) {
                // 忽略统计读取异常
            } finally {
                db.close()
            }
        }
    }
}
