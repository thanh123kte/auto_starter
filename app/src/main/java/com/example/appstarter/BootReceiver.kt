package com.example.appstarter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    private val TAG = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Boot completed received: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                // Debug settings khi boot
                Log.d(TAG, "=== BOOT RECEIVER DEBUG ===")
                AppUtils.debugAllSettings(context)
                val initialDelay = AppUtils.getInitialDelay(context)
                val betweenDelay = AppUtils.getBetweenAppsDelay(context)
                val savedApps = AppUtils.loadSelectedApps(context)
                Log.d(TAG, "Will launch ${savedApps.size} apps with ${initialDelay}s initial delay, ${betweenDelay}s between apps")
                Log.d(TAG, "Apps to launch: $savedApps")
                Log.d(TAG, "========================")

                // Khởi động AppLauncherService trước tiên (MỨC ƯU TIÊN CAO)
                Log.d(TAG, "Starting AppLauncherService immediately...")
                startAppLauncherService(context)

                // Mở MainActivity của app (tùy chọn) - Không bắt buộc
                try {
                    val mainIntent = Intent(context, MainActivity::class.java)
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(mainIntent)
                    Log.d(TAG, "MainActivity launched")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to launch MainActivity", e)
                }
            }
        }
    }

    private fun startAppLauncherService(context: Context) {
        val serviceIntent = Intent(context, AppLauncherService::class.java)

        try {
            Log.d(TAG, "Attempting to start AppLauncherService with SDK_INT: ${Build.VERSION.SDK_INT}")
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Android 8+ yêu cầu foreground service
                Log.d(TAG, "Using startForegroundService (Android 8+)")
                context.startForegroundService(serviceIntent)
            } else {
                Log.d(TAG, "Using startService (Android 7 and below)")
                context.startService(serviceIntent)
            }
            Log.d(TAG, "✅ AppLauncherService started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start AppLauncherService: ${e.message}", e)
            // Thử backup: nếu startForegroundService thất bại, thử startService
            try {
                Log.d(TAG, "Retrying with startService as fallback...")
                context.startService(serviceIntent)
                Log.d(TAG, "✅ AppLauncherService started with fallback")
            } catch (e2: Exception) {
                Log.e(TAG, "❌ Fallback also failed: ${e2.message}", e2)
            }
        }
    }
}