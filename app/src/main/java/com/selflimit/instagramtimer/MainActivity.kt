package com.selflimit.instagramtimer

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.selflimit.instagramtimer.data.UsageRepository
import com.selflimit.instagramtimer.data.WindowRepository
import com.selflimit.instagramtimer.databinding.ActivityMainBinding
import com.selflimit.instagramtimer.service.UsageMonitorService
import com.selflimit.instagramtimer.util.PermissionUtils
import com.selflimit.instagramtimer.util.TimeUtils
import com.selflimit.instagramtimer.view.DayScheduleView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var windowRepository: WindowRepository
    private lateinit var usageRepository: UsageRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        windowRepository = WindowRepository(this)
        usageRepository = UsageRepository(this)

        binding.editWindowsButton.setOnClickListener {
            startActivity(Intent(this, WindowsActivity::class.java))
        }

        binding.reviewPermissionsButton.setOnClickListener {
            startActivity(Intent(this, PermissionsActivity::class.java))
        }

        maybeLaunchFirstRunPermissionFlow()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    refreshDaySchedule()
                    delay(USAGE_REFRESH_INTERVAL_MS)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionsStatus()

        if (PermissionUtils.hasUsageAccess(this)) {
            UsageMonitorService.start(this)
        }
        binding.monitorToggle.setOnCheckedChangeListener(null)
        binding.monitorToggle.isChecked = UsageMonitorService.isRunning
        binding.monitorToggle.setOnCheckedChangeListener { toggle, isChecked ->
            if (isChecked) {
                if (PermissionUtils.hasUsageAccess(this)) {
                    UsageMonitorService.start(this)
                    Toast.makeText(this, "Monitoring started", Toast.LENGTH_SHORT).show()
                } else {
                    toggle.isChecked = false
                    Toast.makeText(this, "Grant Usage Access first, then try again", Toast.LENGTH_LONG).show()
                    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            } else {
                UsageMonitorService.stop(this)
                Toast.makeText(this, "Monitoring stopped", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun maybeLaunchFirstRunPermissionFlow() {
        val prefs = getSharedPreferences(APP_STATE_PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_ONBOARDING_SHOWN, false)) return
        prefs.edit().putBoolean(KEY_ONBOARDING_SHOWN, true).apply()
        if (!PermissionUtils.allGranted(this)) {
            startActivity(Intent(this, PermissionsActivity::class.java))
        }
    }

    private fun refreshPermissionsStatus() {
        setDotStatus(binding.dotUsageAccess, PermissionUtils.hasUsageAccess(this))
        setDotStatus(binding.dotAccessibility, PermissionUtils.isAccessibilityServiceEnabled(this))
        setDotStatus(binding.dotNotifications, PermissionUtils.hasNotificationPermission(this))
        setDotStatus(binding.dotBattery, PermissionUtils.isIgnoringBatteryOptimizations(this))
    }

    private fun setDotStatus(dot: android.view.View, granted: Boolean) {
        val colorRes = if (granted) R.color.status_granted else R.color.status_not_granted
        dot.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, colorRes))
    }

    private fun refreshDaySchedule() {
        val currentMinute = TimeUtils.currentMinuteOfDay()
        val windows = windowRepository.getWindows().sortedBy { it.startMinute }
        val entries = windows.map { window ->
            DayScheduleView.Entry(window, usageRepository.getUsedSeconds(window.id) / 60)
        }
        binding.dayScheduleView.setData(entries, currentMinute)
    }

    companion object {
        private const val USAGE_REFRESH_INTERVAL_MS = 5000L
        private const val APP_STATE_PREFS = "app_state"
        private const val KEY_ONBOARDING_SHOWN = "onboarding_shown"
    }
}
