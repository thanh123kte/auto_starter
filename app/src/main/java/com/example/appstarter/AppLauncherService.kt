package com.example.appstarter

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log

class AppLauncherService : Service() {
    private val TAG = "AppLauncherService"
    private val CHANNEL_ID = "AppStarterChannel"
    private val NOTIFICATION_ID = 1

    private var bootTimerOverlay: FloatingTimerOverlay? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        Log.d(TAG, "Service created and started as foreground")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")

        val handler = Handler(Looper.getMainLooper())

        // Sử dụng thời gian delay đã cài đặt từ SharedPreferences
        val initialDelaySeconds = AppUtils.getInitialDelay(this)
        val betweenAppsDelaySeconds = AppUtils.getBetweenAppsDelay(this)
        val initialDelayMs = initialDelaySeconds * 1000L
        val betweenAppsDelayMs = betweenAppsDelaySeconds * 1000L

        Log.d(TAG, "=== BOOT AUTO LAUNCH SETTINGS ===")
        Log.d(TAG, "Initial delay: ${initialDelaySeconds}s (${initialDelayMs}ms)")
        Log.d(TAG, "Between apps delay: ${betweenAppsDelaySeconds}s (${betweenAppsDelayMs}ms)")
        Log.d(TAG, "================================")

        // Debug all settings
        AppUtils.debugAllSettings(this)

        if (initialDelaySeconds > 0) {
            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                    Settings.canDrawOverlays(this)
                ) {
                    bootTimerOverlay = FloatingTimerOverlay(applicationContext)
                    bootTimerOverlay?.show(initialDelaySeconds.toLong())
                    Log.d(TAG, "Boot timer overlay shown")
                } else {
                    Log.w(TAG, "No overlay permission, skip boot overlay timer")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error showing boot overlay: ${e.message}", e)
            }
        }

        handler.postDelayed({
            val apps = AppUtils.loadSelectedApps(this)
            Log.d(TAG, "Starting ${apps.size} apps after ${initialDelaySeconds}s delay")

            if (apps.isEmpty()) {
                Log.d(TAG, "No apps to launch")
                stopSelf()
                return@postDelayed
            }

            // Update notification to show launching status
            updateNotification("Đang khởi chạy ${apps.size} ứng dụng...")

            apps.forEachIndexed { index, packageName ->
                handler.postDelayed({
                    Log.d(TAG, "Launching app ${index + 1}/${apps.size}: $packageName")
                    launchApp(this, packageName)

                    // Update notification with progress
                    updateNotification("Đã khởi chạy ${index + 1}/${apps.size} ứng dụng")

                    if (index == apps.size - 1) {
                        handler.postDelayed({
                            Log.d(TAG, "All apps launched, stopping service")
                            stopSelf()
                        }, 3000L) // Wait 3s before stopping service
                    }
                }, (index * betweenAppsDelayMs))
            }
        }, initialDelayMs) // Sử dụng thời gian delay đã cài đặt

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Starter Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Service for launching selected apps on boot"
                setShowBadge(false)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val initialDelay = AppUtils.getInitialDelay(this)
        val betweenDelay = AppUtils.getBetweenAppsDelay(this)
        val appsCount = AppUtils.loadSelectedApps(this).size

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("🚀 App Starter")
                .setContentText("Sẽ khởi chạy ${appsCount} apps sau ${initialDelay}s")
                .setSmallIcon(android.R.drawable.ic_menu_rotate)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("🚀 App Starter")
                .setContentText("Sẽ khởi chạy ${appsCount} apps sau ${initialDelay}s")
                .setSmallIcon(android.R.drawable.ic_menu_rotate)
                .setOngoing(true)
                .build()
        }
    }

    private fun updateNotification(message: String) {
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("🚀 App Starter")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_menu_rotate)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("🚀 App Starter")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_menu_rotate)
                .setOngoing(true)
                .build()
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, notification)
    }

    private fun launchApp(context: Context, packageName: String) {
        try {
            if (!AppUtils.isAppInstalled(context, packageName)) {
                Log.w(TAG, "App not installed: $packageName")
                return
            }

            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                )
                context.startActivity(launchIntent)
                Log.d(TAG, "Successfully launched app: $packageName")
            } else {
                Log.w(TAG, "Cannot find launch intent for: $packageName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error launching app $packageName", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        bootTimerOverlay?.dismiss()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}