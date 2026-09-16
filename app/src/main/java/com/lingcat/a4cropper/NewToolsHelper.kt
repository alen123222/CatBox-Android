package com.lingcat.a4cropper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.os.BatteryManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Base64
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.nio.charset.StandardCharsets

object TextToolsHelper {

    // 1. 特殊文本生成 (多达12种精美样式)
    val STYLE_NAMES = listOf(
        "圆圈字符 Ⓣⓔⓧⓣ",
        "胶囊药丸 [ ᴛᴇxᴛ ]",
        "日系前缀 a' ゞ",
        "堆叠重音 Ṫëẍẗ",
        "菱形边框 ◈Text◈",
        "下划线线 t̲e̲x̲t̲",
        "气泡云朵 ҉ ☁",
        "删除线划 t̶e̶x̶t̶",
        "花藤羽翼 ༺Text༻",
        "反转颠倒 ʇxǝꓕ",
        "全角宽体 Ｔｅｘｔ",
        "哥特字体 𝕿𝖊𝖝𝖙"
    )

    fun transformSpecialText(input: String, styleIndex: Int): String {
        if (input.isEmpty()) return ""
        return when (styleIndex) {
            0 -> toCircled(input)
            1 -> "[ " + toSmallCaps(input) + " ]"
            2 -> "a' ゞ $input"
            3 -> input.map { "$it\u0307" }.joinToString("")
            4 -> "◈ " + input.toCharArray().joinToString(" ") + " ◈"
            5 -> input.map { "$it\u0332" }.joinToString("")
            6 -> "☁ ҉ " + input.map { "$it\u0489" }.joinToString("") + " ҉ ☁"
            7 -> input.map { "$it\u0336" }.joinToString("")
            8 -> "༺ " + input + " ༻"
            9 -> toUpsideDown(input)
            10 -> toFullWidth(input)
            11 -> toFraktur(input)
            else -> input
        }
    }

    private fun toCircled(text: String): String {
        val sb = StringBuilder()
        for (ch in text) {
            sb.append(
                when (ch) {
                    in 'a'..'z' -> Character.toChars(0x24D0 + (ch - 'a'))
                    in 'A'..'Z' -> Character.toChars(0x24B6 + (ch - 'A'))
                    in '1'..'9' -> Character.toChars(0x2460 + (ch - '1'))
                    '0' -> Character.toChars(0x24EA)
                    else -> charArrayOf(ch)
                }
            )
        }
        return sb.toString()
    }

    private fun toSmallCaps(text: String): String {
        val map = mapOf(
            'a' to 'ᴀ', 'b' to 'ʙ', 'c' to 'ᴄ', 'd' to 'ᴅ', 'e' to 'ᴇ',
            'f' to 'ꜰ', 'g' to 'ɢ', 'h' to 'ʜ', 'i' to 'ɪ', 'j' to 'ᴊ',
            'k' to 'ᴋ', 'l' to 'ʟ', 'm' to 'ᴍ', 'n' to 'ɴ', 'o' to 'ᴏ',
            'p' to 'ᴘ', 'q' to 'ǫ', 'r' to 'ʀ', 's' to 's', 't' to 'ᴛ',
            'u' to 'ᴜ', 'v' to 'ᴠ', 'w' to 'ᴡ', 'x' to 'x', 'y' to 'ʏ', 'z' to 'ᴢ'
        )
        return text.map { map[it.lowercaseChar()] ?: it }.joinToString("")
    }

    private fun toUpsideDown(text: String): String {
        val normal = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789?,.!'\"()[]{}<>"
        val flipped = "ɐqɔpǝɟɓɥıɾʞןɯuodbɹsʇnʌʍxʎz∀ᗺƆᗡƎℲ⅁HIſʞ˥WNOԀÒᴚS⊥∩ΛMX⅄Z0ƖᄅƐㄣϛ9ㄥ86¿'˙¡,„)(][}{><"
        val sb = StringBuilder()
        for (i in text.length - 1 downTo 0) {
            val ch = text[i]
            val idx = normal.indexOf(ch)
            if (idx != -1) sb.append(flipped[idx]) else sb.append(ch)
        }
        return sb.toString()
    }

    private fun toFullWidth(text: String): String {
        val sb = StringBuilder()
        for (c in text) {
            if (c.code in 33..126) {
                sb.append((c.code + 65248).toChar())
            } else if (c == ' ') {
                sb.append('\u3000')
            } else {
                sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun toFraktur(text: String): String {
        val normal = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val gothic = arrayOf(
            "𝔞","𝔟","𝔠","𝔡","𝔢","𝔣","𝔤","𝔥","𝔦","𝔧","𝔨","𝔩","𝔪","𝔫","𝔬","𝔭","𝔮","𝔯","𝔰","𝔱","𝔲","𝔳","𝔴","𝔵","𝔶","𝔷",
            "𝕬","𝕭","𝕮","𝕯","𝕰","𝕱","𝕲","𝕳","𝕴","𝕵","𝕶","𝕷","𝕸","𝕹","𝕺","𝕻","𝕼","𝕽","𝕾","𝕿","𝖀","𝖁","𝖂","𝖃","𝖄","𝖅"
        )
        val sb = StringBuilder()
        for (ch in text) {
            val idx = normal.indexOf(ch)
            if (idx != -1) sb.append(gothic[idx]) else sb.append(ch)
        }
        return sb.toString()
    }

    // 2. 迷你文字生成 (上标 / 下标)
    val MINI_UPPER_MAP = mapOf(
        '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
        '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
        '+' to '⁺', '-' to '⁻', '=' to '⁼', '(' to '⁽', ')' to '⁾',
        'a' to 'ᵃ', 'b' to 'ᵇ', 'c' to 'ᶜ', 'd' to 'ᵈ', 'e' to 'ᵉ', 'f' to 'ᶠ',
        'g' to 'ᵍ', 'h' to 'ʰ', 'i' to 'ⁱ', 'j' to 'ʲ', 'k' to 'ᵏ', 'l' to 'ˡ',
        'm' to 'ᵐ', 'n' to 'ⁿ', 'o' to 'ᵒ', 'p' to 'ᵖ', 'r' to 'ʳ', 's' to 'ˢ',
        't' to 'ᵗ', 'u' to 'ᵘ', 'v' to 'ᵛ', 'w' to 'ʷ', 'x' to 'ˣ', 'y' to 'ʸ', 'z' to 'ᶻ',
        'A' to 'ᴬ', 'B' to 'ᴮ', 'D' to 'ᴰ', 'E' to 'ᴱ', 'G' to 'ᴳ', 'H' to 'ᴴ',
        'I' to 'ᴵ', 'J' to 'ᴶ', 'K' to 'ᴷ', 'L' to 'ᴸ', 'M' to 'ᴹ', 'N' to 'ᴺ',
        'O' to 'ᴼ', 'P' to 'ᴾ', 'R' to 'ᴿ', 'T' to 'ᵀ', 'U' to 'ᵁ', 'W' to 'ᵂ'
    )

    val MINI_LOWER_MAP = mapOf(
        '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄',
        '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
        '+' to '₊', '-' to '₋', '=' to '₌', '(' to '₍', ')' to '₎',
        'a' to 'ₐ', 'e' to 'ₑ', 'h' to 'ₕ', 'i' to 'ᵢ', 'j' to 'ⱼ', 'k' to 'ₖ',
        'l' to 'ₗ', 'm' to 'ₘ', 'n' to 'ₙ', 'o' to 'ₒ', 'p' to 'ₚ', 'r' to 'ᵣ',
        's' to 'ₛ', 't' to 'ₜ', 'u' to 'ᵤ', 'v' to 'ᵥ', 'x' to 'ₓ'
    )

    fun toMiniText(input: String, isSubscript: Boolean = false): String {
        val targetMap = if (isSubscript) MINI_LOWER_MAP else MINI_UPPER_MAP
        return input.map { targetMap[it] ?: it }.joinToString("")
    }

    // 3. 摩斯密码加解密
    private val MORSE_MAP = mapOf(
        'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".",
        'F' to "..-.", 'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---",
        'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---",
        'P' to ".--.", 'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-",
        'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-", 'Y' to "-.--",
        'Z' to "--..", '0' to "-----", '1' to ".----", '2' to "..---",
        '3' to "...--", '4' to "....-", '5' to ".....", '6' to "-....",
        '7' to "--...", '8' to "---..", '9' to "----.", '.' to ".-.-.-",
        ',' to "--..--", '?' to "..--..", '/' to "-..-.", '-' to "-....-",
        ' ' to "/"
    )
    private val REVERSE_MORSE = MORSE_MAP.entries.associate { (k, v) -> v to k }

    fun encodeMorse(text: String): String {
        return text.uppercase().map { ch ->
            MORSE_MAP[ch] ?: ch.toString()
        }.joinToString(" ")
    }

    fun decodeMorse(morse: String): String {
        val tokens = morse.trim().split("\\s+".toRegex())
        val sb = StringBuilder()
        for (token in tokens) {
            if (token == "/") {
                sb.append(" ")
            } else {
                val ch = REVERSE_MORSE[token]
                if (ch != null) sb.append(ch) else sb.append(token)
            }
        }
        return sb.toString()
    }

    // 4. Base64 加解密
    fun encodeBase64(input: String): String {
        return try {
            Base64.encodeToString(input.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
        } catch (e: Exception) {
            "编码错误: ${e.message}"
        }
    }

    fun decodeBase64(input: String): String {
        return try {
            val bytes = Base64.decode(input.trim(), Base64.NO_WRAP)
            String(bytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            "解码错误: 格式不合法或包含非法字符"
        }
    }
}

object SystemToolsHelper {

    // 5. 设备信息获取
    data class DeviceDetail(val category: String, val items: List<Pair<String, String>>)

    fun getDeviceInfo(context: Context): List<DeviceDetail> {
        val list = mutableListOf<DeviceDetail>()

        // 基础信息
        val basic = listOf(
            "设备品牌 (Brand)" to Build.BRAND,
            "生产厂商 (Manufacturer)" to Build.MANUFACTURER,
            "设备型号 (Model)" to Build.MODEL,
            "主板型号 (Board)" to Build.BOARD,
            "设备代号 (Device)" to Build.DEVICE,
            "硬件名称 (Hardware)" to Build.HARDWARE
        )
        list.add(DeviceDetail("📱 基础硬件", basic))

        // 系统信息
        val system = listOf(
            "Android 版本" to "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            "安全补丁级别" to (Build.VERSION.SECURITY_PATCH ?: "未知"),
            "系统构建版本" to Build.DISPLAY,
            "内核架构" to System.getProperty("os.arch").orEmpty(),
            "支持 ABI" to Build.SUPPORTED_ABIS.joinToString(", ")
        )
        list.add(DeviceDetail("⚙️ 系统与固件", system))

        // 电池电量真实状态
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryPct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val isCharging = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) == BatteryManager.BATTERY_STATUS_CHARGING
        } else false

        val battery = listOf(
            "当前电量" to if (batteryPct >= 0) "$batteryPct%" else "无法读取",
            "充电状态" to if (isCharging) "正在充电 ⚡" else "未充电"
        )
        list.add(DeviceDetail("🔋 电池健康", battery))

        return list
    }

    // 6. 振动控制器
    fun vibrate(context: Context, patternType: Int) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return

        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (patternType) {
                0 -> vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)) // 短促
                1 -> vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)) // 强劲
                2 -> { // 心跳
                    val timings = longArrayOf(0, 150, 100, 150, 600)
                    val amplitudes = intArrayOf(0, 255, 0, 180, 0)
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                }
                3 -> { // SOS 摩斯密码
                    val timings = longArrayOf(0, 100, 100, 100, 100, 100, 300, 300, 100, 300, 100, 300, 300, 100, 100, 100, 100, 100)
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
                }
            }
        } else {
            @Suppress("DEPRECATION")
            when (patternType) {
                0 -> vibrator.vibrate(100)
                1 -> vibrator.vibrate(500)
                2 -> vibrator.vibrate(longArrayOf(0, 150, 100, 150), -1)
                3 -> vibrator.vibrate(longArrayOf(0, 100, 100, 100, 100, 100, 300, 300, 100, 300), -1)
            }
        }
    }

    fun stopVibrate(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        vibrator?.cancel()
    }

    // 7. 二维码生成算法 (ZXing)
    fun generateQrCode(content: String, size: Int = 512, qrColor: Int = AndroidColor.BLACK, bgColor: Int = AndroidColor.WHITE): Bitmap? {
        return try {
            val hints = hashMapOf<EncodeHintType, Any>()
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
            hints[EncodeHintType.MARGIN] = 1

            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
            val pixels = IntArray(size * size)
            for (y in 0 until size) {
                for (x in 0 until size) {
                    pixels[y * size + x] = if (bitMatrix[x, y]) qrColor else bgColor
                }
            }
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    // 8. 图片转像素画算法 (Pixel Art)
    fun convertToPixelArt(source: Bitmap, pixelSize: Int = 16): Bitmap {
        val width = source.width
        val height = source.height

        // 缩放到小尺寸再无平滑放大
        val smallW = (width / pixelSize).coerceAtLeast(1)
        val smallH = (height / pixelSize).coerceAtLeast(1)

        val smallBitmap = Bitmap.createScaledBitmap(source, smallW, smallH, false)
        val pixelArt = Bitmap.createScaledBitmap(smallBitmap, width, height, false)
        return pixelArt
    }
}
