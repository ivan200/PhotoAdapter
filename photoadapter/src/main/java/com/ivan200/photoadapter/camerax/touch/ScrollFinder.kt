package com.ivan200.photoadapter.camerax.touch

import android.content.Context
import android.graphics.PointF
import android.view.GestureDetector
import android.view.MotionEvent
import kotlin.math.abs

/**
 * @author ivan200
 * @since 04.08.2022
 */
class ScrollFinder(context: Context, onScrollListener: ScrollFinder.OnScrollListener) {
    private var points = arrayOf(PointF(0f, 0f), PointF(0f, 0f))
    private var notify = false
    private var horizontal = true

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
            if (e1 == null || e2 == null) return false // Got some crashes about this.
            if (e1.x != points[0].x || e1.y != points[0].y) {
                // First step. We choose now if it's a vertical or horizontal scroll, and
                // stick to it for the whole gesture.
                horizontal = abs(distanceX) >= abs(distanceY)
                points[0].set(e1.x, e1.y)

                onScrollListener.onStartScroll(horizontal)
            }

            points[1].set(e2.x, e2.y)

            var distance = if (horizontal) {
                abs(points[1].x) - abs(points[0].x)
            } else {
                abs(points[1].y) - abs(points[0].y)
            }
            distance = abs(distance)

            if (horizontal) {
                if (points[1].x < points[0].x) distance = -distance
            } else {
                if (points[1].y > points[0].y) distance = -distance // When vertical, up = positive
            }
            onScrollListener.onScroll(horizontal, distance)
            notify = true
            return true
        }
    }

    private val gestureDetector = GestureDetector(context, gestureListener).apply {
        setIsLongpressEnabled(false)
    }

    fun handleTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            notify = false
        }
        gestureDetector.onTouchEvent(event)
        return notify
    }

    interface OnScrollListener {
        fun onStartScroll(horisontal: Boolean)
        fun onScroll(horisontal: Boolean, distance: Float)
    }
}
