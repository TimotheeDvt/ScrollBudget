package com.selflimit.instagramtimer

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.selflimit.instagramtimer.databinding.ActivityPermissionsBinding
import com.selflimit.instagramtimer.util.PermissionUtils

class PermissionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionsBinding

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { refreshStatuses() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.usageAccessButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        binding.accessibilityButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        binding.notificationsButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        binding.batteryButton.setOnClickListener {
            startActivity(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")
                )
            )
        }

        binding.continueButton.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        refreshStatuses()
    }

    private fun refreshStatuses() {
        setStatus(
            binding.usageAccessStatus,
            binding.usageAccessButton,
            PermissionUtils.hasUsageAccess(this)
        )
        setStatus(
            binding.accessibilityStatus,
            binding.accessibilityButton,
            PermissionUtils.isAccessibilityServiceEnabled(this)
        )
        setStatus(
            binding.notificationsStatus,
            binding.notificationsButton,
            PermissionUtils.hasNotificationPermission(this)
        )
        setStatus(
            binding.batteryStatus,
            binding.batteryButton,
            PermissionUtils.isIgnoringBatteryOptimizations(this)
        )
    }

    private fun setStatus(statusView: TextView, button: android.widget.Button, granted: Boolean) {
        if (granted) {
            statusView.text = getString(R.string.status_granted)
            statusView.setTextColor(ContextCompat.getColor(this, R.color.status_granted))
            button.isEnabled = false
        } else {
            statusView.text = getString(R.string.status_not_granted)
            statusView.setTextColor(ContextCompat.getColor(this, R.color.status_not_granted))
            button.isEnabled = true
        }
    }
}
