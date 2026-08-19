package com.selflimit.instagramtimer.data

import android.content.Context
import com.selflimit.instagramtimer.util.TimeUtils

/**
 * Tracks cumulative Instagram usage per window per day. Keys are "<date>|<windowId>"
 * so a day rollover naturally starts every window back at zero; stale-day keys are
 * pruned opportunistically so preferences don't grow unbounded.
 */
class UsageRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getUsedSeconds(windowId: String, date: String = TimeUtils.todayKey()): Int =
        prefs.getInt(key(date, windowId), 0)

    /** Adds [deltaSeconds] to today's usage for [windowId] and returns the new total. */
    fun addUsage(windowId: String, deltaSeconds: Int): Int {
        val date = TimeUtils.todayKey()
        pruneOtherDays(date)
        val newTotal = getUsedSeconds(windowId, date) + deltaSeconds
        prefs.edit().putInt(key(date, windowId), newTotal).apply()
        return newTotal
    }

    private fun pruneOtherDays(currentDate: String) {
        val stale = prefs.all.keys.filter { !it.startsWith("$currentDate|") }
        if (stale.isEmpty()) return
        val editor = prefs.edit()
        stale.forEach { editor.remove(it) }
        editor.apply()
    }

    private fun key(date: String, windowId: String) = "$date|$windowId"

    companion object {
        private const val PREFS_NAME = "usage_tracking"
    }
}
