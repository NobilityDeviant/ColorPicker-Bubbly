package com.jaredrummler.android.colorpicker

import android.graphics.Color
import androidx.lifecycle.ViewModel

class ColorPickerViewModel: ViewModel() {
    var colorPickerDialogListener: ColorPickerDialogListener? = null
    var color = Color.BLACK
    var dialogType = ColorPickerDialog.TYPE_PRESETS
    var dialogId = 0
    var allowCustom = true
    var allowPresets = true
    var showAlphaSlider = false
    var showColorShades = true
    var colorShape = ColorShape.CIRCLE
}