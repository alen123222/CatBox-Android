package com.lingcat.a4cropper

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

// ----------------- 1. 画质压缩工坊 -----------------
@Composable
fun CompressToolScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val coroutineScope = rememberCoroutineScope()
    var rawBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var rawSizeKB by remember { mutableStateOf(0L) }
    var quality by remember { mutableStateOf(80f) }
    var compressedSizeKB by remember { mutableStateOf<Long?>(null) }
    var isCompressing by remember { mutableStateOf(false) }

    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            try {
                val input = context.contentResolver.openInputStream(uri)
                val bytes = input?.readBytes() ?: ByteArray(0)
                rawSizeKB = bytes.size / 1024L
                if (bytes.isNotEmpty()) {
                    rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    compressedSizeKB = null
                }
            } catch (e: Exception) {
                Toast.makeText(context, "读取图片失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(if (isLandscape) 12.dp else 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ToolTopBar(title = "画质压缩", subtitle = "超低损耗极速瘦身归档", onBack = onBack)
        Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 16.dp))

        if (rawBitmap == null) {
            EmptyPickerBox(onPick = {
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            })
        } else {
            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1.2f)
                            .height(230.dp)
                            .clip(RoundedCornerShape(20.dp)),
                        color = Color(0xFF1E293B)
                    ) {
                        Image(
                            bitmap = rawBitmap!!.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        CompressControlPanel(
                            rawSizeKB = rawSizeKB,
                            compressedSizeKB = compressedSizeKB,
                            quality = quality,
                            onQualityChange = { quality = it },
                            onCalcEstimate = {
                                val bmp = rawBitmap
                                if (bmp != null) {
                                    coroutineScope.launch {
                                        val est = withContext(Dispatchers.Default) {
                                            try {
                                                val stream = ByteArrayOutputStream()
                                                bmp.compress(Bitmap.CompressFormat.JPEG, quality.toInt(), stream)
                                                stream.toByteArray().size / 1024L
                                            } catch (e: Exception) {
                                                0L
                                            }
                                        }
                                        compressedSizeKB = est
                                    }
                                }
                            },
                            onReset = { rawBitmap = null },
                            isCompressing = isCompressing,
                            onExport = {
                                val bmp = rawBitmap
                                if (bmp != null) {
                                    isCompressing = true
                                    coroutineScope.launch {
                                        val result = withContext(Dispatchers.Default) {
                                            compressAndSave(context, bmp, quality.toInt())
                                        }
                                        isCompressing = false
                                        if (result.first) {
                                            Toast.makeText(context, "✅ 压缩并保存成功！体积: ${result.second} KB", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    color = Color(0xFF1E293B)
                ) {
                    Image(
                        bitmap = rawBitmap!!.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                CompressControlPanel(
                    rawSizeKB = rawSizeKB,
                    compressedSizeKB = compressedSizeKB,
                    quality = quality,
                    onQualityChange = { quality = it },
                    onCalcEstimate = {
                        val bmp = rawBitmap
                        if (bmp != null) {
                            coroutineScope.launch {
                                val est = withContext(Dispatchers.Default) {
                                    try {
                                        val stream = ByteArrayOutputStream()
                                        bmp.compress(Bitmap.CompressFormat.JPEG, quality.toInt(), stream)
                                        stream.toByteArray().size / 1024L
                                    } catch (e: Exception) {
                                        0L
                                    }
                                }
                                compressedSizeKB = est
                            }
                        }
                    },
                    onReset = { rawBitmap = null },
                    isCompressing = isCompressing,
                    onExport = {
                        val bmp = rawBitmap
                        if (bmp != null) {
                            isCompressing = true
                            coroutineScope.launch {
                                val result = withContext(Dispatchers.Default) {
                                    compressAndSave(context, bmp, quality.toInt())
                                }
                                isCompressing = false
                                if (result.first) {
                                    Toast.makeText(context, "✅ 压缩并保存成功！体积: ${result.second} KB", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CompressControlPanel(
    rawSizeKB: Long,
    compressedSizeKB: Long?,
    quality: Float,
    onQualityChange: (Float) -> Unit,
    onCalcEstimate: () -> Unit,
    onReset: () -> Unit,
    isCompressing: Boolean,
    onExport: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("原始体积: ${rawSizeKB} KB", fontSize = 13.sp, color = Color(0xFF64748B))
                if (compressedSizeKB != null) {
                    Text(
                        "预计: ${compressedSizeKB} KB (-${(100 - (compressedSizeKB * 100 / maxOf(1, rawSizeKB)))}%)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("压缩质量: ${quality.roundToInt()}%", fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), fontSize = 13.sp)
            Slider(
                value = quality,
                onValueChange = onQualityChange,
                valueRange = 10f..95f,
                steps = 16,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFF7043),
                    activeTrackColor = Color(0xFFFF7043)
                )
            )

            Button(
                onClick = onCalcEstimate,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9))
            ) {
                Text("🔍 快速预估体积", color = Color(0xFF334155), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("重选", fontSize = 13.sp)
                }
                Button(
                    onClick = onExport,
                    modifier = Modifier.weight(2f).height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7043)),
                    enabled = !isCompressing
                ) {
                    Text(if (isCompressing) "处理中..." else "✨ 压缩并导出", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

// ----------------- 2. DPI 转换工坊 -----------------
@Composable
fun DpiToolScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val coroutineScope = rememberCoroutineScope()
    var rawBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedDpi by remember { mutableIntStateOf(300) }
    var isExporting by remember { mutableStateOf(false) }

    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            try {
                val input = context.contentResolver.openInputStream(uri)
                rawBitmap = BitmapFactory.decodeStream(input)
            } catch (e: Exception) {
                Toast.makeText(context, "读取图片失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(if (isLandscape) 12.dp else 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ToolTopBar(title = "DPI 转换", subtitle = "高精度打印元数据与像素重采", onBack = onBack)
        Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 16.dp))

        if (rawBitmap == null) {
            EmptyPickerBox(onPick = {
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            })
        } else {
            val bmp = rawBitmap!!
            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1.2f)
                            .height(230.dp)
                            .clip(RoundedCornerShape(20.dp)),
                        color = Color(0xFF1E293B)
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        DpiControlPanel(
                            bmp = bmp,
                            selectedDpi = selectedDpi,
                            onSelectDpi = { selectedDpi = it },
                            onReset = { rawBitmap = null },
                            isExporting = isExporting,
                            onExport = {
                                isExporting = true
                                coroutineScope.launch {
                                    val ok = withContext(Dispatchers.Default) {
                                        exportWithDpi(context, bmp, selectedDpi)
                                    }
                                    isExporting = false
                                    if (ok) {
                                        Toast.makeText(context, "✅ 已导出并注入 ${selectedDpi} DPI 标识！", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    color = Color(0xFF1E293B)
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                DpiControlPanel(
                    bmp = bmp,
                    selectedDpi = selectedDpi,
                    onSelectDpi = { selectedDpi = it },
                    onReset = { rawBitmap = null },
                    isExporting = isExporting,
                    onExport = {
                        isExporting = true
                        coroutineScope.launch {
                            val ok = withContext(Dispatchers.Default) {
                                exportWithDpi(context, bmp, selectedDpi)
                            }
                            isExporting = false
                            if (ok) {
                                Toast.makeText(context, "✅ 已导出并注入 ${selectedDpi} DPI 标识！", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DpiControlPanel(
    bmp: Bitmap,
    selectedDpi: Int,
    onSelectDpi: (Int) -> Unit,
    onReset: () -> Unit,
    isExporting: Boolean,
    onExport: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("当前原始分辨率: ${bmp.width} × ${bmp.height} px", fontSize = 12.sp, color = Color(0xFF64748B))
            Spacer(modifier = Modifier.height(10.dp))
            Text("目标打印精度 (DPI):", fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))

            val dpis = listOf(72, 150, 300, 600)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                dpis.forEach { dpi ->
                    FilterChip(
                        selected = selectedDpi == dpi,
                        onClick = { onSelectDpi(dpi) },
                        label = { Text("$dpi", fontSize = 11.sp) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            val widthInch = bmp.width.toFloat() / selectedDpi
            val heightInch = bmp.height.toFloat() / selectedDpi
            val widthCm = widthInch * 2.54f
            val heightCm = heightInch * 2.54f
            Text(
                "在 ${selectedDpi} DPI 下打印物理尺寸约:\n%.1f × %.1f cm (%.1f × %.1f 英寸)".format(widthCm, heightCm, widthInch, heightInch),
                fontSize = 12.sp,
                color = Color(0xFF3B82F6),
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("重选", fontSize = 13.sp)
                }
                Button(
                    onClick = onExport,
                    modifier = Modifier.weight(2f).height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    enabled = !isExporting
                ) {
                    Text(if (isExporting) "正在写入..." else "🖨️ 导出 ${selectedDpi} DPI", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

// ----------------- 3. 二次元调色板/取色器工坊 -----------------
@Composable
fun ColorExtractToolScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val clipboard = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    var rawBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pickedColor by remember { mutableStateOf(Color(0xFF3B82F6)) }
    var colorPalette by remember { mutableStateOf<List<Color>>(emptyList()) }

    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            try {
                val input = context.contentResolver.openInputStream(uri)
                val bmp = BitmapFactory.decodeStream(input)
                rawBitmap = bmp
                if (bmp != null) {
                    coroutineScope.launch {
                        colorPalette = withContext(Dispatchers.Default) {
                            extractDominantColors(bmp)
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "解析失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(if (isLandscape) 12.dp else 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ToolTopBar(title = "二次元取色器", subtitle = "调色盘提取 · 像素拾色 · Hex复制", onBack = onBack)
        Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 16.dp))

        if (rawBitmap == null) {
            EmptyPickerBox(onPick = {
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            })
        } else {
            val bmp = rawBitmap!!
            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 左侧拾色图
                    Surface(
                        modifier = Modifier
                            .weight(1.2f)
                            .height(240.dp)
                            .clip(RoundedCornerShape(20.dp)),
                        color = Color(0xFF0F172A)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectTapGestures { tapOffset ->
                                            val pixelColor = samplePixelColor(bmp, tapOffset, size.width, size.height)
                                            if (pixelColor != null) {
                                                pickedColor = pixelColor
                                            }
                                        }
                                    }
                            )
                        }
                    }

                    // 右侧颜色卡与调色盘
                    Column(modifier = Modifier.weight(1f)) {
                        ColorInfoAndPalette(
                            pickedColor = pickedColor,
                            colorPalette = colorPalette,
                            onPickColor = { pickedColor = it },
                            onReset = { rawBitmap = null },
                            context = context,
                            clipboard = clipboard
                        )
                    }
                }
            } else {
                // 竖屏
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    color = Color(0xFF0F172A)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures { tapOffset ->
                                        val pixelColor = samplePixelColor(bmp, tapOffset, size.width, size.height)
                                        if (pixelColor != null) {
                                            pickedColor = pixelColor
                                        }
                                    }
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("💡 点击上方图片任意位置即可实时拾取该像素色值", fontSize = 11.sp, color = Color(0xFF64748B))
                Spacer(modifier = Modifier.height(10.dp))

                ColorInfoAndPalette(
                    pickedColor = pickedColor,
                    colorPalette = colorPalette,
                    onPickColor = { pickedColor = it },
                    onReset = { rawBitmap = null },
                    context = context,
                    clipboard = clipboard
                )
            }
        }
    }
}

@Composable
private fun ColorInfoAndPalette(
    pickedColor: Color,
    colorPalette: List<Color>,
    onPickColor: (Color) -> Unit,
    onReset: () -> Unit,
    context: Context,
    clipboard: androidx.compose.ui.platform.ClipboardManager
) {
    val hex = String.format("#%06X", (0xFFFFFF and pickedColor.toArgb()))
    val rgb = "RGB(${((pickedColor.red) * 255).roundToInt()}, ${((pickedColor.green) * 255).roundToInt()}, ${((pickedColor.blue) * 255).roundToInt()})"

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(pickedColor)
                    .border(2.dp, Color(0xFFE2E8F0), CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(hex, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color(0xFF1E293B))
                Spacer(modifier = Modifier.height(2.dp))
                Text(rgb, fontSize = 11.sp, color = Color(0xFF64748B))
            }
            Button(
                onClick = {
                    clipboard.setText(AnnotatedString(hex))
                    Toast.makeText(context, "已复制 $hex 🐾", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
            ) {
                Text("复制", fontSize = 12.sp)
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    Text("自动提取核心调色盘:", fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), fontSize = 13.sp)
    Spacer(modifier = Modifier.height(8.dp))

    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(colorPalette) { c ->
            val colorHex = String.format("#%06X", (0xFFFFFF and c.toArgb()))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable {
                    onPickColor(c)
                    clipboard.setText(AnnotatedString(colorHex))
                    Toast.makeText(context, "选定并复制 $colorHex", Toast.LENGTH_SHORT).show()
                }
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(c)
                        .border(1.dp, Color(0x33000000), RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(colorHex, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF64748B))
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))
    OutlinedButton(
        onClick = onReset,
        modifier = Modifier.fillMaxWidth().height(44.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text("更换图片", fontSize = 13.sp)
    }
}

// ----------------- 通用组件与辅助算法 -----------------

@Composable
fun ToolTopBar(title: String, subtitle: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, Color(0xFFE2E8F0), CircleShape)
        ) {
            Text(text = "‹", fontSize = 26.sp, color = Color(0xFF334155))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            Text(subtitle, fontSize = 11.sp, color = Color(0xFF64748B))
        }
    }
}

@Composable
fun EmptyPickerBox(onPick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .shadow(4.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp)),
        color = Color.White
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(text = "🖼️", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(14.dp))
            Text("轻触从系统相册选取图像", fontSize = 15.sp, color = Color(0xFF475569), fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onPick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("选择照片", fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun compressAndSave(context: Context, bitmap: Bitmap, quality: Int): Pair<Boolean, Long> {
    return try {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        val bytes = stream.toByteArray()
        val sizeKB = (bytes.size / 1024L)

        val filename = "CatCompress_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/A4Cropper")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return Pair(false, 0L)
        resolver.openOutputStream(uri)?.use { it.write(bytes) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        Pair(true, sizeKB)
    } catch (e: Exception) {
        Pair(false, 0L)
    }
}

private fun exportWithDpi(context: Context, bitmap: Bitmap, dpi: Int): Boolean {
    return try {
        bitmap.density = dpi
        val filename = "CatDPI_${dpi}_${System.currentTimeMillis()}.png"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/A4Cropper")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        resolver.openOutputStream(uri)?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        true
    } catch (e: Exception) {
        false
    }
}

private fun samplePixelColor(bitmap: Bitmap, tapOffset: androidx.compose.ui.geometry.Offset, viewW: Int, viewH: Int): Color? {
    if (viewW <= 0 || viewH <= 0) return null
    val scale = minOf(viewW.toFloat() / bitmap.width, viewH.toFloat() / bitmap.height)
    val realW = bitmap.width * scale
    val realH = bitmap.height * scale
    val offsetX = (viewW - realW) / 2f
    val offsetY = (viewH - realH) / 2f

    val bmpX = ((tapOffset.x - offsetX) / scale).toInt()
    val bmpY = ((tapOffset.y - offsetY) / scale).toInt()

    if (bmpX in 0 until bitmap.width && bmpY in 0 until bitmap.height) {
        val pixel = bitmap.getPixel(bmpX, bmpY)
        return Color(pixel)
    }
    return null
}

private fun extractDominantColors(bitmap: Bitmap): List<Color> {
    val sample = Bitmap.createScaledBitmap(bitmap, 40, 40, false)
    val colorCounts = mutableMapOf<Int, Int>()
    for (x in 0 until sample.width step 2) {
        for (y in 0 until sample.height step 2) {
            val color = sample.getPixel(x, y)
            // 简单量化合并邻近色
            val r = (android.graphics.Color.red(color) / 32) * 32
            val g = (android.graphics.Color.green(color) / 32) * 32
            val b = (android.graphics.Color.blue(color) / 32) * 32
            val quantized = android.graphics.Color.rgb(r, g, b)
            colorCounts[quantized] = (colorCounts[quantized] ?: 0) + 1
        }
    }
    return colorCounts.entries
        .sortedByDescending { it.value }
        .take(6)
        .map { Color(it.key) }
}
