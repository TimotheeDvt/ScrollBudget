package com.selflimit.instagramtimer.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

/**
 * Detection of "is Instagram in foreground" happens in [UsageMonitorService] via
 * UsageStatsManager. This service's only job is performing the enforcement action
 * (go home) and showing a short explanation, on request from that service.
 */
class InstagramAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No-op: this service doesn't use accessibility events for detection.
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        // Fires when the system unbinds this service, e.g. the user disabled it in Settings.
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    fun goHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    fun showBlockedMessage(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        var instance: InstagramAccessibilityService? = null
            private set
    }
}
