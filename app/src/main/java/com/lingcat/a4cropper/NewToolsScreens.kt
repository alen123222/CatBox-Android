package com.lingcat.a4cropper

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// -------------------------------------------------------------
// 通用二级页面骨架组件
// -------------------------------------------------------------
@Composable
fun ToolScaffold(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = if (isLandscape) 24.dp else 16.dp, vertical = if (isLandscape) 8.dp else 14.dp)
    ) {
        // 顶部导航栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier
                    .size(if (isLandscape) 34.dp else 40.dp)
                    .shadow(2.dp, CircleShape)
                    .clickable { onBack() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "‹", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    fontSize = if (isLandscape) 18.sp else 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }
        }

        Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 14.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            content()
        }
    }
}

// -------------------------------------------------------------
// 1. 特殊文本生成 (严格复刻并超越用户提供的UI样式)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecialTextScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var inputText by remember { mutableStateOf("测试测试") }
    var selectedStyleIndex by remember { mutableIntStateOf(0) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val transformedText = remember(inputText, selectedStyleIndex) {
        TextToolsHelper.transformSpecialText(inputText, selectedStyleIndex)
    }

    ToolScaffold(title = "特殊文本生成", subtitle = "花样艺术字体 · 符号边框一键转换", onBack = onBack) {
        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // 左侧配置区
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        label = { Text("请输入文本内容") },
                        placeholder = { Text("输入要转换的文字...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = false
                    )

                    // 样式选择下拉选择框
                    ExposedDropdownMenuBox(
                        expanded = isDropdownExpanded,
                        onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = TextToolsHelper.STYLE_NAMES[selectedStyleIndex],
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("请选择样式") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = isDropdownExpanded,
                            onDismissRequest = { isDropdownExpanded = false }
                        ) {
                            TextToolsHelper.STYLE_NAMES.forEachIndexed { index, name ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "$name: ${TextToolsHelper.transformSpecialText("测试", index)}",
                                            fontWeight = if (selectedStyleIndex == index) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        selectedStyleIndex = index
                                        isDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 右侧预览与复制区
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .shadow(2.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("转换结果预览：", fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = transformedText.ifEmpty { "（无预览）" },
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    textAlign = TextAlign.Center
                                )
                            }
                            Button(
                                onClick = {
                                    clipboard.setText(AnnotatedString(transformedText))
                                    Toast.makeText(context, "已复制特殊文本至剪贴板！", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
                            ) {
                                Text("一键复制文本", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("请输入文本内容") },
                    placeholder = { Text("输入要转换的文字...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = false,
                    leadingIcon = { Text("Tᴛ", fontWeight = FontWeight.Bold, color = Color(0xFF64748B)) }
                )

                // 样式选择下拉框
                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded,
                    onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = "${TextToolsHelper.STYLE_NAMES[selectedStyleIndex]}  (${TextToolsHelper.transformSpecialText("测试", selectedStyleIndex)})",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("请选择样式") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        TextToolsHelper.STYLE_NAMES.forEachIndexed { index, name ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = name, fontSize = 13.sp)
                                        Text(
                                            text = TextToolsHelper.transformSpecialText("测试测试", index),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0284C7)
                                        )
                                    }
                                },
                                onClick = {
                                    selectedStyleIndex = index
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // 结果卡片
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("转换效果预览：", fontSize = 13.sp, color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp)),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = transformedText.ifEmpty { "请在上方输入内容" },
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Button(
                            onClick = {
                                clipboard.setText(AnnotatedString(transformedText))
                                Toast.makeText(context, "已复制特殊文本至剪贴板！", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
                        ) {
                            Text("一键复制文本", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. 摩斯密码编解码
// -------------------------------------------------------------
@Composable
fun MorseCodeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var input by remember { mutableStateOf("SOS CAT") }
    var output by remember { mutableStateOf(TextToolsHelper.encodeMorse("SOS CAT")) }

    ToolScaffold(title = "摩斯密码转换", subtitle = "点横电报码 · 双向极速翻译", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("输入内容 (英文/数字 或 摩斯码)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                minLines = 3
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { output = TextToolsHelper.encodeMorse(input) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Text("编码为摩斯码", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { output = TextToolsHelper.decodeMorse(input) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("解码摩斯码", fontWeight = FontWeight.Bold)
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = Color.White
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("翻译结果：", fontSize = 13.sp, color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        color = Color(0xFFF8FAFC)
                    ) {
                        Text(
                            text = output.ifEmpty { "（空）" },
                            modifier = Modifier.padding(14.dp),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF1E293B)
                        )
                    }
                    Button(
                        onClick = {
                            clipboard.setText(AnnotatedString(output))
                            Toast.makeText(context, "已复制结果", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                    ) {
                        Text("复制结果")
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. Base64 加解密
// -------------------------------------------------------------
@Composable
fun Base64Screen(onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var input by remember { mutableStateOf("CatBox 灵猫工具箱") }
    var output by remember { mutableStateOf(TextToolsHelper.encodeBase64("CatBox 灵猫工具箱")) }

    ToolScaffold(title = "Base64 加解密", subtitle = "标准 RFC 4648 · UTF-8 极速转换", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("输入要转换的字符") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                minLines = 3
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { output = TextToolsHelper.encodeBase64(input) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
                ) {
                    Text("Base64 编码", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { output = TextToolsHelper.decodeBase64(input) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                ) {
                    Text("Base64 解码", fontWeight = FontWeight.Bold)
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = Color.White
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("转换结果：", fontSize = 13.sp, color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        color = Color(0xFFF8FAFC)
                    ) {
                        Text(
                            text = output.ifEmpty { "（空）" },
                            modifier = Modifier.padding(14.dp),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color(0xFF1E293B)
                        )
                    }
                    Button(
                        onClick = {
                            clipboard.setText(AnnotatedString(output))
                            Toast.makeText(context, "已复制结果", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("复制结果")
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. 迷你文字生成 (上标 / 下标)
// -------------------------------------------------------------
@Composable
fun MiniTextScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var input by remember { mutableStateOf("CatBox 2026") }
    var isSubscript by remember { mutableStateOf(false) }

    val output = remember(input, isSubscript) {
        TextToolsHelper.toMiniText(input, isSubscript)
    }

    ToolScaffold(title = "迷你文字生成", subtitle = "微信小尾巴 · 极小上标与下标", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("输入普通英数文字") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                minLines = 2
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = !isSubscript,
                    onClick = { isSubscript = false },
                    label = { Text("上标模式 ˢᵘᵖᵉʳ") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                FilterChip(
                    selected = isSubscript,
                    onClick = { isSubscript = true },
                    label = { Text("下标模式 ₛᵤᵦ") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = Color.White
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("迷你小字预览：", fontSize = 13.sp, color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        color = Color(0xFFF1F5F9)
                    ) {
                        Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = output.ifEmpty { "（空）" },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }
                    Button(
                        onClick = {
                            clipboard.setText(AnnotatedString(output))
                            Toast.makeText(context, "已复制迷你文字！", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                    ) {
                        Text("一键复制迷你小字", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 5. 二维码生成
// -------------------------------------------------------------
@Composable
fun QrCodeGeneratorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var contentText by remember { mutableStateOf("https://lingcat.win") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(contentText) {
        if (contentText.isNotEmpty()) {
            withContext(Dispatchers.Default) {
                qrBitmap = SystemToolsHelper.generateQrCode(contentText, 600)
            }
        }
    }

    ToolScaffold(title = "二维码生成", subtitle = "高精容错 · 纯本地极速离线生成", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = contentText,
                onValueChange = { contentText = it },
                label = { Text("输入网址、名片或文字内容") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                minLines = 2
            )

            Surface(
                modifier = Modifier
                    .size(240.dp)
                    .shadow(3.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = Color.White
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(16.dp)) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        CircularProgressIndicator(color = Color(0xFF3B82F6))
                    }
                }
            }

            Button(
                onClick = {
                    val bmp = qrBitmap
                    if (bmp != null) {
                        coroutineScope.launch {
                            val ok = saveBitmapToGallery(context, bmp, "CatQR_${System.currentTimeMillis()}.png")
                            if (ok) Toast.makeText(context, "二维码已保存至相册！", Toast.LENGTH_SHORT).show()
                            else Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text("保存二维码到相册", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -------------------------------------------------------------
// 6. 图片像素画 (Pixel Art)
// -------------------------------------------------------------
@Composable
fun PixelArtScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pixelSize by remember { mutableFloatStateOf(16f) }
    var resultBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val bmp = loadBitmapFromUri(context, uri)
            sourceBitmap = bmp
            if (bmp != null) {
                resultBitmap = SystemToolsHelper.convertToPixelArt(bmp, pixelSize.toInt())
            }
        }
    }

    ToolScaffold(title = "图片像素画工坊", subtitle = "复古像素风 · 块状像素艺术变换", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (sourceBitmap == null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { pickMediaLauncher.launch("image/*") },
                    color = Color.White
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🖼️", fontSize = 42.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("点按选择要转换的图片", fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                        Text("支持任意相片一键转换 8-bit / 16-bit 风格", fontSize = 11.sp, color = Color(0xFF94A3B8))
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
                    if (resultBitmap != null) {
                        Image(
                            bitmap = resultBitmap!!.asImageBitmap(),
                            contentDescription = "Pixel Art",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("像素颗粒大小：", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("${pixelSize.toInt()} px", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6366F1))
                        }

                        Slider(
                            value = pixelSize,
                            onValueChange = {
                                pixelSize = it
                                val src = sourceBitmap
                                if (src != null) {
                                    resultBitmap = SystemToolsHelper.convertToPixelArt(src, pixelSize.toInt())
                                }
                            },
                            valueRange = 4f..64f,
                            steps = 15
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { pickMediaLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("更换图片")
                            }

                            Button(
                                onClick = {
                                    val res = resultBitmap
                                    if (res != null) {
                                        coroutineScope.launch {
                                            val ok = saveBitmapToGallery(context, res, "CatPixel_${System.currentTimeMillis()}.png")
                                            if (ok) Toast.makeText(context, "像素画已保存至相册！", Toast.LENGTH_SHORT).show()
                                            else Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                            ) {
                                Text("保存像素画")
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 7. 电量伪装 (即时系统状态栏覆盖 & 沉浸防借神器)
// -------------------------------------------------------------
@Composable
fun FakeBatteryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var fakePercent by remember { mutableIntStateOf(1) }
    var isCharging by remember { mutableStateOf(false) }
    var isLowBatteryMode by remember { mutableStateOf(true) }
    var isOverlayEnabled by remember { mutableStateOf(FakeStatusBarService.isRunning) }

    val updateOverlay = { enabled: Boolean, pct: Int, charging: Boolean, lowPwr: Boolean ->
        if (enabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                Toast.makeText(context, "请先授予「悬浮窗 / 在其他应用上层显示」权限以覆盖状态栏喵！", Toast.LENGTH_LONG).show()
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            } else {
                val serviceIntent = Intent(context, FakeStatusBarService::class.java).apply {
                    action = FakeStatusBarService.ACTION_START_OR_UPDATE
                    putExtra(FakeStatusBarService.EXTRA_LEVEL, pct)
                    putExtra(FakeStatusBarService.EXTRA_CHARGING, charging)
                    putExtra(FakeStatusBarService.EXTRA_LOW_POWER, lowPwr)
                    putExtra(FakeStatusBarService.EXTRA_DARK, true)
                }
                context.startService(serviceIntent)
                isOverlayEnabled = true
            }
        } else {
            val stopIntent = Intent(context, FakeStatusBarService::class.java).apply {
                action = FakeStatusBarService.ACTION_STOP
            }
            context.startService(stopIntent)
            isOverlayEnabled = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // 保留后台运行或可选择退出时维持状态
        }
    }

    ToolScaffold(title = "电量伪装", subtitle = "系统状态栏即时伪装 · 聚会防借手机神器", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .shadow(3.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = if (fakePercent <= 10) Color(0xFF1E293B) else Color(0xFF0F172A)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isCharging) "⚡" else if (fakePercent <= 5) "🪫" else "🔋",
                        fontSize = 54.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$fakePercent%",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        color = if (fakePercent <= 10) Color(0xFFEF4444) else Color(0xFF10B981)
                    )
                    Text(
                        text = if (isOverlayEnabled) "🟢 状态栏即时伪装已开启 (长按/拖动可自由调整位置)" else "⚪ 状态栏即时伪装未开启",
                        fontSize = 12.sp,
                        color = if (isOverlayEnabled) Color(0xFF34D399) else Color(0xFF94A3B8)
                    )
                }
            }

            // 状态栏实时开关卡片
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = Color.White
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("系统状态栏实时悬浮伪装", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1E293B))
                            Text("修改后在整个手机顶部状态栏即时生效", fontSize = 12.sp, color = Color(0xFF64748B))
                        }
                        Switch(
                            checked = isOverlayEnabled,
                            onCheckedChange = { checked ->
                                updateOverlay(checked, fakePercent, isCharging, isLowBatteryMode)
                            }
                        )
                    }

                    if (isOverlayEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("💡 长按/拖拽顶部电量胶囊可任意移动位置", fontSize = 11.sp, color = Color(0xFF475569))
                            TextButton(
                                onClick = {
                                    val prefs = context.getSharedPreferences("FakeStatusBarPrefs", Context.MODE_PRIVATE)
                                    prefs.edit().clear().apply()
                                    updateOverlay(false, fakePercent, isCharging, isLowBatteryMode)
                                    updateOverlay(true, fakePercent, isCharging, isLowBatteryMode)
                                    Toast.makeText(context, "已复位至右上角默认位置", Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                            ) {
                                Text("复位位置", fontSize = 11.sp, color = Color(0xFF3B82F6))
                            }
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = Color.White
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("调整伪装电量：", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = fakePercent == 1,
                            onClick = {
                                fakePercent = 1
                                if (isOverlayEnabled) updateOverlay(true, 1, isCharging, isLowBatteryMode)
                            },
                            label = { Text("1% 极限濒死") },
                            shape = RoundedCornerShape(10.dp)
                        )
                        FilterChip(
                            selected = fakePercent == 3,
                            onClick = {
                                fakePercent = 3
                                if (isOverlayEnabled) updateOverlay(true, 3, isCharging, isLowBatteryMode)
                            },
                            label = { Text("3% 关机预警") },
                            shape = RoundedCornerShape(10.dp)
                        )
                        FilterChip(
                            selected = fakePercent == 100,
                            onClick = {
                                fakePercent = 100
                                if (isOverlayEnabled) updateOverlay(true, 100, isCharging, isLowBatteryMode)
                            },
                            label = { Text("100% 满血") },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Slider(
                        value = fakePercent.toFloat(),
                        onValueChange = {
                            fakePercent = it.toInt()
                            if (isOverlayEnabled) updateOverlay(true, fakePercent, isCharging, isLowBatteryMode)
                        },
                        valueRange = 1f..100f
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("伪装充电中 ⚡", fontSize = 13.sp, color = Color(0xFF334155))
                        Switch(
                            checked = isCharging,
                            onCheckedChange = {
                                isCharging = it
                                if (isOverlayEnabled) updateOverlay(true, fakePercent, isCharging, isLowBatteryMode)
                            }
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 8. 震动器 (多模式触感回馈测试)
// -------------------------------------------------------------
@Composable
fun VibratorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var activeMode by remember { mutableIntStateOf(-1) }

    DisposableEffect(Unit) {
        onDispose {
            SystemToolsHelper.stopVibrate(context)
        }
    }

    ToolScaffold(title = "震动测试器", subtitle = "马达体检 · 节奏触感测试", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val modes = listOf(
                "轻触微震 (100ms)" to "轻柔反馈，模拟打字或按键触感",
                "强劲长震 (500ms)" to "高强度持续震动，测试马达最大振幅",
                "心跳律动 (Heartbeat)" to "仿生双击脉冲节奏",
                "SOS 救援摩斯电报" to "三短三长三短的莫尔斯振动序列"
            )

            modes.forEachIndexed { index, (title, desc) ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(18.dp))
                        .clickable {
                            activeMode = index
                            SystemToolsHelper.vibrate(context, index)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E293B))
                            Text(text = desc, fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                        Button(
                            onClick = {
                                activeMode = index
                                SystemToolsHelper.vibrate(context, index)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E))
                        ) {
                            Text("震动")
                        }
                    }
                }
            }

            Button(
                onClick = {
                    activeMode = -1
                    SystemToolsHelper.stopVibrate(context)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64748B))
            ) {
                Text("立即停止震动")
            }
        }
    }
}

// -------------------------------------------------------------
// 9. 提取手机壁纸
// -------------------------------------------------------------
@Composable
fun WallpaperExtractorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var wallpaperBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var lockWallpaperBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: 桌面壁纸, 1: 锁屏壁纸
    var isLoading by remember { mutableStateOf(true) }
    var hasPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        )
    }
    val coroutineScope = rememberCoroutineScope()

    val loadWallpapers = {
        isLoading = true
        coroutineScope.launch(Dispatchers.IO) {
            val sysBmp = WallpaperHelper.getWallpaperBitmap(context, WallpaperManager.FLAG_SYSTEM)
            val lockBmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                WallpaperHelper.getWallpaperBitmap(context, WallpaperManager.FLAG_LOCK)
            } else null

            withContext(Dispatchers.Main) {
                wallpaperBitmap = sysBmp
                lockWallpaperBitmap = lockBmp
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadWallpapers()
    }

    val currentDisplayBitmap = if (selectedTab == 0) wallpaperBitmap else (lockWallpaperBitmap ?: wallpaperBitmap)

    ToolScaffold(title = "提取手机壁纸", subtitle = "原画无损提取桌面与锁屏壁纸", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 权限提示引导条
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPermission) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFEF3C7),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Android 13+ 需要文件存储权限以读取原图壁纸",
                            fontSize = 11.sp,
                            color = Color(0xFF92400E),
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } else {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }) {
                            Text("去授权", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Tab 切换：主屏 / 锁屏
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = { Text("主屏幕壁纸") },
                    shape = RoundedCornerShape(10.dp)
                )
                FilterChip(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    label = { Text("锁屏壁纸") },
                    shape = RoundedCornerShape(10.dp)
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(320.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF3B82F6))
                }
            } else if (currentDisplayBitmap != null) {
                val bmp = currentDisplayBitmap
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    color = Color.Black
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Current Wallpaper",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Text(
                    text = "分辨率: ${bmp.width} × ${bmp.height} 像素",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )

                Button(
                    onClick = {
                        coroutineScope.launch {
                            val filename = if (selectedTab == 0) "HomeWallpaper_${System.currentTimeMillis()}.png" else "LockWallpaper_${System.currentTimeMillis()}.png"
                            val ok = saveBitmapToGallery(context, bmp, filename)
                            if (ok) Toast.makeText(context, "壁纸已无损保存至相册！", Toast.LENGTH_SHORT).show()
                            else Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                ) {
                    Text("导出当前壁纸至相册 (PNG无损原画)", fontWeight = FontWeight.Bold)
                }
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    color = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("未能直接读取到壁纸数据", fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Android 13+ 系统对动态壁纸或系统默认壁纸实施沙箱保护。若您使用的是静态壁纸，请点击下方授予全部文件管理权限重试喵~",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { loadWallpapers() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("重新尝试读取")
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 10. 查看设备信息
// -------------------------------------------------------------
@Composable
fun DeviceInfoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val infoCategories = remember { SystemToolsHelper.getDeviceInfo(context) }

    ToolScaffold(title = "设备信息", subtitle = "硬件参数 · 操作系统与电池状态", onBack = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(infoCategories) { cat ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(18.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = cat.category, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        cat.items.forEach { (label, value) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = label, fontSize = 12.sp, color = Color(0xFF64748B))
                                Text(
                                    text = value,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.clickable {
                                        clipboard.setText(AnnotatedString(value))
                                        Toast.makeText(context, "已复制 $value", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

// -------------------------------------------------------------
// 内部工具函数
// -------------------------------------------------------------
private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        }
    } catch (e: Exception) {
        null
    }
}

private suspend fun saveBitmapToGallery(context: Context, bitmap: Bitmap, filename: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/CatBox")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext false
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
}
