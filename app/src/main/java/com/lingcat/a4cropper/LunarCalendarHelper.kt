package com.lingcat.a4cropper

import android.content.Context
import android.icu.util.Calendar
import android.icu.util.ChineseCalendar
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LunarCalendarHelper {

    private val LUNAR_MONTHS = arrayOf(
        "正月", "二月", "三月", "四月", "五月", "六月",
        "七月", "八月", "九月", "十月", "冬月", "腊月"
    )

    private val LUNAR_DAYS = arrayOf(
        "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
    )

    data class DateInfo(
        val solarYear: Int,
        val solarMonth: Int,
        val solarDay: Int,
        val weekDay: String,
        val lunarDateText: String,
        val festival: String?
    )

    fun getTodayInfo(): DateInfo {
        val now = Date()
        val cal = java.util.Calendar.getInstance()
        cal.time = now

        val sYear = cal.get(java.util.Calendar.YEAR)
        val sMonth = cal.get(java.util.Calendar.MONTH) + 1
        val sDay = cal.get(java.util.Calendar.DAY_OF_MONTH)
        val weekDay = SimpleDateFormat("EEEE", Locale.CHINA).format(now)

        var lunarText = "农历"
        var fest: String? = null

        // 公历节日检测
        fest = checkSolarFestival(sMonth, sDay)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                val chineseCal = ChineseCalendar()
                chineseCal.time = now

                val isLeap = chineseCal.get(ChineseCalendar.IS_LEAP_MONTH) == 1
                val lMonthIndex = chineseCal.get(ChineseCalendar.MONTH)
                val lDay = chineseCal.get(ChineseCalendar.DAY_OF_MONTH)

                val monthStr = if (lMonthIndex in LUNAR_MONTHS.indices) LUNAR_MONTHS[lMonthIndex] else "${lMonthIndex + 1}月"
                val dayStr = if (lDay - 1 in LUNAR_DAYS.indices) LUNAR_DAYS[lDay - 1] else "${lDay}日"
                lunarText = (if (isLeap) "闰" else "") + monthStr + dayStr

                // 农历节日判断
                val lunarFest = checkLunarFestival(lMonthIndex + 1, lDay, isLeap)
                if (lunarFest != null) {
                    fest = lunarFest
                }
            } catch (e: Exception) {
                lunarText = "农历吉日"
            }
        }

        return DateInfo(
            solarYear = sYear,
            solarMonth = sMonth,
            solarDay = sDay,
            weekDay = weekDay,
            lunarDateText = lunarText,
            festival = fest
        )
    }

    private fun checkSolarFestival(m: Int, d: Int): String? {
        return when (m) {
            1 -> if (d == 1) "元旦" else null
            2 -> if (d == 14) "情人节" else null
            3 -> if (d == 8) "妇女节" else if (d == 12) "植树节" else null
            4 -> if (d == 1) "愚人节" else null
            5 -> if (d == 1) "劳动节" else if (d == 4) "青年节" else null
            6 -> if (d == 1) "儿童节" else null
            7 -> if (d == 1) "建党节" else null
            8 -> if (d == 1) "建军节" else null
            9 -> if (d == 10) "教师节" else null
            10 -> if (d == 1) "国庆节" else null
            11 -> if (d == 11) "光棍节" else null
            12 -> if (d == 24) "平安夜" else if (d == 25) "圣诞节" else null
            else -> null
        }
    }

    private fun checkLunarFestival(m: Int, d: Int, isLeap: Boolean): String? {
        if (isLeap) return null
        return when (m) {
            1 -> when (d) {
                1 -> "春节 🧧"
                15 -> "元宵节 🏮"
                else -> null
            }
            2 -> if (d == 2) "龙抬头 🐲" else null
            5 -> if (d == 5) "端午节 🛶" else null
            7 -> if (d == 7) "七夕节 🎋" else null
            8 -> if (d == 15) "中秋节 🥮" else null
            9 -> if (d == 9) "重阳节 🌼" else null
            12 -> when (d) {
                8 -> "腊八节 🥣"
                23, 24 -> "小年 🥟"
                30 -> "除夕 🎇"
                else -> null
            }
            else -> null
        }
    }
}
