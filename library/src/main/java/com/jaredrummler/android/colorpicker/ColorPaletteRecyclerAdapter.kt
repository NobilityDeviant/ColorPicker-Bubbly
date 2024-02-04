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

import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import com.jaredrummler.android.colorpicker.library.R

class ColorPaletteRecyclerAdapter(
    val listener: OnColorSelectedListener,
    val colors: IntArray,
    var selectedPosition: Int,
    @param:ColorShape var colorShape: Int = ColorShape.CIRCLE
) : RecyclerView.Adapter<ColorPaletteRecyclerAdapter.ColorViewHolder>() {

    override fun getItemCount(): Int {
        return colors.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val layoutResId: Int = if (colorShape == ColorShape.SQUARE) {
            R.layout.cpv_color_item_square
        } else {
            R.layout.cpv_color_item_circle
        }
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(layoutResId, parent, false)
        val colorPanelView: ColorPanelView = view.findViewById(R.id.cpv_color_panel_view)
        val viewHolder = ColorViewHolder(view)
        colorPanelView.setOnClickListener {
            val oldSelected = selectedPosition
            if (oldSelected != viewHolder.colorPosition) {
                selectedPosition = viewHolder.colorPosition
                notifyItemChanged(oldSelected)
                notifyItemChanged(selectedPosition)
            }
            listener.onColorSelected(viewHolder.color)
        }
        colorPanelView.setOnLongClickListener {
            colorPanelView.showHint()
            true
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        holder.setup(position)
    }

    fun reload() {
        notifyItemRangeChanged(0, colors.size)
    }

    fun selectNone() {
        val oldSelected = selectedPosition
        selectedPosition = -1
        notifyItemChanged(oldSelected)
    }

    interface OnColorSelectedListener {
        fun onColorSelected(color: Int)
    }

    inner class ColorViewHolder(
        view: View
    ) : RecyclerView.ViewHolder(view) {
        private var colorPanelView: ColorPanelView
        private var imageView: ImageView
        private var originalBorderColor: Int
        var color = 0
        var colorPosition = 0

        init {
            colorPanelView = view.findViewById(R.id.cpv_color_panel_view)
            imageView = view.findViewById(R.id.cpv_color_image_view)
            originalBorderColor = colorPanelView.borderColor
            //view.tag = this
        }

        fun setup(colorPosition: Int) {
            this.colorPosition = colorPosition
            color = colors[colorPosition]
            val alpha = Color.alpha(color)
            colorPanelView.color = color
            imageView.setImageResource(
                if (selectedPosition == colorPosition)
                    R.drawable.cpv_preset_checked
                else 0
            )
            if (alpha != 255) {
                if (alpha <= ColorPickerDialog.ALPHA_THRESHOLD) {
                    colorPanelView.borderColor = color or -0x1000000
                    imageView.setColorFilter( /*color | 0xFF000000*/Color.BLACK,
                        PorterDuff.Mode.SRC_IN
                    )
                } else {
                    colorPanelView.borderColor = originalBorderColor
                    imageView.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
                }
            } else {
                setColorFilter(colorPosition)
            }
            //setOnClickListener(position)
        }

        private fun setOnClickListener(position: Int) {
            colorPanelView.setOnClickListener {
                val oldSelected = selectedPosition
                if (oldSelected != position) {
                    selectedPosition = position
                    notifyItemChanged(oldSelected)
                    notifyItemChanged(selectedPosition)
                }
                listener.onColorSelected(colors[position])
            }
            colorPanelView.setOnLongClickListener {
                colorPanelView.showHint()
                true
            }
        }

        private fun setColorFilter(position: Int) {
            if (position == selectedPosition && ColorUtils.calculateLuminance(
                    colors[position]
                ) >= 0.65
            ) {
                imageView.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)
            } else {
                imageView.colorFilter = null
            }
        }
    }
}