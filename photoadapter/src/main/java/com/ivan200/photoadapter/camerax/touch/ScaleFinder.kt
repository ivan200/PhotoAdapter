package com.ivan200.photoadapter.camerax.touch

import android.content.Context
import android.view.MotionEvent
import android.view.ScaleGestureDetector

/**
 * @author ivan200
 * @since 04.08.2022
 */
class ScaleFinder(context: Context, onScaleListener: OnScaleListener) {

    private var notify = false

    private val gestureListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            onScaleListener.onScale(detector.scaleFactor)

            // TODO Попробовать прикрутить переключение по скейлу на более широкоугольную камеру

            notify = true
            return true
        }
    }

    private val gestureDetector = ScaleGestureDetector(context, gestureListener)

    fun handleTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            notify = false
        }
        gestureDetector.onTouchEvent(event)
        return notify
    }

    interface OnScaleListener {
        fun onScale(scaleFactor: Float)
    }
}
