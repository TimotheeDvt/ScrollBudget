package com.selflimit.instagramtimer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.selflimit.instagramtimer.data.UsageRepository
import com.selflimit.instagramtimer.data.WindowRepository
import com.selflimit.instagramtimer.databinding.ActivityMainBinding
import com.selflimit.instagramtimer.databinding.ItemWindowUsageBinding
import com.selflimit.instagramtimer.service.UsageMonitorService
import com.selflimit.instagramtimer.util.PermissionUtils
import com.selflimit.instagramtimer.util.TimeSlots
import com.selflimit.instagramtimer.util.TimeUtils
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

        binding.startButton.setOnClickListener {
            if (PermissionUtils.hasUsageAccess(this)) {
                UsageMonitorService.start(this)
                Toast.makeText(this, "Monitoring started", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Grant Usage Access first, then tap Start again", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        }

        binding.stopButton.setOnClickListener {
            UsageMonitorService.stop(this)
            Toast.makeText(this, "Monitoring stopped", Toast.LENGTH_SHORT).show()
        }

        maybeLaunchFirstRunPermissionFlow()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    refreshUsage()
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
        var granted = 0
        if (PermissionUtils.hasUsageAccess(this)) granted++
        if (PermissionUtils.isAccessibilityServiceEnabled(this)) granted++
        if (PermissionUtils.hasNotificationPermission(this)) granted++
        if (PermissionUtils.isIgnoringBatteryOptimizations(this)) granted++
        binding.statusText.text = getString(R.string.permissions_status_format, granted, TOTAL_PERMISSIONS)
    }

    private fun refreshUsage() {
        binding.windowsUsageContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)
        val currentMinute = TimeUtils.currentMinuteOfDay()
        val windows = windowRepository.getWindows().sortedBy { it.startMinute }

        for (window in windows) {
            val row = ItemWindowUsageBinding.inflate(inflater, binding.windowsUsageContainer, false)
            val usedMinutes = usageRepository.getUsedSeconds(window.id) / 60
            val remaining = (window.capMinutes - usedMinutes).coerceAtLeast(0)
            row.usageText.text = getString(
                R.string.usage_row_format,
                TimeSlots.label(window.startMinute),
                TimeSlots.label(window.endMinute),
                usedMinutes,
                window.capMinutes,
                remaining
            )
            row.usageText.alpha = if (window.contains(currentMinute)) 1.0f else 0.5f
            binding.windowsUsageContainer.addView(row.root)
        }
    }

    companion object {
        private const val USAGE_REFRESH_INTERVAL_MS = 5000L
        private const val APP_STATE_PREFS = "app_state"
        private const val KEY_ONBOARDING_SHOWN = "onboarding_shown"
        private const val TOTAL_PERMISSIONS = 4
    }
}
