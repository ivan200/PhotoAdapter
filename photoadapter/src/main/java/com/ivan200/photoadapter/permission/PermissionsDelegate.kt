package com.ivan200.photoadapter.permission

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.ivan200.photoadapter.CameraBuilder
import com.ivan200.photoadapter.R
import com.ivan200.photoadapter.utils.applyIf

/**
 * Delegate for processing permissions
 *
 * @property mActivity activity for checking permissions
 * @property fragment for requesting permissions
 * @property onPermissionGranted function what called after permissions granted
 * @property onPermissionRejected function what called after permissions rejected
 * @param savedInstanceState instance state to restore state of this delegate
 *
 * Created by Ivan200 on 25.10.2019.
 *
 * TODO переделать на нормальный
 */
@Suppress("MemberVisibilityCanBePrivate")
open class PermissionsDelegate(
    var mActivity: Activity,
    var fragment: Fragment? = null,
    savedInstanceState: Bundle?,
    var onPermissionGranted: (() -> Unit)? = null,
    var onPermissionRejected: (() -> Unit)? = null,
    val codeForRequestPermissions: Int = 3254
) {

    private var allPermissions: Array<String> = arrayListOf(Manifest.permission.CAMERA).toTypedArray()
    private var allPermissionStates = ArrayList(allPermissions.map { PermissionState(it) })
    private var dialogTheme: Int = 0
    private var deniedPermissionsArray: Array<String> = emptyArray()
    private var deniedPermissionsStates = ArrayList<PermissionState>()

    init {
        @Suppress("UNCHECKED_CAST")
        savedInstanceState?.apply {
            (getSerializable(KEY_PERMISSION_STATES) as? ArrayList<PermissionState>)?.let { deniedPermissionsStates = it }
            (getSerializable(KEY_ALL_PERMISSION_STATES) as? ArrayList<PermissionState>)?.let { allPermissionStates = it }
        }
    }

    /**
     * Initializing the permissions list depending on the Builder parameters
     */
    open fun initWithBuilder(cameraBuilder: CameraBuilder? = null) {
        allPermissions = arrayListOf(Manifest.permission.CAMERA)
            .apply {
                if (cameraBuilder?.galleryName != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }
            }.toTypedArray()
        allPermissionStates = ArrayList(allPermissions.map { PermissionState(it) })
        dialogTheme = cameraBuilder?.dialogTheme ?: 0
    }

    /**
     * Saving the state in the bundle of the current activity
     * since after going to the application settings and changing the permissions, the activity may die,
     * you need to save and restore the data of the permissions in order to process them correctly
     */
    fun saveInstanceState(outState: Bundle) {
        outState.putSerializable(KEY_PERMISSION_STATES, deniedPermissionsStates)
        outState.putSerializable(KEY_ALL_PERMISSION_STATES, allPermissionStates)
    }

    open fun isCameraAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mActivity.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        } else {
            mActivity.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
        }
    }

    /**
     * Main method for requesting permissions
     */
    open fun requestPermissions() {
        if (!isCameraAvailable()) {
            return
        }
        allPermissionStates = ArrayList(allPermissions.map { PermissionState(it) })
        deniedPermissionsStates = ArrayList(allPermissionStates.filter { !it.hasPermission(mActivity) })
        deniedPermissionsArray = deniedPermissionsStates.map { it.permission }.toTypedArray()
        if (deniedPermissionsStates.isNotEmpty()) {
            deniedPermissionsStates.forEach { it.setBefore(mActivity) }
            if (fragment != null) {
                fragment!!.requestPermissions(deniedPermissionsArray, codeForRequestPermissions)
            } else {
                ActivityCompat.requestPermissions(mActivity, deniedPermissionsArray, codeForRequestPermissions)
            }
        } else {
            // Если все пермишены разрешены, то ничего не делаем
            onPermissionGranted?.invoke()
        }
    }

    /**
     * Method to return the result of a permission request
     *
     * @param requestCode code for request permissions
     */
    open fun onRequestPermissionsResult(requestCode: Int) {
        if (requestCode == codeForRequestPermissions) {
            deniedPermissionsStates.forEach { it.setAfter(mActivity) }
            val permissionsMap = deniedPermissionsStates.map { Pair(it, it.getState(mActivity)) }
            val deniedPermission = permissionsMap.firstOrNull { it.second.isDenied() }
            if (deniedPermission != null) {
                showDialogOnPermissionRejected(deniedPermission.first.permission, deniedPermission.second.canReAsk())
            } else {
                onPermissionGranted?.invoke()
            }
        }
    }

    /**
     * Receive the result from a previous call to startActivityForResult(Intent, int)
     *
     * @param requestCode code for request permissions
     */
    open fun onActivityResult(requestCode: Int) {
        if (requestCode == codeForRequestPermissions) {
            requestPermissions()
        }
    }

    /**
     * Show dialog on permission rejected
     *
     * @param blockedPermission String of permission which was rejected
     * @param canReAsk if you can call system dialog for request permission once again
     */
    open fun showDialogOnPermissionRejected(blockedPermission: String, canReAsk: Boolean = false) {
        val messageId = when (blockedPermission) {
            Manifest.permission.CAMERA -> {
                if (canReAsk) R.string.permission_camera_rationale
                else R.string.permission_camera_rationale_goto_settings
            }
            else -> {
                if (canReAsk) R.string.permission_sdcard_rationale
                else R.string.permission_sdcard_rationale_goto_settings
            }
        }

        val dialog = AlertDialog.Builder(mActivity, dialogTheme)
            .setTitle(android.R.string.dialog_alert_title)
            .setIconAttribute(android.R.attr.alertDialogIcon)
            .setMessage(messageId)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
                if (canReAsk) {
                    requestPermissions()
                } else {
                    openAppSettings(mActivity)
                }
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                onPermissionRejected?.invoke()
                dialog.dismiss()
            }
            .applyIf(onPermissionRejected != null) {
                setOnCancelListener { dialog ->
                    onPermissionRejected?.invoke()
                    dialog.dismiss()
                }
            }
            .create()
            .applyIf(onPermissionRejected != null) {
                setOnCancelListener { d ->
                    onPermissionRejected?.invoke()
                    d.dismiss()
                }
                setOnKeyListener { arg0, keyCode, _ ->
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        onPermissionRejected?.invoke()
                        arg0.dismiss()
                    }
                    true
                }
            }
        dialog.show()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            dialog.getWindow()?.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    /**
     * Open application settings
     *
     * @param activity activity of application in which parameters we will go
     */
    open fun openAppSettings(activity: Activity) {
        PermissionSettingUtils.gotoPhonePermissionSettings(fragment, activity, codeForRequestPermissions)
//        Intent()
//            .setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
//            .apply {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
//                    putExtra(Intent.EXTRA_PACKAGE_NAME, activity.packageName)
//                }
//            }
//            .setData(Uri.fromParts("package", activity.packageName, null))
//            .addCategory(Intent.CATEGORY_DEFAULT)
//            .addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
//            .apply {
//                try {
//                    if (fragment != null) {
//                        fragment!!.startActivityForResult(this, codeForRequestPermissions)
//                    } else {
//                        activity.startActivityForResult(this, codeForRequestPermissions)
//                    }
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
//            }
    }

    companion object {
        private const val KEY_PERMISSION_STATES = "KEY_PERMISSION_STATES"
        private const val KEY_ALL_PERMISSION_STATES = "KEY_ALL_PERMISSION_STATES"
    }
}
