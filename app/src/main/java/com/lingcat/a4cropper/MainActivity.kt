package com.lingcat.a4cropper

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Rect as AndroidRect
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CatToolboxTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF6F8FC)
                ) {
                    MainAppNav()
                }
            }
        }
    }
}

// 统一所有工具页面枚举
enum class Screen {
    HOME,
    CROPPER,
    COMPRESS,
    DPI,
    COLOR_EXTRACT,
    COMPASS,
    RULER,
    LED,
    TIME,
    APK_EXTRACTOR,
    // 新增工具
    SPECIAL_TEXT,
    MORSE,
    BASE64,
    MINI_TEXT,
    PIXEL_ART,
    QR_CODE,
    FAKE_BATTERY,
    VIBRATOR,
    WALLPAPER,
    DEVICE_INFO
}

data class ToolItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val tag: String,
    val iconEmoji: String,
    val gradientColors: List<Color>
)

val ToolList = listOf(
    ToolItem(
        id = "special_text",
        title = "特殊文本生成",
        subtitle = "花样艺术字体/符号边框",
        tag = "全新",
        iconEmoji = "🔤",
        gradientColors = listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
    ),
    ToolItem(
        id = "pixel_art",
        title = "图片像素画",
        subtitle = "复古8-bit/颗粒像素艺术",
        tag = "全新",
        iconEmoji = "👾",
        gradientColors = listOf(Color(0xFF818CF8), Color(0xFF4F46E5))
    ),
    ToolItem(
        id = "qr_code",
        title = "二维码生成",
        subtitle = "纯本地离线极速生成",
        tag = "全新",
        iconEmoji = "🏁",
        gradientColors = listOf(Color(0xFF34D399), Color(0xFF059669))
    ),
    ToolItem(
        id = "cropper",
        title = "比例裁切",
        subtitle = "A4/标准画幅自由取景",
        tag = "核心",
        iconEmoji = "📐",
        gradientColors = listOf(Color(0xFF4FACFE), Color(0xFF00F2FE))
    ),
    ToolItem(
        id = "apk_extractor",
        title = "应用提取 (APK)",
        subtitle = "已装应用导出/记忆目录",
        tag = "常用",
        iconEmoji = "📦",
        gradientColors = listOf(Color(0xFF38EF7D), Color(0xFF11998E))
    ),
    ToolItem(
        id = "morse",
        title = "摩斯密码转换",
        subtitle = "点横莫尔斯双向编解码",
        tag = "全新",
        iconEmoji = "📻",
        gradientColors = listOf(Color(0xFFFBBF24), Color(0xFFD97706))
    ),
    ToolItem(
        id = "base64",
        title = "Base64 加解密",
        subtitle = "RFC 4648 标准编解码",
        tag = "全新",
        iconEmoji = "🔐",
        gradientColors = listOf(Color(0xFFA78BFA), Color(0xFF7C3AED))
    ),
    ToolItem(
        id = "mini_text",
        title = "迷你文字生成",
        subtitle = "微信小尾巴上标/下标",
        tag = "全新",
        iconEmoji = "🪶",
        gradientColors = listOf(Color(0xFFFB7185), Color(0xFFE11D48))
    ),
    ToolItem(
        id = "fake_battery",
        title = "电量伪装",
        subtitle = "防借手机/1%极限濒死",
        tag = "趣味",
        iconEmoji = "🪫",
        gradientColors = listOf(Color(0xFFF87171), Color(0xFFDC2626))
    ),
    ToolItem(
        id = "vibrator",
        title = "震动测试器",
        subtitle = "马达体检/心跳摩斯律动",
        tag = "实用",
        iconEmoji = "📳",
        gradientColors = listOf(Color(0xFFF43F5E), Color(0xFFBE123C))
    ),
    ToolItem(
        id = "wallpaper",
        title = "提取手机壁纸",
        subtitle = "原画无损提取桌面壁纸",
        tag = "全新",
        iconEmoji = "🖼️",
        gradientColors = listOf(Color(0xFF60A5FA), Color(0xFF2563EB))
    ),
    ToolItem(
        id = "device_info",
        title = "查看设备信息",
        subtitle = "硬件参数/系统与电池状态",
        tag = "系统",
        iconEmoji = "📱",
        gradientColors = listOf(Color(0xFF94A3B8), Color(0xFF475569))
    ),
    ToolItem(
        id = "compress",
        title = "画质压缩",
        subtitle = "低损耗极速瘦身归档",
        tag = "实用",
        iconEmoji = "🗜️",
        gradientColors = listOf(Color(0xFFFF9A8B), Color(0xFFFF6A88))
    ),
    ToolItem(
        id = "dpi",
        title = "DPI 转换",
        subtitle = "打印精度 300DPI 重采",
        tag = "图像",
        iconEmoji = "🖨️",
        gradientColors = listOf(Color(0xFF43E97B), Color(0xFF38F9D7))
    ),
    ToolItem(
        id = "color_extract",
        title = "二次元取色",
        subtitle = "调色盘与像素拾色Hex",
        tag = "图像",
        iconEmoji = "🎨",
        gradientColors = listOf(Color(0xFFFA709A), Color(0xFFFEE140))
    ),
    ToolItem(
        id = "compass",
        title = "指南针水平仪",
        subtitle = "高精地磁罗盘双轴气泡",
        tag = "传感器",
        iconEmoji = "🧭",
        gradientColors = listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6))
    ),
    ToolItem(
        id = "ruler",
        title = "高精度刻度尺",
        subtitle = "毫米/厘米/英寸游标测距",
        tag = "物理",
        iconEmoji = "📏",
        gradientColors = listOf(Color(0xFFF59E0B), Color(0xFFD97706))
    ),
    ToolItem(
        id = "led",
        title = "LED 滚动弹幕",
        subtitle = "字号颜色/常亮全屏字幕",
        tag = "大屏",
        iconEmoji = "📟",
        gradientColors = listOf(Color(0xFF10B981), Color(0xFF047857))
    ),
    ToolItem(
        id = "time",
        title = "全屏极简时钟",
        subtitle = "黑白切换/农历/节日动效",
        tag = "时间",
        iconEmoji = "⏱️",
        gradientColors = listOf(Color(0xFF6366F1), Color(0xFF4338CA))
    )
)

@Composable
fun MainAppNav() {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }

    when (currentScreen) {
        Screen.HOME -> HomeScreen(onOpenTool = { toolId ->
            currentScreen = when (toolId) {
                "cropper" -> Screen.CROPPER
                "compress" -> Screen.COMPRESS
                "dpi" -> Screen.DPI
                "color_extract" -> Screen.COLOR_EXTRACT
                "compass" -> Screen.COMPASS
                "ruler" -> Screen.RULER
                "led" -> Screen.LED
                "time" -> Screen.TIME
                "apk_extractor" -> Screen.APK_EXTRACTOR
                "special_text" -> Screen.SPECIAL_TEXT
                "morse" -> Screen.MORSE
                "base64" -> Screen.BASE64
                "mini_text" -> Screen.MINI_TEXT
                "pixel_art" -> Screen.PIXEL_ART
                "qr_code" -> Screen.QR_CODE
                "fake_battery" -> Screen.FAKE_BATTERY
                "vibrator" -> Screen.VIBRATOR
                "wallpaper" -> Screen.WALLPAPER
                "device_info" -> Screen.DEVICE_INFO
                else -> Screen.HOME
            }
        })
        Screen.CROPPER -> {
            BackHandler { currentScreen = Screen.HOME }
            CropperToolScreen(onBack = { currentScreen = Screen.HOME })
        }
        Screen.COMPRESS -> {
            BackHandler { currentScreen = Screen.HOME }
            CompressToolScreen(onBack = { currentScreen = Screen.HOME })
        }
        Screen.DPI -> {
            BackHandler { currentScreen = Screen.HOME }
            DpiToolScreen(onBack = { currentScreen = Screen.HOME })
        }
        Screen.COLOR_EXTRACT -> {
            BackHandler { currentScreen = Screen.HOME }
            ColorExtractToolScreen(onBack = { currentScreen = Screen.HOME })
        }
        Screen.COMPASS -> {
            BackHandler { currentScreen = Screen.HOME }
            CompassScreen(onBack = { currentScreen = Screen.HOME })
        }
        Screen.RULER -> {
            BackHandler { currentScreen = Screen.HOME }
            RulerScreen(onBack = { currentScreen = Screen.HOME })
        }
        Screen.LED -> {
            BackHandler { currentScreen = Screen.HOME }
            LedScreen(onBack = { currentScreen = Screen.HOME })
        }
        Screen.TIME -> {
            BackHandler { currentScreen = Screen.HOME }
            LunarClockScreen(onBack = { currentScreen = Screen.HOME })
        }
        Screen.APK_EXTRACTOR -> {
            BackHandler { currentScreen = Screen.HOME }
            ApkExtractorScreen(onBack = { currentScreen = Screen.HOME })
        }
        Screen.SPECIAL_TEXT -> {
            BackHandler { currentScreen = Screen.HOME }
            SpecialTextScreen(onBack = { currentScreen = Screen.HOME })
        }
        Screen.MORSE -> {
            BackHandler { currentScreen = Screen.HOME }
            MorseCodeScreen(onBack = { currentScreen = Screen.HOME })
        }
        Screen.BASE64 -> {
            BackHandler { currentScreen = Screen.HOME }
            Base64Screen(onBack = { currentScreen = Screen.HOME })
        }
        Screen.MINI_TEXT -> {
            BackHandler { currentScreen = Screen.HOME }
            MiniTextScreen(onBack = { currentScreen = Screen.HOME })
        }
        Screen.PIXEL_ART -> {
            BackHandler { currentScreen = Screen.HOME }
            PixelArtScreen(onBack = { currentScreen = Screen.HOME })
        }
        Screen.QR_CODE -> {
            BackHandler { currentScreen = Screen.HOME }
            QrCodeGeneratorScreen(onBack = { currentScreen = Screen.HOME })
        }
        Screen.FAKE_BATTERY -> {
            BackHandler { currentScreen = Screen.HOME }
            FakeBatteryScreen(onBack = { currentScreen = Screen.HOME })
        }
        Screen.VIBRATOR -> {
            BackHandler { currentScreen = Screen.HOME }
            VibratorScreen(onBack = { currentScreen = Screen.HOME })
        }
        Screen.WALLPAPER -> {
            BackHandler { currentScreen = Screen.HOME }
            WallpaperExtractorScreen(onBack = { currentScreen = Screen.HOME })
        }
        Screen.DEVICE_INFO -> {
            BackHandler { currentScreen = Screen.HOME }
            DeviceInfoScreen(onBack = { currentScreen = Screen.HOME })
        }
    }
}

@Composable
fun HomeScreen(onOpenTool: (String) -> Unit) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var searchQuery by remember { mutableStateOf("") }

    val filteredTools = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            ToolList
        } else {
            val q = searchQuery.trim().lowercase()
            ToolList.filter {
                it.title.lowercase().contains(q) ||
                it.subtitle.lowercase().contains(q) ||
                it.tag.lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = if (isLandscape) 24.dp else 16.dp, vertical = if (isLandscape) 8.dp else 14.dp)
    ) {
        // 极简顶栏 Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "CatBox",
                fontSize = if (isLandscape) 22.sp else 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )

            Box(
                modifier = Modifier
                    .size(if (isLandscape) 34.dp else 40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🐾", fontSize = if (isLandscape) 16.sp else 20.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 顶部小巧精致搜索框 (Soft UI)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp),
            shape = RoundedCornerShape(21.dp),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔍", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 13.sp,
                        color = Color(0xFF1E293B)
                    ),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "搜索工具名称、功能关键词...",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        innerTextField()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2E8F0))
                            .clickable { searchQuery = "" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", fontSize = 10.sp, color = Color(0xFF64748B))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 12.dp))

        if (filteredTools.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🐱", fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("未找到相关小工具喵~", fontSize = 13.sp, color = Color(0xFF94A3B8))
                }
            }
        } else {
            // 全能网格矩阵：横屏 4 列，竖屏 2 列
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (isLandscape) 4 else 2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredTools, key = { it.id }) { tool ->
                    ToolCard(tool = tool, isLandscape = isLandscape, onClick = { onOpenTool(tool.id) })
                }
            }
        }
    }
}

@Composable
fun ToolCard(tool: ToolItem, isLandscape: Boolean = false, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isLandscape) 116.dp else 136.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = Color(0x18000000),
                ambientColor = Color(0x0C000000)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isLandscape) 10.dp else 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isLandscape) 34.dp else 42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(tool.gradientColors)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = tool.iconEmoji, fontSize = if (isLandscape) 16.sp else 20.sp)
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFE0F2FE)
                ) {
                    Text(
                        text = tool.tag,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF0284C7)
                    )
                }
            }

            Column {
                Text(
                    text = tool.title,
                    fontSize = if (isLandscape) 13.sp else 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tool.subtitle,
                    fontSize = 10.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ----------------- 比例裁切二级页面 -----------------

data class CropRatio(val name: String, val value: Float)

val CropRatios = listOf(
    CropRatio("A4 竖版", 1f / 1.4142f),
    CropRatio("A4 横版", 1.4142f),
    CropRatio("1:1 方形", 1f),
    CropRatio("4:3", 4f / 3f),
    CropRatio("16:9", 16f / 9f),
    CropRatio("3:2", 3f / 2f)
)

@Composable
fun CropperToolScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val coroutineScope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedRatio by remember { mutableStateOf(CropRatios[0]) }
    var isSaving by remember { mutableStateOf(false) }

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(Size.Zero) }

    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                bitmap = BitmapFactory.decodeStream(inputStream)
                scale = 1f
                offset = Offset.Zero
            } catch (e: Exception) {
                Toast.makeText(context, "读取图片失败: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(if (isLandscape) 12.dp else 16.dp)
    ) {
        ToolTopBar(title = "比例裁切", subtitle = "双指缩放拖动调整 · 自由取景", onBack = onBack)
        Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 14.dp))

        if (isLandscape) {
            // 横屏布局：左右分栏，左侧画布，右侧控制面板与操作按键
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 左侧画布
                Surface(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight()
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(20.dp), spotColor = Color(0x22000000))
                        .clip(RoundedCornerShape(20.dp))
                        .onSizeChanged { containerSize = it.toSize() },
                    color = Color(0xFF181920)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (bitmap != null) {
                            val currentBitmap = bitmap!!
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            scale = (scale * zoom).coerceIn(0.1f, 20f)
                                            offset += pan
                                        }
                                    }
                            ) {
                                val imageBitmap = currentBitmap.asImageBitmap()
                                val dstW = (imageBitmap.width * scale).roundToInt()
                                val dstH = (imageBitmap.height * scale).roundToInt()

                                drawImage(
                                    image = imageBitmap,
                                    dstOffset = IntOffset(offset.x.roundToInt(), offset.y.roundToInt()),
                                    dstSize = IntSize(dstW, dstH)
                                )

                                val ratioValue = selectedRatio.value
                                val pad = 24f
                                val availW = max(10f, size.width - pad * 2)
                                val availH = max(10f, size.height - pad * 2)
                                val cropW = if (availW / availH > ratioValue) availH * ratioValue else availW
                                val cropH = cropW / ratioValue
                                val cropLeft = (size.width - cropW) / 2f
                                val cropTop = (size.height - cropH) / 2f

                                drawRect(color = Color(0x66000000), topLeft = Offset.Zero, size = size)
                                drawRect(
                                    color = Color(0xFF38BDF8),
                                    topLeft = Offset(cropLeft, cropTop),
                                    size = Size(cropW, cropH),
                                    style = Stroke(width = 3.dp.toPx())
                                )
                            }
                        } else {
                            EmptyPickerBox(onPick = {
                                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            })
                        }
                    }
                }

                // 右侧控制与导出面板
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(18.dp))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("裁切比例", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF334155))
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                CropRatios.take(3).forEach { ratio ->
                                    FilterChip(
                                        selected = selectedRatio == ratio,
                                        onClick = { selectedRatio = ratio },
                                        label = { Text(ratio.name, fontSize = 11.sp) },
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                CropRatios.drop(3).forEach { ratio ->
                                    FilterChip(
                                        selected = selectedRatio == ratio,
                                        onClick = { selectedRatio = ratio },
                                        label = { Text(ratio.name, fontSize = 11.sp) },
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (bitmap != null) {
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { bitmap = null },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(14.dp),
                                enabled = !isSaving
                            ) {
                                Text("重选图片", color = Color(0xFF64748B), fontSize = 13.sp)
                            }

                            Button(
                                onClick = {
                                    if (isSaving) return@Button
                                    val bmp = bitmap ?: return@Button
                                    val cSize = containerSize
                                    if (cSize.width <= 0 || cSize.height <= 0) {
                                        Toast.makeText(context, "画布尚未加载完毕", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    isSaving = true
                                    coroutineScope.launch {
                                        try {
                                            val cropped = withContext(Dispatchers.Default) {
                                                performCrop(bmp, scale, offset, cSize, selectedRatio.value)
                                            }
                                            withContext(Dispatchers.Main) {
                                                if (cropped != null) {
                                                    saveCroppedBitmap(context, cropped)
                                                } else {
                                                    Toast.makeText(context, "裁剪失败，请调整缩放", Toast.LENGTH_LONG).show()
                                                }
                                                isSaving = false
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "异常: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                                isSaving = false
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                enabled = !isSaving
                            ) {
                                Text(
                                    if (isSaving) "正在导出..." else "✨ 保存至相册",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // 竖屏布局：经典上下排布
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp), spotColor = Color(0x22000000))
                    .clip(RoundedCornerShape(24.dp))
                    .onSizeChanged { containerSize = it.toSize() },
                color = Color(0xFF181920)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (bitmap != null) {
                        val currentBitmap = bitmap!!
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(0.1f, 20f)
                                        offset += pan
                                    }
                                }
                        ) {
                            val imageBitmap = currentBitmap.asImageBitmap()
                            val dstW = (imageBitmap.width * scale).roundToInt()
                            val dstH = (imageBitmap.height * scale).roundToInt()

                            drawImage(
                                image = imageBitmap,
                                dstOffset = IntOffset(offset.x.roundToInt(), offset.y.roundToInt()),
                                dstSize = IntSize(dstW, dstH)
                            )

                            val ratioValue = selectedRatio.value
                            val pad = 40f
                            val availW = max(10f, size.width - pad * 2)
                            val availH = max(10f, size.height - pad * 2)
                            val cropW = if (availW / availH > ratioValue) availH * ratioValue else availW
                            val cropH = cropW / ratioValue
                            val cropLeft = (size.width - cropW) / 2f
                            val cropTop = (size.height - cropH) / 2f

                            drawRect(color = Color(0x66000000), topLeft = Offset.Zero, size = size)
                            drawRect(
                                color = Color(0xFF38BDF8),
                                topLeft = Offset(cropLeft, cropTop),
                                size = Size(cropW, cropH),
                                style = Stroke(width = 3.dp.toPx())
                            )
                        }
                    } else {
                        EmptyPickerBox(onPick = {
                            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        })
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 比例选择器 Pills
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        CropRatios.take(3).forEach { ratio ->
                            FilterChip(
                                selected = selectedRatio == ratio,
                                onClick = { selectedRatio = ratio },
                                label = { Text(ratio.name, fontSize = 12.sp) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        CropRatios.drop(3).forEach { ratio ->
                            FilterChip(
                                selected = selectedRatio == ratio,
                                onClick = { selectedRatio = ratio },
                                label = { Text(ratio.name, fontSize = 12.sp) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (bitmap != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { bitmap = null },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isSaving
                    ) {
                        Text("重选", color = Color(0xFF64748B))
                    }

                    Button(
                        onClick = {
                            if (isSaving) return@Button
                            val bmp = bitmap ?: return@Button
                            val cSize = containerSize
                            if (cSize.width <= 0 || cSize.height <= 0) {
                                Toast.makeText(context, "画布尚未加载完毕", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isSaving = true
                            coroutineScope.launch {
                                try {
                                    val cropped = withContext(Dispatchers.Default) {
                                        performCrop(bmp, scale, offset, cSize, selectedRatio.value)
                                    }
                                    withContext(Dispatchers.Main) {
                                        if (cropped != null) {
                                            saveCroppedBitmap(context, cropped)
                                        } else {
                                            Toast.makeText(context, "裁剪失败，请调整缩放", Toast.LENGTH_LONG).show()
                                        }
                                        isSaving = false
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "异常: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                        isSaving = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(2f).height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        enabled = !isSaving
                    ) {
                        Text(
                            if (isSaving) "正在导出高精图..." else "✨ 确认并保存至相册",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

fun performCrop(bitmap: Bitmap, scale: Float, offset: Offset, containerSize: Size, ratio: Float): Bitmap? {
    return try {
        val pad = 40f
        val availW = max(10f, containerSize.width - pad * 2)
        val availH = max(10f, containerSize.height - pad * 2)
        val cropW = if (availW / availH > ratio) availH * ratio else availW
        val cropH = cropW / ratio
        val cropLeftInView = (containerSize.width - cropW) / 2f
        val cropTopInView = (containerSize.height - cropH) / 2f

        val srcLeft = (cropLeftInView - offset.x) / scale
        val srcTop = (cropTopInView - offset.y) / scale
        val srcWidth = cropW / scale
        val srcHeight = cropH / scale

        val outWidth = max(1, cropW.roundToInt())
        val outHeight = max(1, cropH.roundToInt())
        val result = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(result)
        canvas.drawColor(android.graphics.Color.WHITE)

        val srcR = AndroidRect(srcLeft.toInt(), srcTop.toInt(), (srcLeft + srcWidth).toInt(), (srcTop + srcHeight).toInt())
        val dstR = AndroidRect(0, 0, outWidth, outHeight)
        canvas.drawBitmap(bitmap, srcR, dstR, null)

        result
    } catch (e: Exception) {
        null
    }
}

fun saveCroppedBitmap(context: Context, bitmap: Bitmap) {
    try {
        val filename = "CatCrop_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/A4Cropper")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("无法在系统相册创建条目")

        resolver.openOutputStream(uri)?.use { stream ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                throw IllegalStateException("压缩图片写入失败")
            }
        } ?: throw IllegalStateException("打开输出流失败")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }

        Toast.makeText(context, "✅ 裁切成功！已归档至相册/A4Cropper", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "❌ 保存异常: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun CatToolboxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF0284C7),
            background = Color(0xFFF8FAFC),
            surface = Color.White
        ),
        content = content
    )
}
