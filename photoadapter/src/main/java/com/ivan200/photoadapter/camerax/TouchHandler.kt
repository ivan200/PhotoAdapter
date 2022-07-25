package com.ivan200.photoadapter.camerax

import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.camera.core.Camera
import androidx.camera.core.FocusMeteringAction
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.lang.ref.WeakReference

/**
 * @author ivan200
 * @since 24.02.2022
 */
class TouchHandler(
    private val camera: Camera?,
    private val preview: PreviewView,
    private val focusView: FocusView,
) : ScaleGestureDetector.SimpleOnScaleGestureListener(), View.OnTouchListener {

    private val scaleGestureDetector = ScaleGestureDetector(preview.context, this)

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        camera?.apply {
            val zoom = cameraInfo.zoomState.value?.zoomRatio ?: 0f
            val scale = zoom * detector.scaleFactor
            runCatching {
                cameraControl.setZoomRatio(scale)
            }
        }
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector?) {
        super.onScaleEnd(detector)
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        v.performClick()
        scaleGestureDetector.onTouchEvent(event)
        if (event.pointerCount == 1
            && event.action == MotionEvent.ACTION_UP
            && event.eventTime - event.downTime < TOUCH_SINGLE_PRESS_DELAY
        ) {
            camera?.apply {
                val point = preview.meteringPointFactory.createPoint(event.x, event.y)
                val action = FocusMeteringAction.Builder(point).build()
                focusView.anim(event.x, event.y)
                val future = cameraControl.startFocusAndMetering(action)
                val weakRef = WeakReference(focusView)
                future.addListener({
                    weakRef.get()?.hide()
                }, ContextCompat.getMainExecutor(preview.context))
            }
        }
        return true
    }

    private companion object {
        const val TOUCH_SINGLE_PRESS_DELAY = 200
    }
}
