package com.ivan200.photoadapter.permission

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.ivan200.photoadapter.CameraBuilder
import com.ivan200.photoadapter.R
import com.ivan200.photoadapter.utils.applyIf

//
// Created by Ivan200 on 25.10.2019.
//
@Suppress("MemberVisibilityCanBePrivate")
open class PermissionsDelegate(
    var mActivity: Activity,
    var fragment: Fragment? = null,
    savedInstanceState: Bundle?,
    var cameraBuilder: CameraBuilder,
    var onPermissionGranted: (() -> Unit)? = null,
    var onPermissionRejected: (() -> Unit)? = null
) {
    /**
     * Пермишены для фотографирования и сохранения фоток в галерею
     */
    var mPermissions: Array<String> = arrayListOf(Manifest.permission.CAMERA)
        .applyIf(cameraBuilder.galleryName != null) {
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }.toTypedArray()

    private var permissionStates = ArrayList(mPermissions.map { PermissionState(it) })

    /**
     * Сохранение состояния в бандл текущей активити
     * так как после перехода в настройки приложения и изменения пермишенов, активити может умереть,
     * требуется сохранить и восстановить данные по пермишенам чтобы их коректно обработать
     */
    fun savedInstanceState(outState: Bundle) {
        outState.putSerializable(KEY_PERMISSION_STATES, permissionStates)
    }

    init {
        savedInstanceState?.apply {
            @Suppress("UNCHECKED_CAST")
            permissionStates = getSerializable(KEY_PERMISSION_STATES) as ArrayList<PermissionState>
        }
    }

    open fun isCameraAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mActivity.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        } else {
            mActivity.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
        }
    }

    /**
     * Главный метод запроса разрешений
     */
    open fun requestPermissions() {
        if (!isCameraAvailable()) {
            return
        }

        permissionStates = ArrayList(mPermissions.map { PermissionState(it) })

        if (permissionStates.any { !it.hasPermission(mActivity) }) {
            permissionStates.forEach { it.setBefore(mActivity) }
            if (fragment != null) {
                fragment!!.requestPermissions(mPermissions, codeForRequestPermissions)
            } else {
                ActivityCompat.requestPermissions(
                    mActivity,
                    mPermissions,
                    codeForRequestPermissions
                )
            }
        } else {
            //Если все пермишены разрешены, то ничего не делаем
            onPermissionGranted?.invoke()
        }
    }

    /**
     * Метод, в который требуется возвращать результат запроса разрешений
     */
    open fun onRequestPermissionsResult(requestCode: Int) {
        if (requestCode == codeForRequestPermissions) {
            permissionStates.forEach { it.setAfter(mActivity) }

            val deniedPermission = permissionStates.firstOrNull { it.isDenied(mActivity) }
            if (deniedPermission != null) {
                showDialogOnPermissionRejected(deniedPermission.permission)
            } else {
                onPermissionGranted?.invoke()
            }
        }
    }

    open fun onActivityResult(requestCode: Int) {
        if (requestCode == codeForRequestPermissions) {
            requestPermissions()
        }
    }

    open fun showDialogOnPermissionRejected(blockedPermission: String) {
        val action: String = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val messageId = when (blockedPermission) {
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE -> {
                R.string.permission_sdcard_rationale
            }
            Manifest.permission.CAMERA -> {
                R.string.permission_camera_rationale
            }
            else -> R.string.permission_camera_rationale
        }

        AlertDialog.Builder(mActivity, cameraBuilder.dialogTheme)
            .setTitle(android.R.string.dialog_alert_title)
            .setMessage(messageId)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
                openAppSettings(mActivity, action)
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
            .show()
    }

    open fun openAppSettings(activity: Activity, action: String) {
        Intent()
            .setAction(action)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    putExtra(Intent.EXTRA_PACKAGE_NAME, activity.packageName)
                }
            }
            .setData(Uri.fromParts("package", activity.packageName, null))
            .addCategory(Intent.CATEGORY_DEFAULT)
            .addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            .apply {
                try {
                    if (fragment != null) {
                        fragment!!.startActivityForResult(this, codeForRequestPermissions)
                    } else {
                        activity.startActivityForResult(this, codeForRequestPermissions)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
    }

    companion object {
        private const val KEY_PERMISSION_STATES = "KEY_PERMISSION_STATES"
        private const val codeForRequestPermissions = 3254
    }
}
