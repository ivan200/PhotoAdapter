package com.ivan200.photoadapter.base

/**
 * @author ivan200
 * @since 24.02.2022
 */
sealed class CameraViewState {
    object NotInitialized : CameraViewState()
    object NoPermissions : CameraViewState()
    object Initializing : CameraViewState()
    object Streaming : CameraViewState()
    object Paused : CameraViewState()
    class Error(val error: CameraError, val ex: Exception? = null) : CameraViewState()
}

enum class CameraError {
    NO_CAMERA,
    CAMERA_UNKNOWN_ERROR,
    CAMERA_DISABLED,
    CAMERA_DISCONNECTED,
    CAMERA_ERROR,
    CAMERA_IN_USE,
    CAMERA_MAX_IN_USE,
    CAMERA_UNAVAILABLE_DO_NOT_DISTURB,
}

enum class CaptureError {
    ERROR_UNKNOWN,
    ERROR_FILE_IO,
    ERROR_CAPTURE_FAILED,
    ERROR_CAMERA_CLOSED,
    ERROR_INVALID_CAMERA
}

