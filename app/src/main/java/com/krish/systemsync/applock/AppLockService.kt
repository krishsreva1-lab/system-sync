package com.krish.systemsync.applock

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.krish.systemsync.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class AppLockService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    
    private lateinit var appLockRepository: AppLockRepository
    private lateinit var usageStatsManager: UsageStatsManager

    companion object {
        var isRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        appLockRepository = AppLockRepository(this)
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        
        startMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceJob.cancel()
    }
}
