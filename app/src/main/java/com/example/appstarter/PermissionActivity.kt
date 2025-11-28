package com.example.appstarter

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PermissionActivity : AppCompatActivity() {

    private val TAG = "PermissionActivity"

    private lateinit var btnContinue: Button
    private lateinit var btnOverlay: Button
    private var btnExactAlarm: Button? = null // chỉ tạo nếu cần

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            setBackgroundColor(0xFF0F1419.toInt())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Tiêu đề
        val title = TextView(this).apply {
            text = "🔐 Cấp quyền cho ứng dụng"
            textSize = 26f
            setTextColor(0xFF00D4FF.toInt())
        }
        root.addView(
            title,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 32 }
        )

        // Mô tả
        val description = TextView(this).apply {
            text = "Để ứng dụng tự khởi động và hiển thị overlay trên TV Box, " +
                    "bạn hãy bấm từng nút quyền bên dưới để mở đúng màn hình cài đặt " +
                    "và bật quyền tương ứng.\n\n" +
                    "Sau khi bật đủ quyền, nút \"Tiếp tục\" sẽ được kích hoạt."
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
        }
        root.addView(
            description,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 32 }
        )

        // ===== NÚT QUYỀN: OVERLAY =====
        btnOverlay = Button(this).apply {
            text = "📺 Quyền hiển thị trên ứng dụng khác (Overlay)"
            textSize = 15f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF1E90FF.toInt())
            setOnClickListener {
                PermissionManager.openOverlaySettings(this@PermissionActivity)
            }
        }
        root.addView(
            btnOverlay,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        )

        // ===== NÚT QUYỀN: EXACT ALARM (chỉ Android 12+) =====
        if (PermissionManager.isExactAlarmRequired()) {
            btnExactAlarm = Button(this).apply {
                text = "⏰ Quyền đặt báo thức chính xác (Exact Alarm)"
                textSize = 15f
                setTextColor(0xFFFFFFFF.toInt())
                setBackgroundColor(0xFF1E90FF.toInt())
                setOnClickListener {
                    PermissionManager.openExactAlarmSettings(this@PermissionActivity)
                }
            }
            root.addView(
                btnExactAlarm,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 16 }
            )
        }

        // (Tùy chọn) Nút mở cài đặt ứng dụng tổng
        val btnAppSettings = Button(this).apply {
            text = "⚙️ Mở cài đặt ứng dụng"
            textSize = 15f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF444444.toInt())
            setOnClickListener {
                PermissionManager.openAppSettings(this@PermissionActivity)
            }
        }
        root.addView(
            btnAppSettings,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 32 }
        )

        // ===== NÚT TIẾP TỤC (BỊ VÔ HIỆU HÓA CHO ĐẾN KHI ĐỦ QUYỀN) =====
        btnContinue = Button(this).apply {
            text = "✅ Tiếp tục vào ứng dụng"
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF00AA44.toInt())
            setOnClickListener {
                if (PermissionManager.hasAllRequiredPermissions(this@PermissionActivity)) {
                    startActivity(Intent(this@PermissionActivity, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(
                        this@PermissionActivity,
                        "❌ Bạn chưa bật đủ các quyền bắt buộc.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        root.addView(
            btnContinue,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(root)
        Log.d(TAG, "PermissionActivity created")

        // Lần đầu vào màn hình: khóa nút tiếp tục
        updateContinueButtonState()
    }

    override fun onResume() {
        super.onResume()
        // Khi quay lại từ Settings: kiểm tra lại quyền và cập nhật trạng thái nút
        updateContinueButtonState()
    }

    private fun updateContinueButtonState() {
        val hasAll = PermissionManager.hasAllRequiredPermissions(this)

        btnContinue.isEnabled = hasAll
        btnContinue.alpha = if (hasAll) 1f else 0.4f

        // Optional: đổi text để user hiểu
        if (hasAll) {
            btnContinue.text = "✅ Tiếp tục vào ứng dụng"
        } else {
            btnContinue.text = "🔒 Vui lòng cấp đủ quyền để tiếp tục"
        }
    }
}
