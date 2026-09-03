package com.krish.systemsync.applock

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.krish.systemsync.MainActivity
import com.krish.systemsync.settings.SettingsManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class AppLockService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    
    private lateinit var appLockRepository: AppLockRepository
    private lateinit var usageStatsManager: UsageStatsManager

    companion object {
        const val CHANNEL_ID = "AppLockChannel"
        var isRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        appLockRepository = AppLockRepository(this)
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        createNotificationChannel()
        
        serviceScope.launch {
            val settingsManager = SettingsManager(this@AppLockService)
            val appName = settingsManager.appNameFlow.first()
            startForeground(1, createNotification(appName))
        }
        
        startMonitoring()
    }

    private fun startMonitoring() {
        serviceScope.launch {
            while (isActive) {
                val foregroundApp = getForegroundApp()
                if (foregroundApp != null && foregroundApp != packageName) {
                    val lockedApps = appLockRepository.lockedApps.first()
                    if (lockedApps.contains(foregroundApp)) {
                        val intent = Intent(this@AppLockService, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            putExtra("LOCK_APP_PACKAGE", foregroundApp)
                        }
                        startActivity(intent)
                    }
                }
                delay(200)
            }
        }
    }

    private fun getForegroundApp(): String? {
        val time = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 10, time)
        return stats?.maxByOrNull { it.lastTimeUsed }?.packageName
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "App Lock Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(appName: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("$appName App Locker")
            .setContentText("Monitoring protected apps...")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceJob.cancel()
    }
}
