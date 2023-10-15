package com.ivan200.photoadapter.utils

import android.content.Context
import android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3
import android.hardware.camera2.CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL
import android.hardware.camera2.CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Parcelable
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.core.content.ContextCompat
import kotlinx.parcelize.Parcelize

/**
 * Selector of camera implementation
 *
 * @author ivan200
 * @since 14.08.2022
 */
interface CameraImplSelector : Parcelable {
    fun isImplCamera2(context: Context): Boolean

    /**
     * Force use Camera1 implementation
     */
    @Parcelize
    object AlwaysCamera1 : CameraImplSelector {
        override fun isImplCamera2(context: Context): Boolean = false
    }

    /**
     * If Android 5.0+, then Camera2, otherwise Camera1
     */
    @Parcelize
    object Camera2FromApi21 : CameraImplSelector {
        @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.LOLLIPOP)
        override fun isImplCamera2(context: Context): Boolean = SDK_INT >= Build.VERSION_CODES.LOLLIPOP
    }

    /**
     * If any camera have full Camera2 support, then Camera2 otherwise Camera1
     * Or if any camera has external supported level (since Camera1 is not support it)
     */
    @Parcelize
    object Camera2IfAnyFullSupport : CameraImplSelector {
        override fun isImplCamera2(context: Context): Boolean {
            if (SDK_INT < Build.VERSION_CODES.LOLLIPOP) return false
            ContextCompat.getSystemService(context, CameraManager::class.java)?.apply {
                return try {
                    cameraIdList.any {
                        val supportedLevel = getCameraCharacteristics(it)[INFO_SUPPORTED_HARDWARE_LEVEL]
                        supportedLevel == INFO_SUPPORTED_HARDWARE_LEVEL_FULL
                                || (SDK_INT >= Build.VERSION_CODES.N && supportedLevel == INFO_SUPPORTED_HARDWARE_LEVEL_3)
                                || (SDK_INT >= Build.VERSION_CODES.P && supportedLevel == INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL)
                    }
                } catch (ex: Throwable) {
                    false
                }
            }
            return false
        }
    }
}
