package com.ivan200.photoadapter.base

import java.io.File

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

enum class CameraError {
    /** Indicates that we could not find a camera for the current Facing value */
    NO_CAMERA,
    /**  Unknown error. No further info available. */
    CAMERA_UNKNOWN_ERROR,
    /** The camera is disabled due to a device policy, and cannot be opened. */
    CAMERA_DISABLED,
    /** The camera device is removable and has been disconnected from the Android device,
     *  or the camera service has shut down the connection due to a higher-priority access request for the camera device. */
    CAMERA_DISCONNECTED,
    /** The camera device is currently in the error state. The camera has failed to open
     *  or has failed at a later time as a result of some non-user interaction. */
    CAMERA_ERROR,
    /** The camera device is in use already. */
    CAMERA_IN_USE,
    /** The system-wide limit for number of open cameras or camera resources has been reached,
     *  and more camera devices cannot be opened. */
    CAMERA_MAX_IN_USE,
    /** The camera is unavailable due to NotificationManager.Policy. Some API 28 devices cannot access the camera
     *  when the device is in "Do Not Disturb" mode. The camera will not be accessible until "Do Not Disturb" mode is disabled. */
    CAMERA_UNAVAILABLE_DO_NOT_DISTURB,
}

enum class CaptureError {
    /** An unknown error occurred.See message parameter in onError callback or log for more details. */
    ERROR_UNKNOWN,
    /** An error occurred while attempting to read or write a file, such as when saving an image to a File. */
    ERROR_FILE_IO,
    /** An error reported by camera framework indicating the capture request is failed. */
    ERROR_CAPTURE_FAILED,
    /** An error indicating the request cannot be done due to camera is closed. */
    ERROR_CAMERA_CLOSED,
    /** An error indicating this ImageCapture is not bound to a valid camera. */
    ERROR_INVALID_CAMERA
}

sealed class TakePictureResult {
    class ImageTaken(val file: File) : TakePictureResult()
    class ImageTakeException(val error: CaptureError, val ex: Throwable? = null) : TakePictureResult()
}
