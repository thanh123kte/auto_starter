package com.example.appstarter

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

class FloatingTimerOverlay(private val context: Context) {

    private val TAG = "FloatingTimerOverlay"

    private var windowManager: WindowManager? = null
    private var container: FrameLayout? = null
    private var timerText: TextView? = null
    private var progressBar: ProgressBar? = null
    private var countDownTimer: CountDownTimer? = null
    private var isShowing = false

    private val uiHandler = Handler(Looper.getMainLooper())

    fun show(totalSeconds: Long) {
        uiHandler.post {
            try {
                if (isShowing) return@post

                // Cần quyền overlay
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    !Settings.canDrawOverlays(context)
                ) {
                    Log.w(TAG, "SYSTEM_ALERT_WINDOW permission not granted. Cannot show overlay.")
                    return@post
                }

                windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

                val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }

                // ⬇️ Full màn, ở giữa
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    layoutType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.CENTER
                }

                // Lớp ngoài cùng: full màn, nền đen mờ
                container = FrameLayout(context).apply {
                    setBackgroundColor(0x80000000.toInt()) // 50% đen
                }

                // Card nằm giữa (giống dialog)
                val card = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(48, 48, 48, 48)
                    setBackgroundColor(0xFF1E1E1E.toInt())
                    val lp = FrameLayout.LayoutParams(
                        (context.resources.displayMetrics.widthPixels * 0.8f).toInt(),
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER
                    )
                    layoutParams = lp
                }

                // Progress bar ngang ở trên
                progressBar = ProgressBar(
                    context,
                    null,
                    android.R.attr.progressBarStyleHorizontal
                ).apply {
                    max = totalSeconds.toInt()
                    progress = totalSeconds.toInt()
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        16
                    )
                }

                // Text lớn ở giữa
                timerText = TextView(context).apply {
                    text = "⏳ Khởi chạy: ${totalSeconds}s"
                    textSize = 26f
                    setTextColor(Color.WHITE)
                    setShadowLayer(5f, 0f, 0f, Color.BLACK)
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = 24
                    }
                }

                card.addView(progressBar)
                card.addView(timerText)
                container?.addView(card)

                windowManager?.addView(container, params)
                isShowing = true

                Log.d(TAG, "System overlay dialog shown for $totalSeconds seconds")
                startCountdown(totalSeconds * 1000)
            } catch (e: Exception) {
                Log.e(TAG, "Error showing overlay: ${e.message}", e)
            }
        }
    }

    private fun startCountdown(totalMs: Long) {
        countDownTimer?.cancel()

        countDownTimer = object : CountDownTimer(totalMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                uiHandler.post {
                    timerText?.text = "⏳ Khởi chạy: ${secondsLeft}s"
                    progressBar?.progress = secondsLeft.toInt()
                }
            }

            override fun onFinish() {
                uiHandler.post {
                    timerText?.text = "🚀 Đang khởi chạy!"
                    progressBar?.progress = 0
                }

                uiHandler.postDelayed({
                    dismiss()
                }, 2000)
            }
        }.start()
    }

    fun updateProgress(current: Int, total: Int) {
        uiHandler.post {
            try {
                timerText?.text = "🚀 Đã khởi chạy: $current/$total"
            } catch (e: Exception) {
                Log.e(TAG, "Error updating progress: ${e.message}")
            }
        }
    }

    fun dismiss() {
        uiHandler.post {
            try {
                countDownTimer?.cancel()
                if (isShowing && container != null && windowManager != null) {
                    windowManager?.removeView(container)
                }
                container = null
                windowManager = null
                isShowing = false
            } catch (e: Exception) {
                Log.e(TAG, "Error dismissing overlay: ${e.message}")
            }
        }
    }
}
