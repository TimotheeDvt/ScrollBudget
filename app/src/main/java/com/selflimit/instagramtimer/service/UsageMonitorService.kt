package com.selflimit.instagramtimer.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.selflimit.instagramtimer.R
import com.selflimit.instagramtimer.data.UsageRepository
import com.selflimit.instagramtimer.data.WindowRepository
import com.selflimit.instagramtimer.util.ForegroundAppDetector
import com.selflimit.instagramtimer.util.TimeSlots
import com.selflimit.instagramtimer.util.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class UsageMonitorService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null

    private lateinit var windowRepository: WindowRepository
    private lateinit var usageRepository: UsageRepository
    private lateinit var usageStatsManager: UsageStatsManager

    override fun onCreate() {
        super.onCreate()
        windowRepository = WindowRepository(applicationContext)
        usageRepository = UsageRepository(applicationContext)
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        createNotificationChannel()
        isRunning = true
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val (title, text) = currentNotificationContent()
        startForeground(NOTIFICATION_ID, buildNotification(title, text))
        startMonitoring()
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        scope.cancel()
        super.onDestroy()
    }

    private fun startMonitoring() {
        if (monitorJob?.isActive == true) return
        monitorJob = scope.launch {
            while (isActive) {
                pollOnce()
                updateNotification()
                delay(POLL_INTERVAL_SECONDS * 1000L)
            }
        }
    }

    private fun pollOnce() {
        val foregroundPackage = ForegroundAppDetector.currentForegroundPackage(usageStatsManager, LOOKBACK_MILLIS)
        if (foregroundPackage != INSTAGRAM_PACKAGE) return

        val window = windowRepository.activeWindowFor(TimeUtils.currentMinuteOfDay()) ?: return
        val usedSeconds = usageRepository.addUsage(window.id, POLL_INTERVAL_SECONDS.toInt())

        if (usedSeconds >= window.capMinutes * 60) {
            enforceLimit(window.capMinutes)
        }
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        val (title, text) = currentNotificationContent()
        manager.notify(NOTIFICATION_ID, buildNotification(title, text))
    }

    private fun currentNotificationContent(): Pair<String, String> {
        val window = windowRepository.activeWindowFor(TimeUtils.currentMinuteOfDay())
            ?: return getString(R.string.notification_title) to getString(R.string.notification_text_no_window)
        val usedMinutes = usageRepository.getUsedSeconds(window.id) / 60
        val remaining = (window.capMinutes - usedMinutes).coerceAtLeast(0)
        val title = getString(
            R.string.notification_window_title,
            TimeSlots.label(window.startMinute),
            TimeSlots.label(window.endMinute)
        )
        val text = getString(R.string.notification_text_with_window, remaining, window.capMinutes)
        return title to text
    }

    private fun enforceLimit(capMinutes: Int) {
        val accessibilityService = InstagramAccessibilityService.instance ?: return
        accessibilityService.goHome()
        accessibilityService.showBlockedMessage(getString(R.string.limit_reached_message, capMinutes))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = getString(R.string.notification_channel_description)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(title: String, contentText: String) =
        NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "usage_monitor_v2"
        private const val NOTIFICATION_ID = 1001
        private const val INSTAGRAM_PACKAGE = "com.instagram.android"
        private const val POLL_INTERVAL_SECONDS = 30L
        private const val LOOKBACK_MILLIS = 60 * 60 * 1000L

        /** In-memory flag reflecting whether the service is currently alive in this process. */
        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, UsageMonitorService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, UsageMonitorService::class.java))
        }
    }
}
