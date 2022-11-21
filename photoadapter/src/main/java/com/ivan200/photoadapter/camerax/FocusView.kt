package com.ivan200.photoadapter.camerax

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import androidx.annotation.AttrRes
import androidx.annotation.RequiresApi
import androidx.annotation.StyleRes

/**
 * @author ivan200
 * @since 31.02.2021
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class FocusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes), Animation.AnimationListener {

    private var animated = false
    private var paint = Paint(Paint.ANTI_ALIAS_FLAG).also {
        it.color = Color.WHITE
        it.style = Paint.Style.STROKE
        it.strokeWidth = DEFAULT_STROKE_WIDTH
    }
    private var animation: AnimationSet? = null
    private var fromX = 0f
    private var fromY = 0f
    private var toX = 1.0f
    private var toY = 1.0f

    fun anim(xValue: Float, yValue: Float) {
        this.visibility = VISIBLE
        animated = true
        clearAnimation()
        animation?.setAnimationListener(null)
        animation = null
        invalidate()
        val scaleAnimation = ScaleAnimation(fromX, toX, fromY, toY).apply {
            duration = DEFAULT_ANIMATION_DURATION
        }
        val translateAnimation = TranslateAnimation(
            TranslateAnimation.ABSOLUTE, xValue - width * fromX / 2,
            TranslateAnimation.ABSOLUTE, xValue - width * toX / 2,
            TranslateAnimation.ABSOLUTE, yValue - height * fromY / 2,
            TranslateAnimation.ABSOLUTE, yValue - height * toY / 2
        ).apply {
            duration = DEFAULT_ANIMATION_DURATION
        }

        animation = AnimationSet(false).apply {
            addAnimation(scaleAnimation)
            addAnimation(translateAnimation)
            repeatCount = 0
            fillAfter = true
            isFillEnabled = true
            fillBefore = true
            setAnimationListener(this@FocusView)
        }
        startAnimation(animation)
    }

    fun hide() {
        clearAnimation()
        this.visibility = INVISIBLE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (animated) {
            canvas.drawCircle(
                (width / 2).toFloat(),
                (height / 2).toFloat(),
                width.toFloat() / 2.0f - paint.strokeWidth / 2.0f - 1,
                paint
            )
        }
    }

    override fun onAnimationEnd(animation: Animation?) {
        animated = false
    }

    override fun onAnimationStart(animation: Animation?) {
        //not used
    }

    override fun onAnimationRepeat(animation: Animation?) {
        //not used
    }

    private companion object {
        const val DEFAULT_ANIMATION_DURATION = 300L
        const val DEFAULT_STROKE_WIDTH = 3.0f
    }
}
