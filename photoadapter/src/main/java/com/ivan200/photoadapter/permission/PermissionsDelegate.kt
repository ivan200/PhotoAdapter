package com.ivan200.photoadapter.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import com.ivan200.photoadapter.CameraBuilder
import com.ivan200.photoadapter.R
import com.ivan200.photoadapter.utils.SaveTo

/**
 * Delegate class for checking and requesting permissions
 *
 * @param activity             activity for checking permissions
 * @param savedInstanceState   instance state to restore state of this delegate
 * @param onPermissionResult   function what called after getting permissions request result
 *
 * @author ivan200
 * @since 06.08.2022
 */
open class PermissionsDelegate(
    val activity: ComponentActivity,
    savedInstanceState: Bundle?,
    val dialogTheme: Int = 0,
    var onPermissionResult: (ResultType) -> Unit
) {

    private var permissions: Array<String> = emptyArray()
    private var needToShowDialog = true
    private var permissionResults: List<Pair<String, Boolean>> = emptyList()
    private val missingPermissions: Array<String> get() = permissionResults.filter { it.second == false }.map { it.first }.toTypedArray()
    private val hasAllPermissions get() = permissionResults.isEmpty() || permissionResults.all { it.second == true }

    private var resultLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        updatePermissionResults()
        if (hasAllPermissions) {
            onPermissionResult.invoke(ResultType.Allow.AfterSettings)
        } else {
            needToShowDialog = false
            requestPermissionLauncher.launch(missingPermissions)
        }
    }

    private var requestPermissionLauncher = activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        updatePermissionResults()
        if (hasAllPermissions) {
            onPermissionResult.invoke(if (needToShowDialog) ResultType.Allow.SystemRequest else ResultType.Allow.SystemRequest2)
        } else {
            if (needToShowDialog) {
                val missedPermission = permissionResults.first { it.second == false }.first
                showDialogOnPermissionRejected(missedPermission)
            } else {
                onPermissionResult.invoke(ResultType.Denied.DeniedAfterSettings)
            }
        }
    }

    init {
        @Suppress("UNCHECKED_CAST")
        savedInstanceState?.apply {
            getBoolean(KEY_NEED_TO_SHOW_DIALOG, true).let { needToShowDialog = it }
            getStringArray(KEY_PERMISSIONS).let { permissions = it ?: emptyArray() }
        }
        updatePermissionResults()
    }

    private fun updatePermissionResults() {
        permissionResults = permissions.map {
            it to (checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED)
        }
    }

    private fun getRationale(): Boolean = missingPermissions.let {
        it.isNotEmpty() && it.any { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
    }

    /**
     *
     */
    fun queryPermissionsOnStart() {
        updatePermissionResults()
        if (hasAllPermissions) {
            onPermissionResult.invoke(ResultType.Allow.AlreadyHas)
        } else {
            needToShowDialog = true
            requestPermissionLauncher.launch(missingPermissions)
        }
    }

    /**
     * Initializing the permissions list depending on the Builder parameters
     *
     * @param cameraBuilder
     */
    open fun initWithBuilder(cameraBuilder: CameraBuilder) {
        permissions = arrayListOf(Manifest.permission.CAMERA)
            .apply {
                if (cameraBuilder.saveTo is SaveTo.ToGalleryWithAlbum && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }
            }.toTypedArray()
    }

    /**
     *
     *
     * @param context
     */
    open fun openAppSettings(context: Context) {
        PermissionSettingUtils.gotoPhonePermissionSettings(resultLauncher, activity, this::canNotGoToSettings)
    }

    fun canNotGoToSettings() {
        onPermissionResult.invoke(ResultType.Denied.CanNotGoToSettings)
    }

    /**
     * Show dialog on permission rejected
     *
     * @param blockedPermission string of permission which was rejected
     * @param canReAsk          if you can call system dialog for request permission once again
     */
    open fun showDialogOnPermissionRejected(blockedPermission: String) {
        val titleId = when (blockedPermission) {
            Manifest.permission.CAMERA -> R.string.permission_camera_goto_settings_title
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> R.string.permission_sdcard_goto_settings_title
            else -> 0
        }

        val messageId = when (blockedPermission) {
            Manifest.permission.CAMERA -> R.string.permission_camera_rationale_goto_settings
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> R.string.permission_sdcard_rationale_goto_settings
            else -> 0
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(activity, dialogTheme)
            .setTitle(titleId)
            .setIconAttribute(android.R.attr.alertDialogIcon)
            .setMessage(messageId)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
                openAppSettings(activity)
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                onPermissionResult.invoke(ResultType.Denied.CustomDialogNo)
                dialog.dismiss()
            }
            .setOnCancelListener { dialog ->
                onPermissionResult.invoke(ResultType.Denied.CustomDialogCancelled)
                dialog.dismiss()
            }
            .create()
            .apply {
                setOnCancelListener { d ->
                    onPermissionResult.invoke(ResultType.Denied.CustomDialogCancelled)
                    d.dismiss()
                }
                setOnKeyListener { arg0, keyCode, _ ->
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        onPermissionResult.invoke(ResultType.Denied.CustomDialogCancelled)
                        arg0.dismiss()
                    }
                    true
                }
            }
        dialog.show()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    /**
     * Saving the state in the bundle of the current activity
     * since after going to the application settings and changing the permissions, the activity may die,
     * you need to save data in [outState] and restore it in order to process them correctly
     */
    fun saveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_NEED_TO_SHOW_DIALOG, needToShowDialog)
        outState.putStringArray(KEY_PERMISSIONS, permissions)
    }

    private companion object {
        const val TAG = "PermissionsDelegate"
        const val KEY_NEED_TO_SHOW_DIALOG = "KEY_NEED_TO_SHOW_DIALOG"
        const val KEY_PERMISSIONS = "KEY_PERMISSIONS"
    }
}
