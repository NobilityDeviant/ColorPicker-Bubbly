package com.jaredrummler.android.colorpicker

import android.content.Context
import android.graphics.Color
import android.widget.Toast
import androidx.annotation.ColorInt
import kotlin.math.roundToInt

internal object Tools {

    fun showToast(context: Context?, text: String) {
        Toast.makeText(
            context,
            text,
            Toast.LENGTH_LONG
        ).show()
    }

    @Throws(NumberFormatException::class)
    fun parseColorString(colorString: String): Int {
        var mColorString = colorString
        val a: Int
        var r: Int
        val g: Int
        var b = 0
        if (mColorString.startsWith("#")) {
            mColorString = mColorString.substring(1)
        }
        if (mColorString.isEmpty()) {
            r = 0
            a = 255
            g = 0
        } else if (mColorString.length <= 2) {
            a = 255
            r = 0
            b = mColorString.toInt(16)
            g = 0
        } else if (mColorString.length == 3) {
            a = 255
            r = mColorString.substring(0, 1).toInt(16)
            g = mColorString.substring(1, 2).toInt(16)
            b = mColorString.substring(2, 3).toInt(16)
        } else if (mColorString.length == 4) {
            a = 255
            r = mColorString.substring(0, 2).toInt(16)
            g = r
            r = 0
            b = mColorString.substring(2, 4).toInt(16)
        } else if (mColorString.length == 5) {
            a = 255
            r = mColorString.substring(0, 1).toInt(16)
            g = mColorString.substring(1, 3).toInt(16)
            b = mColorString.substring(3, 5).toInt(16)
        } else if (mColorString.length == 6) {
            a = 255
            r = mColorString.substring(0, 2).toInt(16)
            g = mColorString.substring(2, 4).toInt(16)
            b = mColorString.substring(4, 6).toInt(16)
        } else if (mColorString.length == 7) {
            a = mColorString.substring(0, 1).toInt(16)
            r = mColorString.substring(1, 3).toInt(16)
            g = mColorString.substring(3, 5).toInt(16)
            b = mColorString.substring(5, 7).toInt(16)
        } else if (mColorString.length == 8) {
            a = mColorString.substring(0, 2).toInt(16)
            r = mColorString.substring(2, 4).toInt(16)
            g = mColorString.substring(4, 6).toInt(16)
            b = mColorString.substring(6, 8).toInt(16)
        } else {
            b = -1
            g = -1
            r = -1
            a = -1
        }
        return Color.argb(a, r, g, b)
    }

    fun shadeColor(@ColorInt color: Int, percent: Double): Int {
        val hex = String.format("#%06X", 0xFFFFFF and color)
        val f = hex.substring(1).toLong(16)
        val t = (if (percent < 0) 0 else 255).toDouble()
        val p = if (percent < 0) percent * -1 else percent
        val r = f shr 16
        val g = f shr 8 and 0x00FFL
        val b = f and 0x0000FFL
        val alpha = Color.alpha(color)
        val red = (((t - r) * p).roundToInt() + r).toInt()
        val green = (((t - g) * p).roundToInt() + g).toInt()
        val blue = (((t - b) * p).roundToInt() + b).toInt()
        return Color.argb(alpha, red, green, blue)
    }

    fun getColorShades(@ColorInt color: Int): IntArray {
        return intArrayOf(
            shadeColor(color, 0.9),
            shadeColor(color, 0.7),
            shadeColor(color, 0.5),
            shadeColor(color, 0.333),
            shadeColor(color, 0.166),
            shadeColor(color, -0.125),
            shadeColor(color, -0.25),
            shadeColor(color, -0.375),
            shadeColor(color, -0.5),
            shadeColor(color, -0.675),
            shadeColor(color, -0.7),
            shadeColor(color, -0.775)
        )
    }
}