package com.selflimit.instagramtimer.util

import java.util.Locale

/** 30-minute-step time-of-day options used by the window start/end pickers. */
object TimeSlots {

    /** 00:00 .. 23:30 — a window can start at any half-hour slot except the very end of the day. */
    val startOptions: List<Int> = (0 until 1440 step 30).toList()

    /** 00:30 .. 24:00 — a window's end is exclusive, so it can reach through midnight. */
    val endOptions: List<Int> = (30..1440 step 30).toList()

    fun label(minuteOfDay: Int): String {
        val displayHour = if (minuteOfDay == 1440) 24 else minuteOfDay / 60
        val displayMinute = if (minuteOfDay == 1440) 0 else minuteOfDay % 60
        return String.format(Locale.US, "%02d:%02d", displayHour, displayMinute)
    }
}
