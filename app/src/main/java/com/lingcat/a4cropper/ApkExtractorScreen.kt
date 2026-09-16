package com.lingcat.a4cropper

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.DecimalFormat

data class InstalledAppItem(
    val name: String,
    val packageName: String,
    val versionName: String,
    val sourceDir: String,
    val sizeBytes: Long,
    val isSystemApp: Boolean,
    val icon: Bitmap?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApkExtractorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("catbox_extractor_prefs", Context.MODE_PRIVATE) }

    var savedPathOption by remember { mutableStateOf(prefs.getString("save_path_option", "DEFAULT_DOWNLOAD") ?: "DEFAULT_DOWNLOAD") }
    var customFolderUriString by remember { mutableStateOf(prefs.getString("custom_folder_uri", "") ?: "") }

    var appList by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("USER") } // USER, ALL, SYSTEM

    var showSettingDialog by remember { mutableStateOf(false) }
    var extractingApp by remember { mutableStateOf<InstalledAppItem?>(null) }
    var isExtracting by remember { mutableStateOf(false) }

    // SAF 目录选择器
    val openDocumentTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) {
                // 忽略异常
            }
            customFolderUriString = uri.toString()
            savedPathOption = "CUSTOM_SAF"
            prefs.edit()
                .putString("save_path_option", "CUSTOM_SAF")
                .putString("custom_folder_uri", uri.toString())
                .apply()
            Toast.makeText(context, "已记住自定义存储目录！", Toast.LENGTH_SHORT).show()
        }
    }

    // 异步加载已安装应用
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val packages: List<PackageInfo> = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
                } else {
                    pm.getInstalledPackages(0)
                }
            } catch (e: Exception) {
                emptyList()
            }

            val items = packages.mapNotNull { pkg ->
                try {
                    val appInfo = pkg.applicationInfo ?: return@mapNotNull null
                    val name = pm.getApplicationLabel(appInfo).toString()
                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val file = File(appInfo.sourceDir)
                    val size = if (file.exists()) file.length() else 0L

                    val rawDrawable = pm.getApplicationIcon(appInfo)
                    val iconBitmap = drawableToBitmap(rawDrawable)

                    InstalledAppItem(
                        name = name,
                        packageName = pkg.packageName,
                        versionName = pkg.versionName ?: "1.0",
                        sourceDir = appInfo.sourceDir,
                        sizeBytes = size,
                        isSystemApp = isSystem,
                        icon = iconBitmap
                    )
                } catch (e: Exception) {
                    null
                }
            }.sortedWith(compareBy<InstalledAppItem> { it.isSystemApp }.thenBy { it.name.lowercase() })

            withContext(Dispatchers.Main) {
                appList = items
                isLoading = false
            }
        }
    }

    val filteredList = remember(appList, searchQuery, filterType) {
        appList.filter { item ->
            val matchType = when (filterType) {
                "USER" -> !item.isSystemApp
                "SYSTEM" -> item.isSystemApp
                else -> true
            }
            val matchSearch = searchQuery.isEmpty() ||
                    item.name.contains(searchQuery, ignoreCase = true) ||
                    item.packageName.contains(searchQuery, ignoreCase = true)
            matchType && matchSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(if (isLandscape) 8.dp else 16.dp)
    ) {
        // 顶栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier
                        .size(if (isLandscape) 34.dp else 40.dp)
                        .shadow(2.dp, CircleShape)
                        .clickable { onBack() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "‹", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = "应用提取 (APK)", fontSize = if (isLandscape) 17.sp else 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    if (!isLandscape) {
                        Text(text = "一键提取已安装安装包 · 纯净导出", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                }
            }

            IconButton(onClick = { showSettingDialog = true }, modifier = Modifier.size(if (isLandscape) 34.dp else 48.dp)) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEFF6FF),
                    modifier = Modifier.size(if (isLandscape) 32.dp else 38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "⚙️", fontSize = if (isLandscape) 15.sp else 18.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(if (isLandscape) 6.dp else 12.dp))

        if (isLandscape) {
            // 横屏下：保存位置卡片与搜索框并排显示，充分利用横向宽度并节省垂直高度
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    modifier = Modifier
                        .weight(1.1f)
                        .height(44.dp)
                        .shadow(1.dp, RoundedCornerShape(12.dp))
                        .clickable { showSettingDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text(text = "📂", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            val desc = if (savedPathOption == "CUSTOM_SAF" && customFolderUriString.isNotEmpty()) {
                                "SAF自定义目录"
                            } else {
                                "默认Download/CatBox_APKs"
                            }
                            Text(text = desc, fontSize = 11.sp, color = Color(0xFF3B82F6), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(text = "修改›", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("搜索应用或包名...", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                    modifier = Modifier
                        .weight(1.2f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    singleLine = true
                )
            }
        } else {
            // 竖屏常规显示
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(16.dp))
                    .clickable { showSettingDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text(text = "📂", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "导出保存位置 (点击更改/记住)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                            val desc = if (savedPathOption == "CUSTOM_SAF" && customFolderUriString.isNotEmpty()) {
                                "已记住自定义目录 (系统授权 SAF)"
                            } else {
                                "默认系统下载目录 (/Download/CatBox_APKs)"
                            }
                            Text(text = desc, fontSize = 11.sp, color = Color(0xFF3B82F6), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Text(text = "修改 ›", fontSize = 12.sp, color = Color(0xFF94A3B8))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 搜索框
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("搜索应用名或包名...", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                ),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 分类 Chip (用户应用 / 全部应用 / 系统应用)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filterType == "USER",
                onClick = { filterType = "USER" },
                label = { Text("用户应用 (${appList.count { !it.isSystemApp }})", fontSize = 12.sp) },
                shape = RoundedCornerShape(10.dp)
            )
            FilterChip(
                selected = filterType == "ALL",
                onClick = { filterType = "ALL" },
                label = { Text("全部 (${appList.size})", fontSize = 12.sp) },
                shape = RoundedCornerShape(10.dp)
            )
            FilterChip(
                selected = filterType == "SYSTEM",
                onClick = { filterType = "SYSTEM" },
                label = { Text("系统应用 (${appList.count { it.isSystemApp }})", fontSize = 12.sp) },
                shape = RoundedCornerShape(10.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 列表展示
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF3B82F6))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "正在检索本地所有已安装应用...", fontSize = 13.sp, color = Color(0xFF64748B))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList) { item ->
                    AppItemRow(
                        item = item,
                        onExtract = {
                            extractingApp = item
                        }
                    )
                }
            }
        }
    }

    // 提取确认与保存位置弹窗（询问并记住）
    if (extractingApp != null) {
        val app = extractingApp!!
        AlertDialog(
            onDismissRequest = { if (!isExtracting) extractingApp = null },
            title = {
                Text(text = "📦 导出 APK 安装包", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(text = "准备提取：${app.name}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(text = "包名：${app.packageName}", fontSize = 11.sp, color = Color(0xFF64748B))
                    Text(text = "体积：${formatFileSize(app.sizeBytes)}", fontSize = 11.sp, color = Color(0xFF64748B))

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(text = "选择本次保存位置：", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                savedPathOption = "DEFAULT_DOWNLOAD"
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = savedPathOption == "DEFAULT_DOWNLOAD",
                            onClick = { savedPathOption = "DEFAULT_DOWNLOAD" }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(text = "默认系统下载目录", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(text = "内部存储/Download/CatBox_APKs", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (customFolderUriString.isEmpty()) {
                                    openDocumentTreeLauncher.launch(null)
                                } else {
                                    savedPathOption = "CUSTOM_SAF"
                                }
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = savedPathOption == "CUSTOM_SAF",
                            onClick = {
                                if (customFolderUriString.isEmpty()) {
                                    openDocumentTreeLauncher.launch(null)
                                } else {
                                    savedPathOption = "CUSTOM_SAF"
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(text = "自定义指定目录 (SAF系统授权)", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = if (customFolderUriString.isNotEmpty()) "已授权指定目录 (可直接导出)" else "点击选择手机任意文件夹",
                                fontSize = 11.sp,
                                color = if (customFolderUriString.isNotEmpty()) Color(0xFF10B981) else Color(0xFF3B82F6)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        var rememberChoice by remember { mutableStateOf(true) }
                        Checkbox(
                            checked = rememberChoice,
                            onCheckedChange = { rememberChoice = it }
                        )
                        Text(text = "记住此位置，下次无需重复确认", fontSize = 12.sp, color = Color(0xFF334155))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isExtracting = true
                        // 记录偏好
                        prefs.edit().putString("save_path_option", savedPathOption).apply()
                        scope.launch {
                            val success = doExtractApk(context, app, savedPathOption, customFolderUriString)
                            isExtracting = false
                            extractingApp = null
                            if (success) {
                                Toast.makeText(context, "🎉 ${app.name} 导出成功！", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "❌ 导出失败，请检查存储权限或存储空间", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isExtracting) "正在提取并导出中..." else "立即导出")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { extractingApp = null },
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isExtracting
                ) {
                    Text("取消")
                }
            }
        )
    }

    // 设置弹窗 (单独配置默认保存位置与清空记忆)
    if (showSettingDialog) {
        AlertDialog(
            onDismissRequest = { showSettingDialog = false },
            title = { Text("⚙️ 导出路径设置", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(text = "配置默认记忆的 APK 导出位置：", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            savedPathOption = "DEFAULT_DOWNLOAD"
                            prefs.edit().putString("save_path_option", "DEFAULT_DOWNLOAD").apply()
                            Toast.makeText(context, "已设为默认下载目录", Toast.LENGTH_SHORT).show()
                            showSettingDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = if (savedPathOption == "DEFAULT_DOWNLOAD") Color(0xFF3B82F6) else Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "设为系统默认下载目录 (/Download/CatBox_APKs)",
                            color = if (savedPathOption == "DEFAULT_DOWNLOAD") Color.White else Color(0xFF334155),
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            openDocumentTreeLauncher.launch(null)
                            showSettingDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = if (savedPathOption == "CUSTOM_SAF") Color(0xFF10B981) else Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "选择/更换自定义存储文件夹...",
                            color = if (savedPathOption == "CUSTOM_SAF") Color.White else Color(0xFF334155),
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingDialog = false }) {
                    Text("完成")
                }
            }
        )
    }
}

@Composable
fun AppItemRow(item: InstalledAppItem, onExtract: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                if (item.icon != null) {
                    Image(
                        bitmap = item.icon.asImageBitmap(),
                        contentDescription = item.name,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE2E8F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "📦", fontSize = 20.sp)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (item.isSystemApp) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFFEF3C7)
                            ) {
                                Text(
                                    text = "系统",
                                    fontSize = 9.sp,
                                    color = Color(0xFFD97706),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${item.versionName} · ${formatFileSize(item.sizeBytes)}",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = item.packageName,
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onExtract,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(text = "提取", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 执行 APK 导出拷贝
suspend fun doExtractApk(
    context: Context,
    app: InstalledAppItem,
    option: String,
    customUriStr: String
): Boolean = withContext(Dispatchers.IO) {
    try {
        val srcFile = File(app.sourceDir)
        if (!srcFile.exists()) return@withContext false

        val cleanName = app.name.replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5]"), "_")
        val apkFileName = "${cleanName}_v${app.versionName}.apk"

        if (option == "CUSTOM_SAF" && customUriStr.isNotEmpty()) {
            val treeUri = Uri.parse(customUriStr)
            val docTree = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext false
            val newFileDoc = docTree.createFile("application/vnd.android.package-archive", apkFileName)
                ?: return@withContext false

            context.contentResolver.openOutputStream(newFileDoc.uri)?.use { outStream ->
                FileInputStream(srcFile).use { inStream ->
                    inStream.copyTo(outStream)
                }
            }
            true
        } else {
            // 默认系统下载目录: Download/CatBox_APKs
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val targetDir = File(downloadDir, "CatBox_APKs")
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            val targetFile = File(targetDir, apkFileName)

            FileInputStream(srcFile).use { inStream ->
                FileOutputStream(targetFile).use { outStream ->
                    inStream.copyTo(outStream)
                }
            }
            true
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun drawableToBitmap(drawable: Drawable): Bitmap? {
    return try {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap
    } catch (e: Exception) {
        null
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val fmt = DecimalFormat("#,##0.0")
    return "${fmt.format(bytes / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
}
