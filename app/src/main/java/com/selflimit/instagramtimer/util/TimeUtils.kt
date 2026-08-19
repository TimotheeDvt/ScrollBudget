package com.selflimit.instagramtimer.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object TimeUtils {

    fun currentMinuteOfDay(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    }

    // SimpleDateFormat is not thread-safe; the monitor service (background) and UI
    // (main thread) can both call this, so a fresh instance per call is required.
    fun todayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
}
