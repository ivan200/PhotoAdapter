package com.ivan200.photoadapter.utils

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.os.Build
import android.os.Parcelable
import androidx.core.content.ContextCompat
import kotlinx.parcelize.Parcelize

/**
 * Selector of camera implementaton
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
        override fun isImplCamera2(context: Context): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
    }

    /**
     * If any camera have full Camera2 support, then Camera2 otherwise Camera1
     */
    @Parcelize
    object Camera2IfAnyFullSupport : CameraImplSelector {
        override fun isImplCamera2(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return false

            ContextCompat.getSystemService(context, CameraManager::class.java)?.apply {
                try {
                    return cameraIdList.any {
                        getCameraCharacteristics(it).get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL).let {
                            it == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL ||
                                it == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3
                        }
                    }
                } catch (ex: Throwable) {
                    return false
                }
            }
            return false
        }
    }
}
