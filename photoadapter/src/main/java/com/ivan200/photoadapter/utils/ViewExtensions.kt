@file:Suppress("unused")

package com.ivan200.photoadapter.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.RelativeLayout
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.ivan200.photoadapter.R


fun <T : View> T.onClick(function: () -> Unit): T {
    setOnClickListener { function() }
    return this
}

fun <T : View> T.show(): T {
    if (visibility != View.VISIBLE) visibility = View.VISIBLE
    return this
}

fun <T : View> T.hide(): T {
    if (visibility != View.GONE) visibility = View.GONE
    return this
}

fun <T : View> T.invisible(): T {
    if (visibility != View.INVISIBLE) visibility = View.INVISIBLE
    return this
}

fun <T : View> T.showIf(condition: () -> Boolean): T {
    return if (condition()) show() else hide()
}

fun <T : View> T.hideIf(condition: () -> Boolean): T {
    return if (condition()) hide() else show()
}

fun <T : View> T.invisibleIf(condition: () -> Boolean): T {
    return if (condition()) invisible() else show()
}

@SuppressLint("InlinedApi")
inline fun <T> T.applyIf(condition: Boolean, block: T.() -> Unit): T {
    return if (condition) this.apply(block) else this
}

fun <T : Activity> T.lockOrientation() {
    val windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    if (windowManager != null) {
        val rotation = windowManager.defaultDisplay.rotation
        val orientation = when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> when (rotation) {
                Surface.ROTATION_0,
                Surface.ROTATION_90 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                else -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            }
            Configuration.ORIENTATION_PORTRAIT -> when (rotation) {
                Surface.ROTATION_0,
                Surface.ROTATION_270 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                else -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            }
            else -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        requestedOrientation = orientation
    }
}

fun <T : View> T.hideSystemUI() {
    var flag = View.SYSTEM_UI_FLAG_LOW_PROFILE or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        flag = flag or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        flag = flag or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    systemUiVisibility = flag
}

fun <T : View> T.showSystemUI() {
    var flag = View.SYSTEM_UI_FLAG_VISIBLE
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        flag = flag or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }
    systemUiVisibility = flag
}

fun <T : Activity> T.unlockOrientation() {
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
}

internal fun View.padBottomViewWithInsets(insets: WindowInsetsCompat) {
    val land = this.resources.getBoolean(R.bool.is_land)
    this.setPadding(
        if (land) 0 else insets.systemWindowInsetLeft,
        if (land) insets.systemWindowInsetTop else 0,
        insets.systemWindowInsetRight,
        insets.systemWindowInsetBottom
    )
}

internal fun View.padTopViewWithInsets(insets: WindowInsetsCompat) {
    this.layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, insets.systemWindowInsetTop)
}

internal fun rotateItems(angle: Int, vararg views: View) {
    val rotationDuration = 200L
    views.forEach {
        it.animate().rotation(angle.toFloat()).setDuration(rotationDuration).start()
    }
}


/** Milliseconds used for UI animations */
internal const val ANIMATION_FAST_MILLIS = 50L
internal const val ANIMATION_SLOW_MILLIS = 100L

/**
 * Simulate a button click, including a small delay while it is being pressed to trigger the
 * appropriate animations.
 */
internal fun ImageButton.simulateClick(delay: Long = ANIMATION_FAST_MILLIS) {
    performClick()
    isPressed = true
    invalidate()
    postDelayed({
        invalidate()
        isPressed = false
    }, delay)
}
