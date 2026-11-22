package com.example.focusflow

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val requestPhoneState =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                toast("电话状态权限已就绪，请再次点开始专注")
            } else {
                toast("强制专注需要识别来电，可在应用权限中开启电话权限，或关闭防切出保护")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        FocusGuard.load(this)
        FocusController.refreshStats(this)
        setContent { FocusFlowApp(::toggleSession) }
    }

    private fun toggleSession() {
        when {
            FocusState.isFocusing -> FocusController.stop(this)
            FocusState.guardEnabled && FocusGuard.needsPhonePermission(this) ->
                requestPhoneState.launch(Manifest.permission.READ_PHONE_STATE)
            FocusState.guardEnabled && FocusGuard.needsOverlayPermission(this) -> {
                toast("请允许在其他应用上层显示，再返回开始专注，以便切出后自动回来")
                try {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + packageName)))
                } catch (e: android.content.ActivityNotFoundException) {
                    toast("请在系统设置里为 FocusFlow 开启悬浮窗权限")
                }
            }
            else -> FocusController.start(applicationContext)
        }
    }

    override fun onResume() {
        super.onResume()
        FocusGuard.onForegroundChanged(this, true)
    }

    override fun onPause() {
        super.onPause()
        FocusGuard.onForegroundChanged(this, false)
    }

    override fun onTopResumedActivityChanged(isTopResumedActivity: Boolean) {
        super.onTopResumedActivityChanged(isTopResumedActivity)
        FocusGuard.onForegroundChanged(this, isTopResumedActivity)
    }

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
