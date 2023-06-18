@file:Suppress("unused")

package com.ivan200.photoadapter.utils

import android.animation.Animator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.os.Parcelable
import android.util.TypedValue
import android.view.Display
import android.view.Surface
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.RelativeLayout
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.ivan200.photoadapter.R
import java.io.Serializable
import java.lang.ref.WeakReference
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.math.ceil

fun <T : View> T.onClick(function: () -> Unit): T {
    setOnClickListener { function() }
    return this
}

fun <T : Activity> T.lockOrientation() {
    displayCompat?.let {
        val rotation = it.rotation
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

fun <T : Activity> T.unlockOrientation() {
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
}

@Suppress("DEPRECATION")
val Context.displayCompat: Display?
    get() = if (SDK_INT >= Build.VERSION_CODES.R) display else null
        ?: ContextCompat.getSystemService(this, WindowManager::class.java)?.defaultDisplay

@Suppress("DEPRECATION")
fun <T : Activity> T.hideSystemUI() {
    if (SDK_INT >= Build.VERSION_CODES.R) {
        window.setDecorFitsSystemWindows(false)
        window.insetsController?.apply {
            hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    } else {
        // Before setting full screen flags, we must wait a bit to let UI settle; otherwise, we may
        // be trying to set app to immersive mode before it's ready and the flags do not stick
        window.decorView.postDelayed({
            var flag = View.SYSTEM_UI_FLAG_LOW_PROFILE or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            if (SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                flag = flag or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            }
            if (SDK_INT >= Build.VERSION_CODES.KITKAT) {
                flag = flag or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            }
            window.decorView.systemUiVisibility = flag
        }, 500L)
    }
}

@Suppress("DEPRECATION", "KotlinConstantConditions")
fun <T : Activity> T.showSystemUI() {
//    WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())

    if (SDK_INT >= Build.VERSION_CODES.R) {
        window.setDecorFitsSystemWindows(true)
        window.insetsController?.apply {
            show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        }
    } else {
        var flag = View.SYSTEM_UI_FLAG_VISIBLE
        if (SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            flag = flag or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
        window.decorView.systemUiVisibility = flag
    }
}

internal fun View.padBottomViewWithInsets(insets: WindowInsetsCompat) {
    val land = this.resources.getBoolean(R.bool.is_land)
    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
    this.setPadding(
        if (land) 0 else systemBars.left,
        if (land) systemBars.top else 0,
        systemBars.right,
        systemBars.bottom
    )
}

internal fun View.padTopViewWithInsets(insets: WindowInsetsCompat) {
    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
    this.layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, systemBars.top)
}

internal fun rotateItems(angle: Int, vararg views: View) {
    views.forEach {
        val rotationDuration = it.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
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
internal fun ImageButton.simulateClick(delay: Long = ANIMATION_SLOW_MILLIS) {
    performClick()
    isPressed = true
    invalidate()
    val ref = WeakReference(this)
    postDelayed({
        ref.get()?.apply {
            isPressed = false
            invalidate()
        }
    }, delay)
}

/**
 * Changing the visibility of the view with smooth showing or hiding
 *
 * @param show     show or hide the view
 * @param listener view click listener, so when the view start smoothly hiding, view will immediately unclickable
 *
 * partially from here
 * https://developer.android.com/develop/ui/views/animations/reveal-or-hide-view#CrossfadeAnimate
 */
fun View.animateFadeVisibility(show: Boolean, listener: View.OnClickListener? = null) {
    val duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
    if (show) {
        if (!isVisible || alpha != 1f) {
            isVisible = true
            clearAnimation()
            animate().alpha(1f).setDuration(duration).setListener(null)
        }
        listener?.let { setOnClickListener(it) }
    } else {
        if (isVisible) {
            clearAnimation()
            animate().alpha(0f).setDuration(duration).setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) = Unit
                override fun onAnimationCancel(animation: Animator) = Unit
                override fun onAnimationRepeat(animation: Animator) = Unit
                override fun onAnimationEnd(animation: Animator) {
                    isVisible = false
                }
            })
        }
        listener?.let { setOnClickListener(null) }
    }
}

/**
 * Получение ресурса цвета, привязаного к теме через аттрибуты, например `android.R.attr.textColorPrimary`
 */
@SuppressLint("ResourceAsColor")
@ColorInt
fun Context.getColorResCompat(@AttrRes id: Int) = TypedValue()
    .also { theme.resolveAttribute(id, it, true) }
    .run { if (resourceId != 0) resourceId else data }
    .let { ContextCompat.getColor(this, it) }


@ColorInt
fun Context.getColorCompat(@ColorRes id: Int) = ContextCompat.getColor(this, id)

@JvmOverloads
fun Number.dpToPxInt(context: Context? = null): Int {
    val res = context?.resources ?: android.content.res.Resources.getSystem()
    return ceil(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), res.displayMetrics)).toInt()
}

fun Number.dpToPx(context: Context? = null): Float {
    val res = context?.resources ?: android.content.res.Resources.getSystem()
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), res.displayMetrics)
}


inline fun <reified T : Serializable> Bundle.serializableCompat(key: String): T? = when {
    SDK_INT >= TIRAMISU -> getSerializable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getSerializable(key) as? T
}

inline fun <reified T : Parcelable> Intent.parcelableCompat(key: String): T? = when {
    SDK_INT >= TIRAMISU -> getParcelableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
}

inline fun <reified T : Parcelable> Bundle.parcelableArrayCompat(key: String): Array<out T>? = when {
    SDK_INT >= TIRAMISU -> getParcelableArray(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableArray(key)?.filterIsInstance<T>()?.toTypedArray()
}

inline fun <reified T : Parcelable> Intent.parcelableArrayCompat(key: String): Array<out T>? = when {
    SDK_INT >= TIRAMISU -> getParcelableArrayExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableArrayExtra(key)?.filterIsInstance<T>()?.toTypedArray()
}


//
//Logger
//
inline val <T : Any> T.TAG: String get() = this::class.java.simpleName
inline val <T : Any> T.logger: Logger get() = Logger.getLogger(this.TAG)
fun <T : Any> T.log(value: Any) = logger.log(Level.INFO, value.toString())
fun <T : Any> T.log(tag: String, value: Any) = Logger.getLogger(tag).log(Level.INFO, value.toString())