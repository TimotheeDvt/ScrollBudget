package com.selflimit.instagramtimer

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.selflimit.instagramtimer.databinding.ActivityMainBinding
import com.selflimit.instagramtimer.service.UsageMonitorService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.startButton.setOnClickListener {
            if (hasUsageAccess()) {
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
    }

    private fun hasUsageAccess(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
