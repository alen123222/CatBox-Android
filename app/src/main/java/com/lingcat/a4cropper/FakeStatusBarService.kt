package com.lingcat.a4cropper

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class FakeStatusBarService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: FrameLayout? = null

    override fun onBind(intent: Intent?): IBinder? = null

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

    private fun showOrUpdateOverlay(level: Int, isCharging: Boolean, isLowPower: Boolean, isDark: Boolean) {
        if (windowManager == null) {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        }

        val textColor = if (isDark) Color.WHITE else Color.BLACK
        val barBgColor = if (isDark) Color.argb(220, 15, 15, 18) else Color.argb(220, 245, 245, 245)

        val statusHeight = getStatusBarHeight()

        if (overlayView == null) {
            overlayView = FrameLayout(this).apply {
                setBackgroundColor(barBgColor)
            }

            // Right layout container for battery indicator
            val rightLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL or Gravity.END
                setPadding(0, 0, dpToPx(16), 0)
            }

            val tvPercent = TextView(this).apply {
                id = View.generateViewId()
                tag = "tv_percent"
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(textColor)
                text = "$level%"
            }

            // Battery body icon container
            val batteryContainer = FrameLayout(this).apply {
                val pL = LinearLayout.LayoutParams(dpToPx(24), dpToPx(13)).apply {
                    marginStart = dpToPx(6)
                }
                layoutParams = pL
            }

            // Outer border
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

            // Inner fill
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
                val fillWidth = (dpToPx(20) * (level.coerceIn(5, 100) / 100f)).toInt()
                val lp = FrameLayout.LayoutParams(fillWidth, dpToPx(9)).apply {
                    gravity = Gravity.CENTER_VERTICAL or Gravity.START
                    marginStart = dpToPx(2)
                }
                layoutParams = lp
            }

            // Battery tip
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

            // Charging lightning text / icon
            val tvCharging = TextView(this).apply {
                tag = "tv_charging"
                text = "⚡"
                textSize = 10f
                setTextColor(if (isCharging) Color.parseColor("#FFD60A") else Color.TRANSPARENT)
                val pL = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    marginStart = dpToPx(4)
                }
                layoutParams = pL
                visibility = if (isCharging) View.VISIBLE else View.GONE
            }

            batteryContainer.addView(outerBorder)
            batteryContainer.addView(innerFill)

            rightLayout.addView(tvPercent)
            rightLayout.addView(batteryContainer)
            rightLayout.addView(batteryTip)
            rightLayout.addView(tvCharging)

            val rootLp = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL or Gravity.END
            }
            overlayView?.addView(rightLayout, rootLp)

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                statusHeight,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 0
            }

            try {
                windowManager?.addView(overlayView, params)
                isRunning = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // Update existing view
            overlayView?.setBackgroundColor(barBgColor)
            val tvPercent = overlayView?.findViewWithTag<TextView>("tv_percent")
            val innerFill = overlayView?.findViewWithTag<View>("inner_fill")
            val outerBorder = overlayView?.findViewWithTag<View>("outer_border")
            val batteryTip = overlayView?.findViewWithTag<View>("battery_tip")
            val tvCharging = overlayView?.findViewWithTag<TextView>("tv_charging")

            tvPercent?.text = "$level%"
            tvPercent?.setTextColor(textColor)

            val fillColor = when {
                isLowPower || level <= 15 -> Color.parseColor("#FF453A")
                isCharging -> Color.parseColor("#34C759")
                else -> textColor
            }

            outerBorder?.background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(3).toFloat()
                setStroke(dpToPx(1), textColor)
                setColor(Color.TRANSPARENT)
            }

            batteryTip?.background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(1).toFloat()
                setColor(textColor)
            }

            innerFill?.background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(2).toFloat()
                setColor(fillColor)
            }
            val fillWidth = (dpToPx(20) * (level.coerceIn(5, 100) / 100f)).toInt()
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
        if (overlayView != null && windowManager != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
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
