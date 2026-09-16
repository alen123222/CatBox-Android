package com.lingcat.a4cropper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build

object WallpaperHelper {

    fun getWallpaperBitmap(context: Context, which: Int = WallpaperManager.FLAG_SYSTEM): Bitmap? {
        val wallpaperManager = WallpaperManager.getInstance(context)

        // 1. Android 7.0+ 尝试直接读取系统壁纸文件流 (ParcelFileDescriptor)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                val pfd = wallpaperManager.getWallpaperFile(which)
                if (pfd != null) {
                    pfd.use {
                        val bmp = BitmapFactory.decodeFileDescriptor(it.fileDescriptor)
                        if (bmp != null) return bmp
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. 尝试 peekDrawable() - 优先获取已缓存在系统的 Drawable（不强刷，兼容部分权限）
        try {
            val peek = wallpaperManager.peekDrawable()
            if (peek != null) {
                val bmp = drawableToBitmap(peek)
                if (bmp != null) return bmp
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. 尝试 fastDrawable
        try {
            val fast = wallpaperManager.fastDrawable
            if (fast != null) {
                val bmp = drawableToBitmap(fast)
                if (bmp != null) return bmp
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 4. 尝试 drawable
        try {
            val d = wallpaperManager.drawable
            if (d != null) {
                val bmp = drawableToBitmap(d)
                if (bmp != null) return bmp
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 5. 尝试 builtInDrawable
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                val builtIn = wallpaperManager.getBuiltInDrawable(which)
                if (builtIn != null) {
                    val bmp = drawableToBitmap(builtIn)
                    if (bmp != null) return bmp
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return null
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }
        return try {
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1080
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 2400
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
