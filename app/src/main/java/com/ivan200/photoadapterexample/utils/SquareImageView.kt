package com.ivan200.photoadapterexample.utils

import android.content.Context
import android.util.AttributeSet
import android.view.View.MeasureSpec.EXACTLY
import android.view.View.MeasureSpec.getMode
import android.view.View.MeasureSpec.getSize
import android.view.View.MeasureSpec.makeMeasureSpec
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.min

/**
 * Created by Zakharovi on 09.11.2017.
 */
//Square ImageView, height is calculated from width, or width from height, depends on less size
class SquareImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {
    public override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val wExactly = getMode(widthMeasureSpec) == EXACTLY
        val hExactly = getMode(heightMeasureSpec) == EXACTLY
        val wSize = getSize(widthMeasureSpec)
        val hSize = getSize(heightMeasureSpec)
        val size = when {
            wExactly && hExactly && wSize > 0 && hSize > 0 -> min(wSize, hSize)
            wExactly && wSize > 0 -> wSize
            hExactly && hSize > 0 -> hSize
            else -> min(wSize, hSize)
        }
        val finalMeasureSpec = makeMeasureSpec(size, EXACTLY)
        super.onMeasure(finalMeasureSpec, finalMeasureSpec)
    }
}