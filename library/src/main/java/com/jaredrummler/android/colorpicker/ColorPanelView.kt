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

import android.content.Context
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import com.jaredrummler.android.colorpicker.library.R

/**
 * This class draws a panel which which will be filled with a color which can be set. It can be used to show the
 * currently selected color which you will get from the [ColorPickerView].
 */
class ColorPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private var alphaPattern: Drawable? = null
    private lateinit var borderPaint: Paint
    private lateinit var colorPaint: Paint
    private lateinit var alphaPaint: Paint
    private lateinit var originalPaint: Paint
    private lateinit var drawingRect: Rect
    private lateinit var colorRect: Rect
    private var centerRect = RectF()
    private var showOldColor = false

    /* The width in pixels of the border surrounding the color panel. */
    private var borderWidthPx = 0
    var borderColor = DEFAULT_BORDER_COLOR
        set(value) {
        field = value
        invalidate()
    }

    var color = Color.BLACK
        set(value) {
            field = value
            invalidate()
        }
    private var shape = 0

    init {
        init(context, attrs)
    }

    public override fun onSaveInstanceState(): Parcelable {
        val state = Bundle()
        state.putParcelable("instanceState", super.onSaveInstanceState())
        state.putInt("color", color)
        return state
    }

    public override fun onRestoreInstanceState(state: Parcelable) {
        var mState: Parcelable? = state
        if (mState is Bundle) {
            val bundle = mState
            color = bundle.getInt("color")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                mState = bundle.getParcelable("instanceState", Parcelable::class.java)
            } else {
                @Suppress("DEPRECATION")
                mState = bundle.getParcelable("instanceState")
            }
        }
        super.onRestoreInstanceState(mState)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        val a = getContext().obtainStyledAttributes(attrs, R.styleable.ColorPanelView)
        shape = a.getInt(R.styleable.ColorPanelView_cpv_colorShape, ColorShape.CIRCLE)
        showOldColor = a.getBoolean(R.styleable.ColorPanelView_cpv_showOldColor, false)
        check(!(showOldColor && shape != ColorShape.CIRCLE)) { "Color preview is only available in circle mode" }
        borderColor = a.getColor(R.styleable.ColorPanelView_cpv_borderColor, DEFAULT_BORDER_COLOR)
        a.recycle()
        if (borderColor == DEFAULT_BORDER_COLOR) {
            // If no specific border color has been set we take the default secondary text color as border/slider color.
            // Thus it will adopt to theme changes automatically.
            val value = TypedValue()
            val typedArray = context.obtainStyledAttributes(
                value.data,
                intArrayOf(android.R.attr.textColorSecondary)
            )
            borderColor = typedArray.getColor(0, borderColor)
            typedArray.recycle()
        }
        borderWidthPx = DrawingUtils.dpToPx(context, 1f)
        borderPaint = Paint()
        borderPaint.isAntiAlias = true
        colorPaint = Paint()
        colorPaint.isAntiAlias = true
        if (showOldColor) {
            originalPaint = Paint()
        }
        if (shape == ColorShape.CIRCLE) {
            val cpvAlpha = ResourcesCompat.getDrawable(
                resources,
                R.drawable.cpv_alpha,
                null
            )
            val bitmap = (cpvAlpha as BitmapDrawable).bitmap
            alphaPaint = Paint()
            alphaPaint.isAntiAlias = true
            val shader = BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            alphaPaint.shader = shader
        }
    }

    override fun onDraw(canvas: Canvas) {
        borderPaint.color = borderColor
        colorPaint.color = color
        if (shape == ColorShape.SQUARE) {
            if (borderWidthPx > 0) {
                canvas.drawRect(drawingRect, borderPaint)
            }
            alphaPattern?.draw(canvas)
            canvas.drawRect(colorRect, colorPaint)
        } else if (shape == ColorShape.CIRCLE) {
            val outerRadius = measuredWidth / 2
            if (borderWidthPx > 0) {
                canvas.drawCircle(
                    (measuredWidth / 2).toFloat(),
                    (measuredHeight / 2).toFloat(),
                    outerRadius.toFloat(),
                    borderPaint
                )
            }
            if (Color.alpha(color) < 255) {
                canvas.drawCircle(
                    (measuredWidth / 2).toFloat(),
                    (measuredHeight / 2).toFloat(),
                    (outerRadius - borderWidthPx).toFloat(),
                    alphaPaint
                )
            }
            if (showOldColor) {
                canvas.drawArc(
                    centerRect,
                    90f,
                    180f,
                    true,
                    originalPaint
                )
                canvas.drawArc(
                    centerRect,
                    270f,
                    180f,
                    true,
                    colorPaint
                )
            } else {
                canvas.drawCircle(
                    (measuredWidth / 2).toFloat(),
                    (measuredHeight / 2).toFloat(),
                    (outerRadius - borderWidthPx).toFloat(),
                    colorPaint
                )
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (shape == ColorShape.SQUARE) {
            val width = MeasureSpec.getSize(widthMeasureSpec)
            val height = MeasureSpec.getSize(heightMeasureSpec)
            setMeasuredDimension(width, height)
        } else if (shape == ColorShape.CIRCLE) {
            super.onMeasure(widthMeasureSpec, widthMeasureSpec)
            setMeasuredDimension(measuredWidth, measuredWidth)
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (shape == ColorShape.SQUARE || showOldColor) {
            drawingRect = Rect()
            drawingRect.left = paddingLeft
            drawingRect.right = w - paddingRight
            drawingRect.top = paddingTop
            drawingRect.bottom = h - paddingBottom
            if (showOldColor) {
                setUpCenterRect()
            } else {
                setUpColorRect()
            }
        }
    }

    private fun setUpCenterRect() {
        val left = drawingRect.left + borderWidthPx
        val top = drawingRect.top + borderWidthPx
        val bottom = drawingRect.bottom - borderWidthPx
        val right = drawingRect.right - borderWidthPx
        centerRect = RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
    }

    private fun setUpColorRect() {
        val left = drawingRect.left + borderWidthPx
        val top = drawingRect.top + borderWidthPx
        val bottom = drawingRect.bottom - borderWidthPx
        val right = drawingRect.right - borderWidthPx
        colorRect = Rect(left, top, right, bottom)
        alphaPattern = AlphaPatternDrawable(DrawingUtils.dpToPx(context, 4f))
        alphaPattern?.setBounds(
            Math.round(colorRect.left.toFloat()), Math.round(
                colorRect.top.toFloat()
            ), Math.round(colorRect.right.toFloat()),
            Math.round(colorRect.bottom.toFloat())
        )
    }


    /**
     * Set the original color. This is only used for previewing colors.
     *
     * @param color The original color
     */
    fun setOriginalColor(@ColorInt color: Int) {
        originalPaint.color = color
    }


    /**
     * Get the shape
     *
     * @return Either [ColorShape.SQUARE] or [ColorShape.CIRCLE].
     */
    @ColorShape
    fun getShape(): Int {
        return shape
    }

    /**
     * Set the shape.
     *
     * @param shape Either [ColorShape.SQUARE] or [ColorShape.CIRCLE].
     */
    fun setShape(@ColorShape shape: Int) {
        this.shape = shape
        invalidate()
    }

    /**
     * Show a toast message with the hex color code below the view.
     */
    fun showHint() {
        val screenPos = IntArray(2)
        val displayFrame = Rect()
        getLocationOnScreen(screenPos)
        getWindowVisibleDisplayFrame(displayFrame)
        val context = context
        val width = width
        val height = height
        val midy = screenPos[1] + height / 2
        var referenceX = screenPos[0] + width / 2
        if (ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_LTR) {
            val screenWidth = context.resources.displayMetrics.widthPixels
            referenceX = screenWidth - referenceX // mirror
        }
        val hint = StringBuilder("#")
        if (Color.alpha(color) != 255) {
            hint.append(Integer.toHexString(color).uppercase())
        } else {
            hint.append(String.format("%06X", 0xFFFFFF and color).uppercase())
        }
        val cheatSheet = Toast.makeText(context, hint.toString(), Toast.LENGTH_SHORT)
        if (midy < displayFrame.height()) {
            // Show along the top; follow action buttons
            cheatSheet.setGravity(
                Gravity.TOP or GravityCompat.END,
                referenceX,
                screenPos[1] + height - displayFrame.top
            )
        } else {
            // Show along the bottom center
            cheatSheet.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, height)
        }
        cheatSheet.show()
    }

    companion object {
        private const val DEFAULT_BORDER_COLOR = -0x919192
    }
}