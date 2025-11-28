package com.example.appstarter

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log

object AppUtils {
    private const val PREFS_NAME = "AppStarterPrefs"
    private const val KEY_SELECTED_APPS = "selected_apps"
    private const val KEY_INITIAL_DELAY = "initial_delay"
    private const val KEY_BETWEEN_APPS_DELAY = "between_apps_delay"
    private const val TAG = "AppUtils"

    // Default delays in seconds
    private const val DEFAULT_INITIAL_DELAY = 15
    private const val DEFAULT_BETWEEN_APPS_DELAY = 5

    fun saveSelectedApps(context: Context, packages: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val success = prefs.edit().putStringSet(KEY_SELECTED_APPS, packages.toSet()).commit()
        Log.d(TAG, "Saved ${packages.size} apps, success: $success")
    }

    fun getSavedApps(context: Context): List<String> {
        return loadSelectedApps(context)
    }

    fun loadSelectedApps(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val apps = prefs.getStringSet(KEY_SELECTED_APPS, emptySet())?.toList() ?: emptyList()
        Log.d(TAG, "Loaded ${apps.size} saved apps")
        return apps
    }

    // Lưu thời gian delay ban đầu (giây)
    fun saveInitialDelay(context: Context, delaySeconds: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val success = prefs.edit().putInt(KEY_INITIAL_DELAY, delaySeconds).commit()
        Log.d(TAG, "Saved initial delay: ${delaySeconds}s, success: $success")

        // Verify save
        val savedValue = prefs.getInt(KEY_INITIAL_DELAY, -1)
        Log.d(TAG, "Verified saved initial delay: ${savedValue}s")
    }

    // Lấy thời gian delay ban đầu (giây)
    fun getInitialDelay(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val delay = prefs.getInt(KEY_INITIAL_DELAY, DEFAULT_INITIAL_DELAY)
        Log.d(TAG, "Retrieved initial delay: ${delay}s")
        return delay
    }

    // Lưu thời gian delay giữa các apps (giây)
    fun saveBetweenAppsDelay(context: Context, delaySeconds: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val success = prefs.edit().putInt(KEY_BETWEEN_APPS_DELAY, delaySeconds).commit()
        Log.d(TAG, "Saved between apps delay: ${delaySeconds}s, success: $success")

        // Verify save
        val savedValue = prefs.getInt(KEY_BETWEEN_APPS_DELAY, -1)
        Log.d(TAG, "Verified saved between apps delay: ${savedValue}s")
    }

    // Lấy thời gian delay giữa các apps (giây)
    fun getBetweenAppsDelay(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val delay = prefs.getInt(KEY_BETWEEN_APPS_DELAY, DEFAULT_BETWEEN_APPS_DELAY)
        Log.d(TAG, "Retrieved between apps delay: ${delay}s")
        return delay
    }

    // Debug function để kiểm tra tất cả settings
    fun debugAllSettings(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        Log.d(TAG, "=== DEBUG ALL SETTINGS ===")
        Log.d(TAG, "Initial delay: ${prefs.getInt(KEY_INITIAL_DELAY, -1)}s")
        Log.d(TAG, "Between apps delay: ${prefs.getInt(KEY_BETWEEN_APPS_DELAY, -1)}s")
        Log.d(TAG, "Selected apps: ${prefs.getStringSet(KEY_SELECTED_APPS, emptySet())}")
        Log.d(TAG, "========================")
    }

    fun getLaunchableApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        return try {
            pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                .map {
                    AppInfo(
                        name = it.loadLabel(pm).toString(),
                        packageName = it.activityInfo.packageName
                    )
                }
                .distinctBy { it.packageName }
                .sortedBy { it.name.lowercase() }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting launchable apps", e)
            emptyList()
        }
    }

    fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}

data class AppInfo(val name: String, val packageName: String)