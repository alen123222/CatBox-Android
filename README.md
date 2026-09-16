# 🐾 CatBox (灵猫工具箱)

[![Android](https://img.shields.io/badge/Platform-Android_14--16-3DDC84.svg?style=flat&logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack_Compose_M3-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Release](https://img.shields.io/github/v/release/alen123222/CatBox-Android?color=FF4081)](https://github.com/alen123222/CatBox-Android/releases)

**CatBox（灵猫工具箱）** 是一款基于 Jetpack Compose 与 Material 3 架构打造的原生 Android 高颜值多功能工具箱应用。

针对手机与折叠屏/平板设备进行了精细的横竖屏自适应优化（横屏 4 列、竖屏 2 列自适应网格，工具页左右双分栏），界面遵循 Soft UI（轻新拟态）高质感设计规范，追求极致的清爽与无缝交互体验。

---

## ✨ 工具套件一览 (19 合 1)

### 🎨 图像精修工坊
- **📐 A4 智能画幅裁切**：双指手势自由缩放与平移微调，自动基于原始 Bitmap 物理像素无损裁切，智能白底居中补边，一键直存系统相册。
- **🗜️ 图像超轻压缩**：支持 JPEG / WEBP 双算法与自由质量滑块，实时预估输出体积与宽高信息。
- **📏 DPI 与分辨率换算**：快速计算并导出指定物理尺寸与 DPI 下的清晰图片。
- **🎨 二次元取色器**：点选图片任意像素精准获取 HEX / RGB / HSL 颜色值，并一键复制到剪贴板。
- **👾 图片像素画 (Pixel Art)**：从相册选图，自由调节 4px ~ 64px 颗粒粗细，生成复古 8-bit / 16-bit 像素艺术风作品。

### 🔤 文本与编码工具
- **✨ 特殊文本生成**：提供 12+ 种精致花样字符样式（圆圈字、胶囊药丸、哥特艺术字、堆叠重音、颠倒文字、菱形边框等），一键格式化与复制。
- **📻 摩斯密码转换**：标准英数与 Morse 电码的双向编解码。
- **🔐 Base64 编解码**：遵循 RFC 4648 规范，支持 UTF-8 极速编码与还原，带格式校验提示。
- **🪶 迷你文字生成**：一键生成上标 (`ˢᵘᵖᵉʳ`) 与下标 (`ₛᵤᵦ`) 字符，适用于社交昵称与特殊排版。

### 🧭 物理感知与实用测量
- **🧭 3D 拟真指南针**：平滑角度滤波阻尼，显示方位角、八向罗盘及实时偏角读数。
- **⚖️ 气泡水平仪**：高精度陀螺仪与重力传感器支持，实时呈现倾角与平整度反馈。
- **📐 高精刻度尺**：毫米 (mm) / 厘米 (cm) 双制式，自动锁定设备长边无缝丈量。

### 📱 屏幕显示与极简视觉
- **💬 LED 沉浸弹幕**：全屏手持打 call 神器，支持字号、色彩、滚动速度调节与全屏沉浸防误触。
- **🕒 农历极简翻页时钟**：融合中国传统农历、节气与时间显示，支持大屏常亮桌面摆件模式。

### ⚙️ 系统辅助与极客工具
- **🏁 二维码生成**：集成本地离线生成算法，支持 H 级高容错二维码生成与相册保存。
- **📦 APK 应用提取器**：免 Root 扫描并一键提取本机已安装应用的完整 APK 安装包，支持 Android SAF 记忆路径存储。
- **🖼️ 手机壁纸提取**：原画读取当前桌面主屏幕壁纸，一键无损导出为 PNG 到相册。
- **📱 详细设备信息**：聚合查看硬件厂商、主板型号、Android 内核架构、电池实时状态等核心参数。
- **🪫 电量状态伪装**：自由调节滑块伪装任意电量百分比，聚会防借手机必备。
- **📳 触感震动测试**：提供轻触微震、强劲脉冲、心跳律动与 SOS 摩斯救援信号测试马达触感。

---

## 🛠️ 技术栈与架构特性

- **语言**：Kotlin 1.9+
- **构建系统**：Gradle 8.5+ / Android Gradle Plugin 8.5.1
- **UI 框架**：Jetpack Compose + Material 3
- **目标平台**：Android 14 ~ 16 (API 34 ~ 36)
- **核心特性**：
  - **横竖屏自适应**：横屏模式下核心工具界面采用左右分栏布局（左取景/右控制），彻底杜绝拥挤与重叠。
  - **极致轻量**：无冗余商业 SDK，纯原生与轻量算法支撑。
  - **隐私安全**：离线设计，零隐私上传。

---

## 📥 下载安装

前往 [Releases 页面](https://github.com/alen123222/CatBox-Android/releases) 获取最新编译完成的 APK 安装包：

- **最新版**：[CatBox-v1.1.0-debug.apk](https://github.com/alen123222/CatBox-Android/releases/download/v1.1.0/CatBox-v1.1.0-debug.apk)

---

## 📄 开源许可

本项目遵循 [MIT License](LICENSE) 开源协议。
