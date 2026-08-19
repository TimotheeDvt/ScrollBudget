package com.selflimit.instagramtimer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.selflimit.instagramtimer.util.PermissionUtils

/**
 * Restarts monitoring after a device reboot, so the user doesn't have to reopen
 * the app for tracking to resume. Starting a foreground service from a
 * BOOT_COMPLETED receiver is one of Android's documented exemptions to the
 * background-start restrictions.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (PermissionUtils.hasUsageAccess(context)) {
            UsageMonitorService.start(context)
        }
    }
}
