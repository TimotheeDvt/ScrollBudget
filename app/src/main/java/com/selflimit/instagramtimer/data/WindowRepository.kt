package com.selflimit.instagramtimer.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Stores the user's configurable time windows. Phase 3 adds the UI to edit these;
 * this repository is the single source of truth the background service reads live.
 */
class WindowRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getWindows(): List<TimeWindow> {
        val raw = prefs.getString(KEY_WINDOWS, null)
        if (raw == null) {
            saveWindows(DEFAULT_WINDOWS)
            return DEFAULT_WINDOWS
        }
        return parse(raw)
    }

    fun saveWindows(windows: List<TimeWindow>) {
        val array = JSONArray()
        for (window in windows) {
            val obj = JSONObject()
            obj.put("id", window.id)
            obj.put("startMinute", window.startMinute)
            obj.put("endMinute", window.endMinute)
            obj.put("capMinutes", window.capMinutes)
            array.put(obj)
        }
        prefs.edit().putString(KEY_WINDOWS, array.toString()).apply()
    }

    fun activeWindowFor(minuteOfDay: Int): TimeWindow? = getWindows().find { it.contains(minuteOfDay) }

    private fun parse(raw: String): List<TimeWindow> {
        val array = JSONArray(raw)
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            TimeWindow(
                id = obj.getString("id"),
                startMinute = obj.getInt("startMinute"),
                endMinute = obj.getInt("endMinute"),
                capMinutes = obj.getInt("capMinutes")
            )
        }
    }

    companion object {
        private const val PREFS_NAME = "window_config"
        private const val KEY_WINDOWS = "windows_json"

        val DEFAULT_WINDOWS = listOf(
            TimeWindow(id = "default-morning", startMinute = 0, endMinute = 600, capMinutes = 20),
            TimeWindow(id = "default-day", startMinute = 600, endMinute = 1080, capMinutes = 30),
            TimeWindow(id = "default-evening", startMinute = 1080, endMinute = 1440, capMinutes = 20)
        )
    }
}
