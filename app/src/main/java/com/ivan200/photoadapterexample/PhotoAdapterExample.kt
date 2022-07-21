package com.ivan200.photoadapterexample

import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDexApplication

/**
 * @author ivan200
 * @since 22.07.2022
 */
class PhotoAdapterExample : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }
}