package com.selflimit.instagramtimer

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

        // Auto-start so the user doesn't have to tap Start every time they open the
        // app; harmless to call repeatedly since the monitor service no-ops if its
        // polling loop is already running. Boot persistence is handled separately
        // by BootCompletedReceiver.
        if (PermissionUtils.hasUsageAccess(this)) {
            UsageMonitorService.start(this)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    refreshUsage()
                    delay(USAGE_REFRESH_INTERVAL_MS)
                }
            }
        }
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
    }
}
