/*
 * Copyright (C) 2017 Jared Rummler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jaredrummler.android.colorpicker

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.TextWatcher
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup.GONE
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewGroup.VISIBLE
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.annotation.ColorInt
import androidx.annotation.IntDef
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.jaredrummler.android.colorpicker.ColorPickerView.OnColorChangedListener
import com.jaredrummler.android.colorpicker.Tools.getColorShades
import com.jaredrummler.android.colorpicker.Tools.parseColorString
import com.jaredrummler.android.colorpicker.library.R
import com.jaredrummler.android.colorpicker.library.databinding.CpvDialogColorPickerBinding
import com.jaredrummler.android.colorpicker.library.databinding.CpvDialogPresetsBinding
import java.util.Locale

class ColorPickerDialog private constructor(
    private var mPresets: IntArray?,
    private val dialogTitle: Int
) : DialogFragment(), OnColorChangedListener, TextWatcher {

    constructor() : this(
        mPresets = MATERIAL_COLORS,
        dialogTitle = R.string.cpv_default_title
    )

    lateinit var vm: ColorPickerViewModel

    private val gridColumns = 5
    private lateinit var rootView: FrameLayout
    private var mPresetsBinding: CpvDialogPresetsBinding? = null
    private val presetsBinding get() = mPresetsBinding!!

    private var mColorPickerBinding: CpvDialogColorPickerBinding? = null
    private val colorPickerBinding get() = mColorPickerBinding!!

    override fun onDestroyView() {
        super.onDestroyView()
        mPresetsBinding = null
        mColorPickerBinding = null
    }

    private val presets get() = mPresets!!
    private lateinit var adapter: ColorPaletteRecyclerAdapter

    // -- CUSTOM ---------------------------
    private var fromEditText = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm = ViewModelProvider(this)
            .get(ColorPickerViewModel::class.java)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        rootView = FrameLayout(requireActivity())
        mPresetsBinding = CpvDialogPresetsBinding.inflate(
            layoutInflater,
            rootView,
            false
        )
        mColorPickerBinding = CpvDialogColorPickerBinding.inflate(
            layoutInflater,
            rootView,
            false
        )
        if (vm.dialogType == TYPE_CUSTOM) {
            rootView.addView(createPickerView())
        } else if (vm.dialogType == TYPE_PRESETS) {
            rootView.addView(createPresetsView())
        }
        val builder = AlertDialog.Builder(requireActivity()).setView(rootView)
            .setPositiveButton(R.string.cpv_select) { _: DialogInterface?, _: Int ->
                onColorSelected(
                    vm.color
                )
            }
        if (dialogTitle != 0) {
            builder.setTitle(dialogTitle)
        }
        var createNeutral = true
        if (vm.dialogType == TYPE_PRESETS) {
            if (!vm.allowCustom) {
                createNeutral = false
            }
        } else if (vm.dialogType == TYPE_CUSTOM) {
            if (!vm.allowPresets) {
                createNeutral = false
            }
        }
        if (createNeutral) {
            val neutralButtonStringRes: Int = when (vm.dialogType) {
                TYPE_CUSTOM -> {
                    R.string.cpv_presets
                }
                TYPE_PRESETS -> {
                    R.string.cpv_custom
                }
                else -> {
                    0
                }
            }
            if (neutralButtonStringRes != 0) {
                builder.setNeutralButton(neutralButtonStringRes, null)
            }
        }
        return builder.create()
    }

    private fun reloadDialog() {
        rootView.removeAllViews()
        if (vm.dialogType == TYPE_CUSTOM) {
            rootView.addView(createPickerView())
        } else if (vm.dialogType == TYPE_PRESETS) {
            rootView.addView(createPresetsView(true))
        }
        val dialog = dialog as AlertDialog
        val neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
        if (vm.dialogType == TYPE_PRESETS) {
            if (!vm.allowCustom) {
                neutralButton.visibility = GONE
            }
        } else if (vm.dialogType == TYPE_CUSTOM) {
            if (!vm.allowPresets) {
                neutralButton.visibility = GONE
            }
        }
        if (neutralButton.visibility == VISIBLE) {
            neutralButton.setText(
                if (vm.dialogType == TYPE_PRESETS)
                    R.string.cpv_custom
                else
                    R.string.cpv_presets
            )
        }
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as AlertDialog

        // http://stackoverflow.com/a/16972670/1048340
        dialog.window?.clearFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
        )
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        // Do not dismiss the dialog when clicking the neutral button.
        val neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
        neutralButton?.setOnClickListener { v: View ->
            if (vm.dialogType == TYPE_CUSTOM) {
                vm.dialogType = TYPE_PRESETS
                reloadDialog()
            } else if (vm.dialogType == TYPE_PRESETS) {
                vm.dialogType = TYPE_CUSTOM
                reloadDialog()
            }
            /*rootView.removeAllViews()
            when (vm.dialogType) {
                TYPE_CUSTOM -> {
                    vm.dialogType = TYPE_PRESETS
                    if (v is Button) {
                        v.setText(
                            R.string.cpv_custom
                        )
                    }
                    rootView.addView(createPresetsView())
                }

                TYPE_PRESETS -> {
                    vm.dialogType = TYPE_CUSTOM
                    if (v is Button) {
                        v.setText(
                            R.string.cpv_presets
                        )
                    }
                    rootView.addView(createPickerView())
                }
            }*/
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDialogDismissed()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createPickerView(): View {
        try {
            val value = TypedValue()
            val typedArray = requireActivity().obtainStyledAttributes(
                value.data,
                intArrayOf(android.R.attr.textColorPrimary)
            )
            val arrowColor = typedArray.getColor(0, Color.BLACK)
            typedArray.recycle()
            colorPickerBinding.cpvArrowRight.setColorFilter(arrowColor)
        } catch (ignored: Exception) {
        }
        colorPickerBinding.cpvColorPickerView.setAlphaSliderVisible(vm.showAlphaSlider)
        colorPickerBinding.cpvColorPanelOld.color = vm.color
        colorPickerBinding.cpvColorPickerView.setColor(vm.color, true)
        colorPickerBinding.cpvColorPanelNew.color = vm.color
        setHex(vm.color)
        if (!vm.showAlphaSlider) {
            colorPickerBinding.cpvHex.filters = arrayOf<InputFilter>(LengthFilter(6))
        }
        colorPickerBinding.cpvColorPanelNew.setOnClickListener {
            if (colorPickerBinding.cpvColorPanelNew.color == vm.color) {
                onColorSelected(vm.color)
                dismiss()
            }
        }
        colorPickerBinding.root.setOnTouchListener(onPickerTouchListener)
        colorPickerBinding.cpvColorPickerView.setOnColorChangedListener(this)
        colorPickerBinding.cpvHex.addTextChangedListener(this)
        colorPickerBinding.cpvHex.setOnFocusChangeListener { _: View?, hasFocus: Boolean ->
            if (hasFocus) {
                val imm = requireActivity().getSystemService(
                    Context.INPUT_METHOD_SERVICE
                ) as InputMethodManager
                imm.showSoftInput(colorPickerBinding.cpvHex, InputMethodManager.SHOW_IMPLICIT)
            }
        }
        return colorPickerBinding.root
    }

    override fun onColorChanged(newColor: Int) {
        vm.color = newColor
        colorPickerBinding.cpvColorPanelNew.color = newColor
        if (!fromEditText) {
            setHex(newColor)
            if (colorPickerBinding.cpvHex.hasFocus()) {
                val imm = requireActivity().getSystemService(
                    Context.INPUT_METHOD_SERVICE
                ) as InputMethodManager
                imm.hideSoftInputFromWindow(colorPickerBinding.cpvHex.windowToken, 0)
                colorPickerBinding.cpvHex.clearFocus()
            }
        }
        fromEditText = false
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(s: Editable) {
        if (colorPickerBinding.cpvHex.isFocused) {
            val color = parseColorString(s.toString())
            if (color != colorPickerBinding.cpvColorPickerView.color) {
                fromEditText = true
                colorPickerBinding.cpvColorPickerView.setColor(color, true)
            }
        }
    }

    private fun setHex(color: Int) {
        if (vm.showAlphaSlider) {
            colorPickerBinding.cpvHex.setText(String.format("%08X", color))
        } else {
            colorPickerBinding.cpvHex.setText(String.format("%06X", 0xFFFFFF and color))
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun createPresetsView(reload: Boolean = false): View {
        loadPresets()
        if (vm.showColorShades) {
            createColorShades(vm.color)
        } else {
            presetsBinding.shadesLayout.visibility = View.GONE
            presetsBinding.shadesDivider.visibility = View.GONE
        }
        adapter = ColorPaletteRecyclerAdapter(object :
            ColorPaletteRecyclerAdapter.OnColorSelectedListener {
            override fun onColorSelected(color: Int) {
                if (!vm.showColorShades) {
                    vm.color = color
                    this@ColorPickerDialog.onColorSelected(vm.color)
                    dismiss()
                    return
                }
                if (vm.color == color) {
                    // Double tab selects the color
                    this@ColorPickerDialog.onColorSelected(vm.color)
                    dismiss()
                    return
                }
                vm.color = color
                createColorShades(vm.color)
            }
        }, presets, selectedItemPosition, vm.colorShape)
        presetsBinding.recyclerView.setItemViewCacheSize(presets.size)
        adapter.setHasStableIds(true)
        presetsBinding.recyclerView.setHasFixedSize(true)
        presetsBinding.recyclerView.isNestedScrollingEnabled = false
        presetsBinding.recyclerView.adapter = adapter
        val layoutManager = GridLayoutManager(
            context,
            gridColumns,
        )
        presetsBinding.recyclerView.layoutManager = layoutManager
        if (!reload) {
            presetsBinding.recyclerView.addItemDecoration(
                RecyclerDecoration(gridColumns, 30.dp)
            )
        }
        adapter.notifyDataSetChanged()
        if (vm.showAlphaSlider) {
            setupTransparency()
        } else {
            presetsBinding.transparencyLayout.visibility = View.GONE
            presetsBinding.transparencyTitle.visibility = View.GONE
        }
        return presetsBinding.root
    }

    private fun loadPresets() {
        val alpha = Color.alpha(vm.color)
        if (mPresets == null) {
            mPresets = MATERIAL_COLORS
        }
        val isMaterialColors = presets.contentEquals(MATERIAL_COLORS)
        mPresets =
            presets.copyOf(presets.size) // don't update the original array when modifying alpha
        if (alpha != 255) {
            // add alpha to the presets
            for (i in presets.indices) {
                val color = presets[i]
                val red = Color.red(color)
                val green = Color.green(color)
                val blue = Color.blue(color)
                presets[i] = Color.argb(alpha, red, green, blue)
            }
        }
        mPresets = unshiftIfNotExists(presets, vm.color)
        //val initialColor = color
        //if (initialColor != color) {
        // The user clicked a color and a configuration change occurred. Make sure the initial color is in the presets
        //  mPresets = unshiftIfNotExists(presets, initialColor)
        //}
        if (isMaterialColors && presets.size == 19) {
            // Add black to have a total of 20 colors if the current color is in the material color palette
            mPresets = pushIfNotExists(presets, Color.argb(alpha, 0, 0, 0))
        }
    }

    fun createColorShades(@ColorInt color: Int) {
        val colorShades = getColorShades(color)
        if (presetsBinding.shadesLayout.childCount != 0) {
            for (i in 0 until presetsBinding.shadesLayout.childCount) {
                val layout = presetsBinding.shadesLayout.getChildAt(i) as FrameLayout
                val cpv = layout.findViewById<View>(R.id.cpv_color_panel_view) as ColorPanelView
                val iv = layout.findViewById<View>(R.id.cpv_color_image_view) as ImageView
                cpv.color = colorShades[i]
                cpv.tag = false
                iv.setImageDrawable(null)
            }
            return
        }
        val horizontalPadding = resources.getDimensionPixelSize(R.dimen.cpv_item_horizontal_padding)
        for (colorShade in colorShades) {
            val layoutResId: Int = if (vm.colorShape == ColorShape.SQUARE) {
                R.layout.cpv_color_item_square
            } else {
                R.layout.cpv_color_item_circle
            }
            val view = View.inflate(activity, layoutResId, null)
            val colorPanelView =
                view.findViewById<View>(R.id.cpv_color_panel_view) as ColorPanelView
            val params = colorPanelView.layoutParams as MarginLayoutParams
            params.rightMargin = horizontalPadding
            params.leftMargin = params.rightMargin
            colorPanelView.layoutParams = params
            colorPanelView.color = colorShade
            presetsBinding.shadesLayout.addView(view)
            colorPanelView.post { // todo The color is black when rotating the dialog. This is a dirty fix. WTF!?
                colorPanelView.color = colorShade
            }
            colorPanelView.setOnClickListener(View.OnClickListener { v ->
                if (v.tag is Boolean && v.tag as Boolean) {
                    onColorSelected(vm.color)
                    dismiss()
                    return@OnClickListener  // already selected
                }
                vm.color = colorPanelView.color
                adapter.selectNone()
                for (i in 0 until presetsBinding.shadesLayout.childCount) {
                    val layout = presetsBinding.shadesLayout.getChildAt(i) as FrameLayout
                    val cpv = layout.findViewById<View>(R.id.cpv_color_panel_view) as ColorPanelView
                    val iv = layout.findViewById<View>(R.id.cpv_color_image_view) as ImageView
                    iv.setImageResource(if (cpv == v) R.drawable.cpv_preset_checked else 0)
                    if (cpv == v && ColorUtils.calculateLuminance(cpv.color) >= 0.50
                        || Color.alpha(cpv.color) <= ALPHA_THRESHOLD
                    ) {
                        iv.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)
                    } else {
                        iv.colorFilter = null
                    }
                    cpv.tag = cpv == v
                }
            })
            colorPanelView.setOnLongClickListener {
                colorPanelView.showHint()
                true
            }
        }
    }

    private fun onColorSelected(color: Int) {
        if (vm.colorPickerDialogListener != null) {
            vm.colorPickerDialogListener!!.onColorSelected(vm.dialogId, color)
            return
        }
        if (activity is ColorPickerDialogListener) {
            (activity as ColorPickerDialogListener).onColorSelected(vm.dialogId, color)
        } else {
            throw IllegalStateException("The activity must implement ColorPickerDialogListener")
        }
    }

    private fun onDialogDismissed() {
        if (vm.colorPickerDialogListener != null) {
            vm.colorPickerDialogListener!!.onDialogDismissed(vm.dialogId)
            return
        }
        if (activity is ColorPickerDialogListener) {
            (activity as ColorPickerDialogListener).onDialogDismissed(vm.dialogId)
        }
    }

    private fun setupTransparency() {
        val progress = 255 - Color.alpha(vm.color)
        presetsBinding.transparencySeekbar.max = 255
        presetsBinding.transparencySeekbar.progress = progress
        val percentage = (progress.toDouble() * 100 / 255).toInt()
        presetsBinding.transparencyText.text = String.format(Locale.ENGLISH, "%d%%", percentage)
        presetsBinding.transparencySeekbar.setOnSeekBarChangeListener(object :
            OnSeekBarChangeListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val newPercentage = (progress.toDouble() * 100 / 255).toInt()
                presetsBinding.transparencyText.text =
                    String.format(Locale.ENGLISH, "%d%%", newPercentage)
                val alpha = 255 - progress
                // update items in GridView:
                for (i in adapter.colors.indices) {
                    val color = adapter.colors[i]
                    val red = Color.red(color)
                    val green = Color.green(color)
                    val blue = Color.blue(color)
                    adapter.colors[i] = Color.argb(alpha, red, green, blue)
                }
                // update shades:
                for (i in 0 until presetsBinding.shadesLayout.childCount) {
                    val layout = presetsBinding.shadesLayout.getChildAt(i) as FrameLayout
                    val cpv = layout.findViewById<View>(R.id.cpv_color_panel_view) as ColorPanelView
                    val iv = layout.findViewById<View>(R.id.cpv_color_image_view) as ImageView
                    if (layout.tag == null) {
                        // save the original border color
                        layout.tag = cpv.borderColor
                    }
                    var color = cpv.color
                    color = Color.argb(
                        alpha,
                        Color.red(color),
                        Color.green(color),
                        Color.blue(color)
                    )
                    if (alpha <= ALPHA_THRESHOLD) {
                        cpv.borderColor = color or -0x1000000
                    } else {
                        if (layout.tag is Int) {
                            cpv.borderColor = layout.tag as Int
                        }
                    }
                    if (cpv.tag != null && cpv.tag as Boolean) {
                        // The alpha changed on the selected shaded color. Update the checkmark color filter.
                        if (alpha <= ALPHA_THRESHOLD) {
                            iv.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)
                        } else {
                            if (ColorUtils.calculateLuminance(color) >= 0.65) {
                                iv.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)
                            } else {
                                iv.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
                            }
                        }
                    }
                    cpv.color = color
                }
                // update color:
                val red = Color.red(vm.color)
                val green = Color.green(vm.color)
                val blue = Color.blue(vm.color)
                vm.color = Color.argb(alpha, red, green, blue)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                adapter.reload()
            }
        })
        presetsBinding.transparencyLayout.visibility = VISIBLE
        presetsBinding.transparencyTitle.visibility = VISIBLE
    }

    private fun unshiftIfNotExists(array: IntArray?, value: Int): IntArray {
        var present = false
        for (i in array!!) {
            if (i == value) {
                present = true
                break
            }
        }
        if (!present) {
            val newArray = IntArray(array.size + 1)
            newArray[0] = value
            System.arraycopy(array, 0, newArray, 1, newArray.size - 1)
            return newArray
        }
        return array
    }

    private fun pushIfNotExists(array: IntArray?, value: Int): IntArray {
        var present = false
        for (i in array!!) {
            if (i == value) {
                present = true
                break
            }
        }
        if (!present) {
            val newArray = IntArray(array.size + 1)
            newArray[newArray.size - 1] = value
            System.arraycopy(array, 0, newArray, 0, newArray.size - 1)
            return newArray
        }
        return array
    }

    private val selectedItemPosition: Int
        get() {
            for (i in presets.indices) {
                if (presets[i] == vm.color) {
                    return i
                }
            }
            return -1
        }

    @SuppressLint("ClickableViewAccessibility")
    private val onPickerTouchListener = View.OnTouchListener { v, _ ->
        if (v !== colorPickerBinding.cpvHex && colorPickerBinding.cpvHex.hasFocus()) {
            colorPickerBinding.cpvHex.clearFocus()
            val imm = requireActivity().getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager
            imm.hideSoftInputFromWindow(colorPickerBinding.cpvHex.windowToken, 0)
            colorPickerBinding.cpvHex.clearFocus()
            return@OnTouchListener true
        }
        false
    }

    @IntDef(TYPE_CUSTOM, TYPE_PRESETS)
    annotation class DialogType
    class Builder internal constructor() {

        private var colorPickerDialogListener: ColorPickerDialogListener? = null

        @StringRes
        private var dialogTitle = R.string.cpv_default_title

        @DialogType
        private var dialogType = TYPE_PRESETS
        private var presets = MATERIAL_COLORS

        private var color = Color.BLACK
        private var dialogId = 0
        private var showAlphaSlider = false
        private var allowPresets = true
        private var allowCustom = true
        private var showColorShades = true

        @ColorShape
        var colorShape = ColorShape.CIRCLE

        /**
         * Set the dialog title string resource id
         *
         * @param dialogTitle The string resource used for the dialog title
         * @return This builder object for chaining method calls
         */
        fun setDialogTitle(@StringRes dialogTitle: Int): Builder {
            this.dialogTitle = dialogTitle
            return this
        }

        /**
         * Set which dialog view to show.
         *
         * @param dialogType Either [ColorPickerDialog.TYPE_CUSTOM] or [ColorPickerDialog.TYPE_PRESETS].
         * @return This builder object for chaining method calls
         */
        fun setDialogType(@DialogType dialogType: Int): Builder {
            this.dialogType = dialogType
            return this
        }

        /**
         * Set the colors used for the presets
         *
         * @param presets An array of color ints.
         * @return This builder object for chaining method calls
         */
        fun setPresets(presets: IntArray): Builder {
            this.presets = presets
            return this
        }

        /**
         * Set the original color
         *
         * @param color The default color for the color picker
         * @return This builder object for chaining method calls
         */
        fun setColor(color: Int): Builder {
            this.color = color
            return this
        }

        /**
         * Set the dialog id used for callbacks
         *
         * @param dialogId The id that is sent back to the [ColorPickerDialogListener].
         * @return This builder object for chaining method calls
         */
        fun setDialogId(dialogId: Int): Builder {
            this.dialogId = dialogId
            return this
        }

        /**
         * Show the alpha slider
         *
         * @param showAlphaSlider `true` to show the alpha slider. Currently only supported with the [                        ].
         * @return This builder object for chaining method calls
         */
        fun setShowAlphaSlider(showAlphaSlider: Boolean): Builder {
            this.showAlphaSlider = showAlphaSlider
            return this
        }

        /**
         * Show/Hide a neutral button to select preset colors.
         *
         * @param allowPresets `false` to disable showing the presets button.
         * @return This builder object for chaining method calls
         */
        fun setAllowPresets(allowPresets: Boolean): Builder {
            this.allowPresets = allowPresets
            return this
        }

        /**
         * Show/Hide the neutral button to select a custom color.
         *
         * @param allowCustom `false` to disable showing the custom button.
         * @return This builder object for chaining method calls
         */
        fun setAllowCustom(allowCustom: Boolean): Builder {
            this.allowCustom = allowCustom
            return this
        }

        /**
         * Show/Hide the color shades in the presets picker
         *
         * @param showColorShades `false` to hide the color shades.
         * @return This builder object for chaining method calls
         */
        fun setShowColorShades(showColorShades: Boolean): Builder {
            this.showColorShades = showColorShades
            return this
        }

        /**
         * Set the shape of the color panel view.
         *
         * @param colorShape Either [ColorShape.CIRCLE] or [ColorShape.SQUARE].
         * @return This builder object for chaining method calls
         */
        fun setColorShape(colorShape: Int): Builder {
            this.colorShape = colorShape
            return this
        }

        fun setOnColorSelectedListener(
            colorPickerDialogListener: ColorPickerDialogListener
        ): Builder {
            this.colorPickerDialogListener = colorPickerDialogListener
            return this
        }

        private fun create(): ColorPickerDialog {
            return ColorPickerDialog(
                mPresets = presets,
                dialogTitle = dialogTitle
            )
        }

        /**
         * Create and show the [ColorPickerDialog] created with this builder.
         *
         * @param activity The current activity.
         */
        fun show(activity: FragmentActivity) {
            val dialog = create()
            dialog.showNow(activity.supportFragmentManager, "color-picker-dialog")
            dialog.vm.dialogType = dialogType
            dialog.vm.dialogId = dialogId
            dialog.vm.colorPickerDialogListener = colorPickerDialogListener
            dialog.vm.color = color
            dialog.vm.allowCustom = allowCustom
            dialog.vm.allowPresets = allowPresets
            dialog.vm.showAlphaSlider = showAlphaSlider
            dialog.vm.showColorShades = showColorShades
            dialog.vm.colorShape = colorShape
            dialog.reloadDialog()
        }
    }

    companion object {
        const val TYPE_CUSTOM = 0
        const val TYPE_PRESETS = 1

        /**
         * Material design colors used as the default color presets
         */
        val MATERIAL_COLORS = intArrayOf(
            -0xbbcca,  // RED 500
            -0x16e19d,  // PINK 500
            -0xd36d,  // LIGHT PINK 500
            -0x63d850,  // PURPLE 500
            -0x98c549,  // DEEP PURPLE 500
            -0xc0ae4b,  // INDIGO 500
            -0xde690d,  // BLUE 500
            -0xfc560c,  // LIGHT BLUE 500
            -0xff432c,  // CYAN 500
            -0xff6978,  // TEAL 500
            -0xb350b0,  // GREEN 500
            -0x743cb6,  // LIGHT GREEN 500
            -0x3223c7,  // LIME 500
            -0x14c5,  // YELLOW 500
            -0x3ef9,  // AMBER 500
            -0x6800,  // ORANGE 500
            -0x86aab8,  // BROWN 500
            -0x9f8275,  // BLUE GREY 500
            -0x616162
        )
        const val ALPHA_THRESHOLD = 165

        fun newBuilder(): Builder {
            return Builder()
        }
    }
}