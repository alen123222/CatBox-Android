package com.lingcat.a4cropper

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class FakeStatusBarService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayContainer: FrameLayout? = null
    private var pillLayout: LinearLayout? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private lateinit var prefs: SharedPreferences

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("FakeStatusBarPrefs", Context.MODE_PRIVATE)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopOverlay()
            stopSelf()
            return START_NOT_STICKY
        }

        val level = intent?.getIntExtra(EXTRA_LEVEL, 20) ?: 20
        val isCharging = intent?.getBooleanExtra(EXTRA_CHARGING, false) ?: false
        val isLowPower = intent?.getBooleanExtra(EXTRA_LOW_POWER, false) ?: false
        val isDark = intent?.getBooleanExtra(EXTRA_DARK, true) ?: true

        showOrUpdateOverlay(level, isCharging, isLowPower, isDark)
        return START_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showOrUpdateOverlay(level: Int, isCharging: Boolean, isLowPower: Boolean, isDark: Boolean) {
        if (windowManager == null) {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        }

        val textColor = if (isDark) Color.WHITE else Color.BLACK
        val pillBgColor = if (isDark) Color.argb(230, 24, 24, 28) else Color.argb(230, 240, 240, 245)

        if (overlayContainer == null) {
            val container = FrameLayout(this)

            // 浮动电量小胶囊 (Pill Layout)
            val pill = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dpToPx(8), dpToPx(3), dpToPx(8), dpToPx(3))

                val pillDrawable = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    cornerRadius = dpToPx(14).toFloat()
                    setColor(pillBgColor)
                    setStroke(dpToPx(1), if (isDark) Color.argb(80, 255, 255, 255) else Color.argb(40, 0, 0, 0))
                }
                background = pillDrawable
            }
            pillLayout = pill

            // 1. 百分比文字
            val tvPercent = TextView(this).apply {
                tag = "tv_percent"
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(textColor)
                text = "$level%"
            }

            // 2. 电池框主体容器
            val batteryContainer = FrameLayout(this).apply {
                val pL = LinearLayout.LayoutParams(dpToPx(22), dpToPx(11)).apply {
                    marginStart = dpToPx(5)
                }
                layoutParams = pL
            }

            // 外边框
            val outerBorder = View(this).apply {
                tag = "outer_border"
                val bg = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    cornerRadius = dpToPx(3).toFloat()
                    setStroke(dpToPx(1), textColor)
                    setColor(Color.TRANSPARENT)
                }
                background = bg
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            }

            // 内部电量条
            val innerFill = View(this).apply {
                tag = "inner_fill"
                val fillColor = when {
                    isLowPower || level <= 15 -> Color.parseColor("#FF453A")
                    isCharging -> Color.parseColor("#34C759")
                    else -> textColor
                }
                val bg = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    cornerRadius = dpToPx(2).toFloat()
                    setColor(fillColor)
                }
                background = bg
                val fillWidth = (dpToPx(18) * (level.coerceIn(5, 100) / 100f)).toInt()
                val lp = FrameLayout.LayoutParams(fillWidth, dpToPx(7)).apply {
                    gravity = Gravity.CENTER_VERTICAL or Gravity.START
                    marginStart = dpToPx(2)
                }
                layoutParams = lp
            }

            // 电池小正极凸起
            val batteryTip = View(this).apply {
                tag = "battery_tip"
                val bg = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    cornerRadius = dpToPx(1).toFloat()
                    setColor(textColor)
                }
                background = bg
                val pL = LinearLayout.LayoutParams(dpToPx(2), dpToPx(5)).apply {
                    marginStart = dpToPx(1)
                }
                layoutParams = pL
            }

            // 闪电图标
            val tvCharging = TextView(this).apply {
                tag = "tv_charging"
                text = "⚡"
                textSize = 9f
                setTextColor(if (isCharging) Color.parseColor("#FFD60A") else Color.TRANSPARENT)
                val pL = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    marginStart = dpToPx(3)
                }
                layoutParams = pL
                visibility = if (isCharging) View.VISIBLE else View.GONE
            }

            batteryContainer.addView(outerBorder)
            batteryContainer.addView(innerFill)

            pill.addView(tvPercent)
            pill.addView(batteryContainer)
            pill.addView(batteryTip)
            pill.addView(tvCharging)

            container.addView(pill)
            overlayContainer = container

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            // 读取已保存的位置偏好 (默认在右上角，距离屏幕边缘 x=16dp, y=系统状态栏高度 / 4)
            val savedX = prefs.getInt("pos_x", dpToPx(16))
            val savedY = prefs.getInt("pos_y", getStatusBarHeight() / 4)

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                x = savedX
                y = savedY
            }
            layoutParams = params

            // 触摸与自由拖拽定位逻辑 (长按或直接拖动均可顺滑平移)
            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f
            var isMoving = false

            pill.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isMoving = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (initialTouchX - event.rawX).toInt() // Gravity 为 TOP|END，x 越大离右侧越远
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10 || isMoving) {
                            isMoving = true
                            params.x = (initialX + dx).coerceAtLeast(0)
                            params.y = (initialY + dy).coerceAtLeast(0)
                            try {
                                windowManager?.updateViewLayout(container, params)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (isMoving) {
                            // 持久化保存用户拖拽后的位置
                            prefs.edit()
                                .putInt("pos_x", params.x)
                                .putInt("pos_y", params.y)
                                .apply()
                        }
                        true
                    }
                    else -> false
                }
            }

            try {
                windowManager?.addView(container, params)
                isRunning = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // 更新已存在的视图内容与外观
            val pill = pillLayout
            val pillBg = pill?.background as? android.graphics.drawable.GradientDrawable
            pillBg?.setColor(pillBgColor)
            pillBg?.setStroke(dpToPx(1), if (isDark) Color.argb(80, 255, 255, 255) else Color.argb(40, 0, 0, 0))

            val tvPercent = pill?.findViewWithTag<TextView>("tv_percent")
            val innerFill = pill?.findViewWithTag<View>("inner_fill")
            val outerBorder = pill?.findViewWithTag<View>("outer_border")
            val batteryTip = pill?.findViewWithTag<View>("battery_tip")
            val tvCharging = pill?.findViewWithTag<TextView>("tv_charging")

            tvPercent?.text = "$level%"
            tvPercent?.setTextColor(textColor)

            val fillColor = when {
                isLowPower || level <= 15 -> Color.parseColor("#FF453A")
                isCharging -> Color.parseColor("#34C759")
                else -> textColor
            }

            val outerDrawable = outerBorder?.background as? android.graphics.drawable.GradientDrawable
            outerDrawable?.setStroke(dpToPx(1), textColor)

            val tipDrawable = batteryTip?.background as? android.graphics.drawable.GradientDrawable
            tipDrawable?.setColor(textColor)

            val fillDrawable = innerFill?.background as? android.graphics.drawable.GradientDrawable
            fillDrawable?.setColor(fillColor)

            val fillWidth = (dpToPx(18) * (level.coerceIn(5, 100) / 100f)).toInt()
            val lp = innerFill?.layoutParams as? FrameLayout.LayoutParams
            lp?.width = fillWidth
            innerFill?.layoutParams = lp

            if (isCharging) {
                tvCharging?.visibility = View.VISIBLE
                tvCharging?.setTextColor(Color.parseColor("#FFD60A"))
            } else {
                tvCharging?.visibility = View.GONE
            }
        }
    }

    private fun stopOverlay() {
        if (overlayContainer != null && windowManager != null) {
            try {
                windowManager?.removeView(overlayContainer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayContainer = null
            pillLayout = null
            layoutParams = null
        }
        isRunning = false
    }

    override fun onDestroy() {
        stopOverlay()
        super.onDestroy()
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        if (result <= 0) {
            result = dpToPx(28)
        }
        return result
    }

    companion object {
        const val ACTION_START_OR_UPDATE = "com.lingcat.a4cropper.ACTION_FAKE_BATTERY"
        const val ACTION_STOP = "com.lingcat.a4cropper.ACTION_STOP_FAKE_BATTERY"
        const val EXTRA_LEVEL = "extra_level"
        const val EXTRA_CHARGING = "extra_charging"
        const val EXTRA_LOW_POWER = "extra_low_power"
        const val EXTRA_DARK = "extra_dark"

        var isRunning: Boolean = false
    }
}
