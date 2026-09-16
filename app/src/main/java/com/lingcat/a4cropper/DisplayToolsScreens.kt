package com.lingcat.a4cropper

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}

// ----------------- 1. LED 手持弹幕字幕 -----------------
@Composable
fun LedScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    var text by remember { mutableStateOf("灵猫工坊 66 🐾") }
    var fontSizeSp by remember { mutableFloatStateOf(if (isLandscape) 140f else 90f) }
    var isScrolling by remember { mutableStateOf(true) }
    var textColor by remember { mutableStateOf(Color(0xFF00FF66)) } // 经典荧光绿
    var bgColor by remember { mutableStateOf(Color(0xFF000000)) }

    var containerW by remember { mutableIntStateOf(1000) }
    var textW by remember { mutableIntStateOf(600) }
    var isConfigOpen by remember { mutableStateOf(false) }

    val colors = listOf(
        Color(0xFF00FF66), // 荧光绿
        Color(0xFFFF0055), // 荧光粉
        Color(0xFF00E5FF), // 霓虹蓝
        Color(0xFFFFEE00), // 荧光黄
        Color(0xFFFFFFFF), // 纯白
        Color(0xFFFF6600)  // 暖橙
    )

    // 滚动动画驱动
    val scrollAnim = remember { Animatable(0f) }
    LaunchedEffect(isScrolling, text, fontSizeSp, containerW, textW) {
        if (isScrolling && containerW > 0 && textW > 0) {
            val totalDistance = (containerW + textW).toFloat()
            while (true) {
                scrollAnim.snapTo(containerW.toFloat())
                scrollAnim.animateTo(
                    targetValue = -textW.toFloat(),
                    animationSpec = tween(
                        durationMillis = (totalDistance * 6).toInt().coerceAtLeast(1500),
                        easing = LinearEasing
                    )
                )
            }
        } else {
            scrollAnim.snapTo(0f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .clickable { isConfigOpen = !isConfigOpen }
            .onSizeChanged { containerW = it.width }
    ) {
        // 核心显示区域
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = textColor,
                fontSize = fontSizeSp.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .onSizeChanged { textW = it.width }
                    .then(
                        if (isScrolling) {
                            Modifier.offset { IntOffset(scrollAnim.value.roundToInt(), 0) }
                        } else {
                            Modifier.padding(horizontal = 16.dp)
                        }
                    )
            )
        }

        // 顶栏微型返回按钮 (常亮但半透明，点击不遮挡)
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0x55FFFFFF))
        ) {
            Text("‹", color = Color.White, fontSize = 24.sp)
        }

        // 浮层控制面板 (点击屏幕任何地方展开/收起)
        AnimatedVisibility(
            visible = isConfigOpen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xEE1E293B),
                shadowElevation = 12.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("LED 弹幕控制", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        TextButton(onClick = { isConfigOpen = false }) {
                            Text("完成", color = Color(0xFF38BDF8))
                        }
                    }

                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("弹幕文本", color = Color.LightGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("滚动模式", color = Color.White, fontSize = 13.sp)
                        Switch(
                            checked = isScrolling,
                            onCheckedChange = { isScrolling = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("字号大小 (${fontSizeSp.roundToInt()}sp)", color = Color.White, fontSize = 13.sp)
                    Slider(
                        value = fontSizeSp,
                        onValueChange = { fontSizeSp = it },
                        valueRange = 40f..180f
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("LED 霓虹色彩", color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(colors) { c ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .clickable { textColor = c }
                                    .then(if (textColor == c) Modifier.border(2.dp, Color.White, CircleShape) else Modifier)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ----------------- 2. 全屏极简时间时钟 (黑白切换/农历/节日/动效) -----------------
@Composable
fun LunarClockScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    var isDarkTheme by remember { mutableStateOf(true) }
    var currentTime by remember { mutableStateOf(Date()) }
    var todayInfo by remember { mutableStateOf(LunarCalendarHelper.getTodayInfo()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(1000)
        }
    }

    // 主题过渡色彩动效
    val animatedBg by animateColorAsState(
        targetValue = if (isDarkTheme) Color(0xFF000000) else Color(0xFFFFFFFF),
        animationSpec = tween(500),
        label = "bgColor"
    )
    val animatedFg by animateColorAsState(
        targetValue = if (isDarkTheme) Color(0xFFFFFFFF) else Color(0xFF000000),
        animationSpec = tween(500),
        label = "fgColor"
    )
    val subTextColor by animateColorAsState(
        targetValue = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
        animationSpec = tween(500),
        label = "subColor"
    )

    val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val formattedTime = timeFormatter.format(currentTime)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedBg)
            .clickable { isDarkTheme = !isDarkTheme }
            .padding(if (isLandscape) 14.dp else 24.dp)
    ) {
        // 顶部退出与切换提示
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(if (isLandscape) 36.dp else 40.dp)
                    .clip(CircleShape)
                    .background(if (isDarkTheme) Color(0x33FFFFFF) else Color(0x11000000))
            ) {
                Text("‹", fontSize = 24.sp, color = animatedFg)
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDarkTheme) Color(0x22FFFFFF) else Color(0x11000000)
            ) {
                Text(
                    text = if (isDarkTheme) "☀️ 点按切换白底" else "🌙 点按切换黑底",
                    color = subTextColor,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        // 中心超大时钟与年月日农历节日
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 公历年月日与星期
            Text(
                text = "${todayInfo.solarYear}年${todayInfo.solarMonth}月${todayInfo.solarDay}日 · ${todayInfo.weekDay}",
                fontSize = if (isLandscape) 16.sp else 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = subTextColor,
                letterSpacing = if (isLandscape) 1.5.sp else 2.sp
            )

            Spacer(modifier = Modifier.height(if (isLandscape) 4.dp else 8.dp))

            // 农历日期与节日高光 Badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = todayInfo.lunarDateText,
                    fontSize = if (isLandscape) 13.sp else 15.sp,
                    color = subTextColor,
                    fontWeight = FontWeight.Medium
                )
                if (todayInfo.festival != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEF4444)
                    ) {
                        Text(
                            text = todayInfo.festival!!,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isLandscape) 10.dp else 24.dp))

            // 主时钟数字 (大字号+Monospace)
            AnimatedContent(
                targetState = formattedTime,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(300)) + slideInVertically { it / 8 })
                        .togetherWith(fadeOut(animationSpec = tween(300)) + slideOutVertically { -it / 8 })
                },
                label = "clockTick"
            ) { targetTime ->
                Text(
                    text = targetTime,
                    fontSize = if (isLandscape) 82.sp else 68.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = animatedFg,
                    letterSpacing = 1.sp
                )
            }
        }

        // 底部极简提示
        Text(
            text = "屏幕常亮中 · 伴随主人静谧专注 🐾",
            fontSize = 11.sp,
            color = subTextColor.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }
}
