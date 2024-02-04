package com.jaredrummler.android.colorpicker

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration

//https://stackoverflow.com/questions/71039347/recyclerview-layout-grid-items-not-aligning-properly
class RecyclerDecoration(
    private val numberOfColumns: Int,
    private val spacing: Int,
    private val addSpacingToPerimeter: Boolean = false
) : ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view) // item position
        val column = position % numberOfColumns // item column
        if (addSpacingToPerimeter) {
            outRect.left = spacing - column * spacing / numberOfColumns
            outRect.right = (column + 1) * spacing / numberOfColumns
            if (position < numberOfColumns) { // top edge
                outRect.top = spacing
            }
            outRect.bottom = spacing // item bottom
        } else {
            outRect.left = column * spacing / numberOfColumns
            outRect.right = spacing - (column + 1) * spacing / numberOfColumns
            if (position >= numberOfColumns) {
                outRect.top = spacing // item top
            }
        }
    }
}