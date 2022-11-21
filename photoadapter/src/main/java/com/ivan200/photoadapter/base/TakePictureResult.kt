package com.ivan200.photoadapter.base

import androidx.annotation.StringRes
import com.ivan200.photoadapter.R
import java.io.File

/**
 * @author ivan200
 * @since 24.02.2022
 */
sealed class TakePictureResult {
    class ImageTaken(val file: File) : TakePictureResult()
    class ImageTakeException(val error: CaptureError, val ex: Throwable? = null) : TakePictureResult()
}

enum class CaptureError(
    @StringRes
    val messageRes: Int
) {
    /** An unknown error occurred. See message parameter in onError callback or log for more details. */
    ERROR_UNKNOWN(R.string.capture_error_unknown),

    /** An error occurred while attempting to read or write a file, such as when saving an image to a File. */
    ERROR_FILE_IO(R.string.capture_error_file_io),

    /** An error reported by camera framework indicating the capture request is failed. */
    ERROR_CAPTURE_FAILED(R.string.capture_error_failed),

    /** An error indicating the request cannot be done due to camera is closed. */
    ERROR_CAMERA_CLOSED(R.string.capture_error_camera_closed),

    /** An error indicating this ImageCapture is not bound to a valid camera. */
    ERROR_INVALID_CAMERA(R.string.capture_error_invalid_camera)
}