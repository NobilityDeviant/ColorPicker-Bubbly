package com.jaredrummler.android.colorpicker

import android.content.res.Resources

val Number.dp: Int
    get() = if (this.toInt() > 0) (this.toInt() / Resources.getSystem()
        .displayMetrics.density).toInt() else this.toInt()

val Number.px: Int
    get() = if (this.toInt() > 0) (this.toInt() * Resources.getSystem()
        .displayMetrics.density).toInt() else this.toInt()