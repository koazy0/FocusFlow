package com.example.focusflow

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.telecom.TelecomManager
import android.util.Log

object FocusGuard {
    // Updated by the activity and checked by the timer on the main thread.
    private var activityInFront = false
    private var nextAttemptAt = 0L
    private val mainHandler = Handler(Looper.getMainLooper())

    fun onForegroundChanged(context: Context, inFront: Boolean) {
        if (activityInFront == inFront) return
        activityInFront = inFront
        nextAttemptAt = 0L
        if (!inFront && FocusState.isFocusing && FocusState.guardEnabled) {
            // Let the lifecycle callback finish before requesting a new foreground transition.
            val appContext = context.applicationContext
            mainHandler.post { returnIfNeeded(appContext) }
        }
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences("focus_options", Context.MODE_PRIVATE)

    fun load(context: Context) {
        FocusState.guardEnabled = preferences(context).getBoolean("strict_focus", true)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        if (FocusState.isFocusing) return
        preferences(context).edit().putBoolean("strict_focus", enabled).apply()
        FocusState.guardEnabled = enabled
    }

    fun needsPhonePermission(context: Context) =
        (context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) ||
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELECOM)) &&
            context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED

    fun needsOverlayPermission(context: Context) =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !Settings.canDrawOverlays(context)

    fun returnIfNeeded(context: Context) {
        if (!FocusState.isFocusing || !FocusState.guardEnabled || activityInFront) return
        if (!context.getSystemService(PowerManager::class.java).isInteractive ||
            context.getSystemService(KeyguardManager::class.java).isKeyguardLocked) return
        val now = SystemClock.elapsedRealtime()
        if (now < nextAttemptAt) return
        try {
            val telecom = context.getSystemService(TelecomManager::class.java)
            if (needsPhonePermission(context)) return
            if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
                telecom?.isInCall == true) return
            nextAttemptAt = now + 1000
            context.startActivity(Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            })
        } catch (e: RuntimeException) {
            Log.w("FocusGuard", "Unable to bring the focus screen forward", e)
        }
    }
}
