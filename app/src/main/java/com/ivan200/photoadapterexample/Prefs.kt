package com.ivan200.photoadapterexample

import android.content.Context
import com.ivan200.photoadapterexample.utils.AnyPref

class Prefs(context: Context) {
    var facingBack by AnyPref(context, true)
    var allowChangeCamera by AnyPref(context, true)
    var allowPreviewResult by AnyPref(context, true)
    var allowMultipleImages by AnyPref(context, true)
    var lockRotate by AnyPref(context, true)
    var saveToGallery by AnyPref(context, false)
    var galleryName by AnyPref(context, context.getString(R.string.app_name))
    var allowToggleFit by AnyPref(context, true)
    var fullScreenMode by AnyPref(context, true)
    var maxImageSize by AnyPref(context, 1200)
    var useSnapshot by AnyPref(context, true)
    var fillPreview by AnyPref(context, true)
    var flipFrontal by AnyPref(context, true)
    var forceCamera1 by AnyPref(context, false)
    var blurOnSwitch by AnyPref(context, true)

    var images by AnyPref(context, mutableSetOf<String>())
    var imagePreviewNumber by AnyPref(context, 0)

    val sortedImages get() = images.sortedDescending()
}
