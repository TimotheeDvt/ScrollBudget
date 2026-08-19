package com.selflimit.instagramtimer.data

/**
 * [startMinute, endMinute) in minutes since midnight (0..1440), snapped to :00/:30.
 * endMinute may be 1440 to represent a window that runs through midnight.
 */
data class TimeWindow(
    val id: String,
    val startMinute: Int,
    val endMinute: Int,
    val capMinutes: Int
) {
    fun contains(minuteOfDay: Int): Boolean = minuteOfDay >= startMinute && minuteOfDay < endMinute
}
