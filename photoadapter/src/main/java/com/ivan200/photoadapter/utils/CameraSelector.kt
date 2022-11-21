package com.ivan200.photoadapter.utils

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.Keep
import com.ivan200.photoadapter.R
import kotlin.math.ceil

/**
 * Вью выбора камеры
 * аналог обычного чекбокса, но при выделении анимированно рисуется белый кружок
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class CameraSelector @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val rectF = RectF()
    private val size = ceil(context.resources.getDimension(R.dimen.size_select_camera)).toInt()
    private val drawBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    private val drawCanvas = Canvas(drawBitmap)
    private var progress = 0f
    private var checkAnimator: ObjectAnimator? = null
    private var attachedToWindow = false
    var isChecked = false
        private set

    @ColorInt
    private var camerasBackground = context.getColorCompat(R.color.color_circle_icon_normal)

    @ColorInt
    private var selectedCameraBackground = context.getColorCompat(R.color.color_background_grey_50)

    fun setColors(@ColorInt checked: Int, @ColorInt check: Int) {
        camerasBackground = checked
        selectedCameraBackground = check
    }

    @Keep
    fun setProgress(value: Float) {
        if (progress == value) {
            return
        }
        progress = value
        invalidate()
    }

    @Keep
    fun getProgress(): Float {
        return progress
    }

    private fun cancelCheckAnimator() {
        if (checkAnimator != null) {
            checkAnimator!!.cancel()
        }
    }

    private fun animateToCheckedState(newCheckedState: Boolean) {
        checkAnimator = ObjectAnimator.ofFloat(this, "progress", if (newCheckedState) 1f else 0f)
        checkAnimator!!.duration = 300
        checkAnimator!!.start()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attachedToWindow = true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        attachedToWindow = false
    }

    fun setChecked(checked: Boolean, animated: Boolean) {
        if (checked == isChecked) {
            return
        }
        isChecked = checked
        if (attachedToWindow && animated) {
            animateToCheckedState(checked)
        } else {
            cancelCheckAnimator()
            setProgress(if (checked) 1.0f else 0.0f)
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (visibility != VISIBLE) {
            return
        }
        rectF.set(0f, 0f, size.toFloat(), size.toFloat())
        drawBitmap.eraseColor(0)

        val half = size.toFloat() / 2.toFloat()

        drawCanvas.drawCircle(half, half, half * progress, checkboxSquare_checkPaint)

        checkboxSquare_checkPaint.color = selectedCameraBackground

        canvas.drawBitmap(drawBitmap, 0f, 0f, null)
    }

    companion object {
        val checkboxSquare_checkPaint: Paint by lazy {
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
            }
        }
    }
}
