package com.lingcat.a4cropper

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

// ----------------- 1. 指南针与水平仪合体 -----------------
@Composable
fun CompassScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var azimuth by remember { mutableFloatStateOf(0f) }
    var pitch by remember { mutableFloatStateOf(0f) }
    var roll by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val lastAccel = FloatArray(3)
        val lastMag = FloatArray(3)
        var hasAccel = false
        var hasMag = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    // orientation[0]: 方位角 [-pi, pi]
                    val deg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    azimuth = (deg + 360f) % 360f
                    pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
                    roll = Math.toDegrees(orientation[2].toDouble()).toFloat()
                } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    System.arraycopy(event.values, 0, lastAccel, 0, 3)
                    hasAccel = true
                    computeFallback()
                } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    System.arraycopy(event.values, 0, lastMag, 0, 3)
                    hasMag = true
                    computeFallback()
                }
            }

            private fun computeFallback() {
                if (hasAccel && hasMag) {
                    val r = FloatArray(9)
                    val i = FloatArray(9)
                    if (SensorManager.getRotationMatrix(r, i, lastAccel, lastMag)) {
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(r, orientation)
                        val deg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                        azimuth = (deg + 360f) % 360f
                        pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
                        roll = Math.toDegrees(orientation[2].toDouble()).toFloat()
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(listener, magSensor, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    val smoothAzimuth by animateFloatAsState(targetValue = azimuth, animationSpec = tween(100), label = "azimuth")

    val directionText = when {
        smoothAzimuth in 337.5..360.0 || smoothAzimuth in 0.0..22.5 -> "正北 N"
        smoothAzimuth in 22.5..67.5 -> "东北 NE"
        smoothAzimuth in 67.5..112.5 -> "正东 E"
        smoothAzimuth in 112.5..157.5 -> "东南 SE"
        smoothAzimuth in 157.5..202.5 -> "正南 S"
        smoothAzimuth in 202.5..247.5 -> "西南 SW"
        smoothAzimuth in 247.5..292.5 -> "正西 W"
        else -> "西北 NW"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(if (isLandscape) 12.dp else 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ToolTopBar(title = "指南针 & 水平仪", subtitle = "高精地磁罗盘 · 双轴双泡水平仪", onBack = onBack)
        Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 16.dp))

        if (isLandscape) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧紧凑大罗盘
                Surface(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight()
                        .shadow(6.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp)),
                    color = Color(0xFF0B0F19)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Canvas(modifier = Modifier.size(200.dp)) {
                            val center = Offset(size.width / 2, size.height / 2)
                            val radius = size.width / 2 - 16f

                            drawCircle(color = Color(0xFF1E293B), radius = radius, center = center, style = Stroke(width = 3f))

                            for (i in 0 until 360 step 30) {
                                val rad = Math.toRadians((i - smoothAzimuth - 90).toDouble())
                                val x1 = center.x + (radius - 14) * Math.cos(rad).toFloat()
                                val y1 = center.y + (radius - 14) * Math.sin(rad).toFloat()
                                val x2 = center.x + radius * Math.cos(rad).toFloat()
                                val y2 = center.y + radius * Math.sin(rad).toFloat()
                                val isCard = (i % 90 == 0)
                                drawLine(
                                    color = if (isCard) Color(0xFF38BDF8) else Color(0xFF475569),
                                    start = Offset(x1, y1),
                                    end = Offset(x2, y2),
                                    strokeWidth = if (isCard) 4f else 2f
                                )
                            }

                            drawLine(color = Color(0x33FFFFFF), start = Offset(center.x - 24, center.y), end = Offset(center.x + 24, center.y), strokeWidth = 1.5f)
                            drawLine(color = Color(0x33FFFFFF), start = Offset(center.x, center.y - 24), end = Offset(center.x, center.y + 24), strokeWidth = 1.5f)

                            val maxOffset = radius * 0.4f
                            val bubbleX = (roll / 45f).coerceIn(-1f, 1f) * maxOffset
                            val bubbleY = (-pitch / 45f).coerceIn(-1f, 1f) * maxOffset
                            val isLeveled = abs(pitch) < 1.0f && abs(roll) < 1.0f

                            drawCircle(
                                color = if (isLeveled) Color(0xFF10B981) else Color(0xAA0284C7),
                                radius = 18f,
                                center = Offset(center.x + bubbleX, center.y + bubbleY)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 18f,
                                center = Offset(center.x + bubbleX, center.y + bubbleY),
                                style = Stroke(width = 2.5f)
                            )
                        }

                        // 顶端静态北向指示标三角
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp)) {
                            Surface(shape = RoundedCornerShape(3.dp), color = Color(0xFFEF4444)) {
                                Text("▼", color = Color.White, fontSize = 12.sp)
                            }
                        }

                        // 罗盘中央数值
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${smoothAzimuth.roundToInt()}°",
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(directionText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF38BDF8))
                        }
                    }
                }

                // 右侧数据面板
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .shadow(4.dp, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("俯仰 (Pitch)", fontSize = 13.sp, color = Color(0xFF64748B))
                            Text(
                                "${pitch.roundToInt()}°",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (abs(pitch) < 1f) Color(0xFF10B981) else Color(0xFF1E293B)
                            )
                        }
                        Divider(color = Color(0xFFF1F5F9))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("横滚 (Roll)", fontSize = 13.sp, color = Color(0xFF64748B))
                            Text(
                                "${roll.roundToInt()}°",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (abs(roll) < 1f) Color(0xFF10B981) else Color(0xFF1E293B)
                            )
                        }
                        Divider(color = Color(0xFFF1F5F9))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("水平仪状态", fontSize = 13.sp, color = Color(0xFF64748B))
                            Text(
                                if (abs(pitch) < 1f && abs(roll) < 1f) "✅ 已水平" else "倾斜中",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (abs(pitch) < 1f && abs(roll) < 1f) Color(0xFF10B981) else Color(0xFFF59E0B)
                            )
                        }
                    }
                }
            }
        } else {
            // 竖屏
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp)),
                color = Color(0xFF0B0F19)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Canvas(modifier = Modifier.size(280.dp)) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = size.width / 2 - 20f

                        // 绘制外圈刻度
                        drawCircle(color = Color(0xFF1E293B), radius = radius, center = center, style = Stroke(width = 4f))

                        for (i in 0 until 360 step 30) {
                            val rad = Math.toRadians((i - smoothAzimuth - 90).toDouble())
                            val x1 = center.x + (radius - 20) * Math.cos(rad).toFloat()
                            val y1 = center.y + (radius - 20) * Math.sin(rad).toFloat()
                            val x2 = center.x + radius * Math.cos(rad).toFloat()
                            val y2 = center.y + radius * Math.sin(rad).toFloat()
                            val isCard = (i % 90 == 0)
                            drawLine(
                                color = if (isCard) Color(0xFF38BDF8) else Color(0xFF475569),
                                start = Offset(x1, y1),
                                end = Offset(x2, y2),
                                strokeWidth = if (isCard) 5f else 2f
                            )
                        }

                        // 绘制中心十字准星
                        drawLine(color = Color(0x33FFFFFF), start = Offset(center.x - 30, center.y), end = Offset(center.x + 30, center.y), strokeWidth = 2f)
                        drawLine(color = Color(0x33FFFFFF), start = Offset(center.x, center.y - 30), end = Offset(center.x, center.y + 30), strokeWidth = 2f)

                        // 绘制水平仪气泡
                        val maxOffset = radius * 0.4f
                        val bubbleX = (roll / 45f).coerceIn(-1f, 1f) * maxOffset
                        val bubbleY = (-pitch / 45f).coerceIn(-1f, 1f) * maxOffset
                        val isLeveled = abs(pitch) < 1.0f && abs(roll) < 1.0f

                        drawCircle(
                            color = if (isLeveled) Color(0xFF10B981) else Color(0xAA0284C7),
                            radius = 24f,
                            center = Offset(center.x + bubbleX, center.y + bubbleY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 24f,
                            center = Offset(center.x + bubbleX, center.y + bubbleY),
                            style = Stroke(width = 3f)
                        )
                    }

                    // 顶端静态北向指示标三角
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.TopCenter).padding(top = 18.dp)) {
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFEF4444)) {
                            Text("▼", color = Color.White, fontSize = 14.sp)
                        }
                    }

                    // 罗盘中央数值
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${smoothAzimuth.roundToInt()}°",
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(directionText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF38BDF8))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 下方水平仪倾角数据卡片
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier.padding(18.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("俯仰 (Pitch)", fontSize = 12.sp, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "${pitch.roundToInt()}°",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (abs(pitch) < 1f) Color(0xFF10B981) else Color(0xFF1E293B)
                        )
                    }
                    Box(modifier = Modifier.width(1.dp).height(36.dp).background(Color(0xFFE2E8F0)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("横滚 (Roll)", fontSize = 12.sp, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "${roll.roundToInt()}°",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (abs(roll) < 1f) Color(0xFF10B981) else Color(0xFF1E293B)
                        )
                    }
                    Box(modifier = Modifier.width(1.dp).height(36.dp).background(Color(0xFFE2E8F0)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("状态", fontSize = 12.sp, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            if (abs(pitch) < 1f && abs(roll) < 1f) "✅ 已水平" else "倾斜",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (abs(pitch) < 1f && abs(roll) < 1f) Color(0xFF10B981) else Color(0xFFF59E0B)
                        )
                    }
                }
            }
        }
    }
}

// ----------------- 2. 高精度刻度尺 (厘米/英寸) -----------------
@Composable
fun RulerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dm = context.resources.displayMetrics
    val xdpi = dm.xdpi // 屏幕物理每英寸像素点
    val pxPerMm = (xdpi / 25.4f)
    val pxPerCm = pxPerMm * 10f
    val pxPerInch = xdpi

    var unitIsCm by remember { mutableStateOf(true) }
    var indicatorY by remember { mutableFloatStateOf(300f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 顶部控制条
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), CircleShape)
            ) {
                Text("‹", fontSize = 26.sp, color = Color(0xFF334155))
            }

            // 当前游标测量值大字
            val measuredValue = if (unitIsCm) {
                indicatorY / pxPerCm
            } else {
                indicatorY / pxPerInch
            }
            Text(
                "%.2f %s".format(measuredValue, if (unitIsCm) "cm" else "inch"),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF0284C7)
            )

            Row {
                FilterChip(
                    selected = unitIsCm,
                    onClick = { unitIsCm = true },
                    label = { Text("cm") },
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                FilterChip(
                    selected = !unitIsCm,
                    onClick = { unitIsCm = false },
                    label = { Text("in") },
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        // 尺子刻度绘图主体
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFFFFFBEB)) // 经典实木米黄尺色
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        indicatorY = (indicatorY + dragAmount.y).coerceIn(0f, size.height.toFloat())
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                val stepPx = if (unitIsCm) pxPerMm else (pxPerInch / 16f)
                val totalSteps = (h / stepPx).toInt()

                for (i in 0..totalSteps) {
                    val y = i * stepPx
                    val isMain = if (unitIsCm) (i % 10 == 0) else (i % 16 == 0)
                    val isHalf = if (unitIsCm) (i % 5 == 0 && !isMain) else (i % 8 == 0 && !isMain)

                    val lineLen = when {
                        isMain -> 100f
                        isHalf -> 65f
                        else -> 40f
                    }

                    // 左侧刻度
                    drawLine(
                        color = Color(0xFF451A03),
                        start = Offset(0f, y),
                        end = Offset(lineLen, y),
                        strokeWidth = if (isMain) 3f else 1.5f
                    )

                    // 右侧刻度对称绘制
                    drawLine(
                        color = Color(0xFF451A03),
                        start = Offset(w - lineLen, y),
                        end = Offset(w, y),
                        strokeWidth = if (isMain) 3f else 1.5f
                    )

                    // 绘制数字标识
                    if (isMain) {
                        val num = if (unitIsCm) i / 10 else i / 16
                        drawContext.canvas.nativeCanvas.drawText(
                            "$num",
                            lineLen + 20f,
                            y + 12f,
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.rgb(69, 26, 3)
                                textSize = 32f
                                isFakeBoldText = true
                            }
                        )
                    }
                }

                // 绘制拖动测量红色指示游标
                drawLine(
                    color = Color(0xFFEF4444),
                    start = Offset(0f, indicatorY),
                    end = Offset(w, indicatorY),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
            }

            // 游标拖动手柄说明
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEF4444),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (indicatorY / LocalContext.current.resources.displayMetrics.density - 16).dp)
            ) {
                Text(
                    "拖动测量线",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}
