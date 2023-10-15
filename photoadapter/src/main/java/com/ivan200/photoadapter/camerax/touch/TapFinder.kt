package com.ivan200.photoadapter.camerax.touch

import android.content.Context
import android.graphics.PointF
import android.view.GestureDetector
import android.view.MotionEvent

/**
 * @author ivan200
 * @since 04.08.2022
 */
class TapFinder(context: Context, onTapListener: OnTapListener) {

    private var notify = false

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            onTapListener.onTap(PointF(e.x, e.y))
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

    fun interface OnTapListener {
        fun onTap(point: PointF)
    }
}
