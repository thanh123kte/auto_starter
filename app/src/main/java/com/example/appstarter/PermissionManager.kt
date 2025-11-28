package com.example.appstarter

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast

object PermissionManager {

    private const val TAG = "PermissionManager"

    /** Có bắt buộc quyền EXACT_ALARM không (Android 12+) */
    fun isExactAlarmRequired(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    /** Đã cấp đủ toàn bộ quyền bắt buộc chưa? */
    fun hasAllRequiredPermissions(context: Context): Boolean {
        val overlayOk = hasOverlayPermission(context)
        val exactAlarmOk = if (isExactAlarmRequired()) {
            hasExactAlarmPermission(context)
        } else {
            true
        }

        return overlayOk && exactAlarmOk
    }

    // ========= OVERLAY =========

    fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun openOverlaySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Toast.makeText(
                context,
                "📺 Bật quyền \"Hiển thị trên các ứng dụng khác\" cho ứng dụng.",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Log.e(TAG, "Không thể mở cài đặt overlay: ${e.message}")
            Toast.makeText(
                context,
                "❌ Không thể mở cài đặt overlay.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ========= EXACT ALARM =========

    fun hasExactAlarmPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val alarmManager = context.getSystemService(AlarmManager::class.java)
                alarmManager?.canScheduleExactAlarms() ?: true
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi kiểm tra EXACT_ALARM: ${e.message}")
                true
            }
        } else {
            true // Dưới Android 12 không cần xin riêng
        }
    }

    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Toast.makeText(
                    context,
                    "⏰ Cho phép ứng dụng đặt báo thức chính xác.",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Log.e(TAG, "Không thể mở cài đặt exact alarm: ${e.message}")
                Toast.makeText(
                    context,
                    "❌ Không thể mở cài đặt báo thức chính xác.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(
                context,
                "Phiên bản Android này không cần quyền Exact Alarm.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ========= CÀI ĐẶT ỨNG DỤNG (tùy chọn thêm) =========

    fun openAppSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Không thể mở cài đặt ứng dụng: ${e.message}")
            Toast.makeText(
                context,
                "❌ Không thể mở cài đặt ứng dụng.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
