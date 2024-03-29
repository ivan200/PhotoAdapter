package com.ivan200.photoadapter.base

import androidx.annotation.StringRes
import com.ivan200.photoadapter.R

/**
 * @author ivan200
 * @since 24.02.2022
 */
sealed class CameraViewState {
    object NotInitialized : CameraViewState()
    object NoPermissions : CameraViewState()
    object Initializing : CameraViewState()
    object Streaming : CameraViewState()
    class Error(val error: CameraError, val ex: Exception? = null) : CameraViewState()
}

enum class CameraError(
    @StringRes
    val messageRes: Int
) {
    /** Indicates that we could not find a camera for the current Facing value */
    NO_CAMERA(R.string.camera_error_no_camera),

    /**  Unknown error. No further info available. */
    CAMERA_UNKNOWN_ERROR(R.string.camera_error_unknown),

    /** The camera is disabled due to a device policy, and cannot be opened. */
    CAMERA_DISABLED(R.string.camera_error_disabled),

    /** The camera device is removable and has been disconnected from the Android device,
     *  or the camera service has shut down the connection due to a higher-priority access request for the camera device. */
    CAMERA_DISCONNECTED(R.string.camera_error_disconnected),

    /** The camera device is currently in the error state. The camera has failed to open
     *  or has failed at a later time as a result of some non-user interaction. */
    CAMERA_ERROR(R.string.camera_error_in_error_state),

    /** The camera device is in use already. */
    CAMERA_IN_USE(R.string.camera_error_in_use),

    /** The system-wide limit for number of open cameras or camera resources has been reached,
     *  and more camera devices cannot be opened. */
    CAMERA_MAX_IN_USE(R.string.camera_error_max_in_use),

    /** The camera is unavailable due to NotificationManager.Policy. Some API 28 devices cannot access the camera
     *  when the device is in "Do Not Disturb" mode. The camera will not be accessible until "Do Not Disturb" mode is disabled. */
    CAMERA_UNAVAILABLE_DO_NOT_DISTURB(R.string.camera_error_do_not_disturb)
}

