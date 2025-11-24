package com.d4viddf.hyperbridge.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.models.HyperIslandData
import com.d4viddf.hyperbridge.service.translators.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotificationReaderService : NotificationListenerService() {

    private val TAG = "HyperBridgeService"
    private val ISLAND_CHANNEL_ID = "hyper_bridge_island_channel"

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var allowedPackageSet: Set<String> = emptySet()
    private val activeTranslations = mutableMapOf<String, Int>()

    // Performance: Rate Limiting Map (Key -> Last Update Timestamp)
    private val lastUpdateMap = mutableMapOf<String, Long>()
    private val UPDATE_INTERVAL_MS = 300L // Max 3 updates per second per app

    // Translators
    private lateinit var progressTranslator: ProgressTranslator
    private lateinit var navTranslator: NavTranslator
    private lateinit var timerTranslator: TimerTranslator
    private lateinit var standardTranslator: StandardTranslator

    override fun onCreate() {
        super.onCreate()
        createIslandChannel()
        progressTranslator = ProgressTranslator(this)
        navTranslator = NavTranslator(this)
        timerTranslator = TimerTranslator(this)
        standardTranslator = StandardTranslator(this)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "HyperBridge Connected")
        val preferences = AppPreferences(this)
        serviceScope.launch {
            preferences.allowedPackagesFlow.collectLatest { allowedPackageSet = it }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn?.let {
            // 1. BLACKLIST: Critical System Apps & Self
            if (shouldIgnore(it.packageName)) return

            // 2. USER ALLOW LIST
            if (isAppAllowed(it.packageName)) {

                // 3. RATE LIMITER (Performance Fix)
                if (shouldRateLimit(it)) return

                translateAndPost(it)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn?.let {
            val key = it.key
            if (activeTranslations.containsKey(key)) {
                val hyperId = activeTranslations[key] ?: return
                try {
                    NotificationManagerCompat.from(this).cancel(hyperId)
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing island", e)
                }
                activeTranslations.remove(key)
                lastUpdateMap.remove(key) // Clear cache
            }
        }
    }

    /**
     * Determines if we should skip this update to save battery/CPU.
     */
    private fun shouldRateLimit(sbn: StatusBarNotification): Boolean {
        val key = sbn.key
        val now = System.currentTimeMillis()
        val lastTime = lastUpdateMap[key] ?: 0L

        // Always allow if enough time has passed
        if (now - lastTime > UPDATE_INTERVAL_MS) {
            lastUpdateMap[key] = now
            return false
        }

        // Check if it's a "Progress" notification (High Frequency)
        val extras = sbn.notification.extras
        val isProgress = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) > 0

        if (isProgress) {
            // Skip this frame (download is updating too fast)
            return true
        }

        // If not progress (e.g. text changed), allow immediately
        lastUpdateMap[key] = now
        return false
    }

    private fun shouldIgnore(packageName: String): Boolean {
        return packageName == this.packageName || // Don't bridge ourselves
                packageName == "android" ||        // System Interface
                packageName == "com.android.systemui" ||
                packageName.contains("miui.notification")
    }

    private fun translateAndPost(sbn: StatusBarNotification) {
        try {
            val extras = sbn.notification.extras
            val title = extras.getString(Notification.EXTRA_TITLE) ?: sbn.packageName

            val bridgeId = sbn.key.hashCode()
            val picKey = "pic_${bridgeId}"

            // --- DETECTION ---
            val isNavigation = sbn.notification.category == Notification.CATEGORY_NAVIGATION ||
                    sbn.packageName.contains("maps") ||
                    sbn.packageName.contains("waze")

            val progressMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
            val isIndeterminate = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE)
            val hasProgress = (progressMax > 0) || isIndeterminate

            val usesChronometer = extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER)
            val chronometerBase = sbn.notification.`when`

            // --- ROUTING ---
            val data: HyperIslandData = when {
                isNavigation -> navTranslator.translate(sbn, picKey)
                usesChronometer && chronometerBase > 0 -> timerTranslator.translate(sbn, picKey)
                hasProgress -> progressTranslator.translate(sbn, title, picKey)
                else -> standardTranslator.translate(sbn, picKey)
            }

            // --- POSTING ---
            val notificationBuilder = NotificationCompat.Builder(this, ISLAND_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("HyperBridge")
                .setContentText("Active")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .addExtras(data.resources)

            sbn.notification.contentIntent?.let { notificationBuilder.setContentIntent(it) }

            val notification = notificationBuilder.build()
            notification.extras.putString("miui.focus.param", data.jsonParam)

            NotificationManagerCompat.from(this).notify(bridgeId, notification)
            activeTranslations[sbn.key] = bridgeId

        } catch (e: Exception) {
            Log.e(TAG, "Translation failed for ${sbn.packageName}", e)
        }
    }

    private fun createIslandChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ISLAND_CHANNEL_ID, "Active Islands", NotificationManager.IMPORTANCE_HIGH
            ).apply { setSound(null, null); enableVibration(false) }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun isAppAllowed(packageName: String): Boolean {
        return allowedPackageSet.contains(packageName)
    }
}