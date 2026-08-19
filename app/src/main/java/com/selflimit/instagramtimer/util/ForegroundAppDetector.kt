package com.selflimit.instagramtimer.util

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager

object ForegroundAppDetector {

    /**
     * Returns the package currently in the foreground, or null if none could be
     * determined (e.g. usage access not granted, or the last event was a
     * move-to-background). Looks back [lookbackMillis] to find the most recent
     * foreground/background transition.
     */
    fun currentForegroundPackage(usageStatsManager: UsageStatsManager, lookbackMillis: Long): String? {
        val end = System.currentTimeMillis()
        val begin = end - lookbackMillis
        val events = usageStatsManager.queryEvents(begin, end)

        var lastPackage: String? = null
        var lastEventType = -1
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                event.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND
            ) {
                lastPackage = event.packageName
                lastEventType = event.eventType
            }
        }

        return if (lastEventType == UsageEvents.Event.MOVE_TO_FOREGROUND) lastPackage else null
    }
}
