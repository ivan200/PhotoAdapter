package com.ivan200.photoadapter.permission

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.fragment.app.Fragment
import java.util.*


object PermissionSettingUtils {

    fun gotoPhonePermissionSettings(fragment: Fragment? = null, activity: Activity, requestCode: Int) {
        var brand: String? = null
        try {
            brand = Build.BRAND
            brand = brand.toLowerCase(Locale.US)
        } catch (e: Exception) {
            e.printStackTrace()
            goToDefaultSettings(fragment, activity, requestCode)
        }
        when (brand) {
            "redmi", "xiaomi" -> gotoMiuiPermission(fragment, activity, requestCode)
            "honor", "huawei" -> gotoHuaweiPermission(fragment, activity, requestCode)
            "meizu" -> gotoMeizuPermission(fragment, activity, requestCode)
            else -> goToDefaultSettings(fragment, activity, requestCode)
        }
    }

    private fun start(intent: Intent, fragment: Fragment? = null, activity: Activity, requestCode: Int) {
        if (fragment != null) {
            fragment.startActivityForResult(intent, requestCode)
        } else {
            activity.startActivityForResult(intent, requestCode)
        }
    }


    /**
     * Jump to miui's permission management page
     */
    fun gotoMiuiPermission(fragment: Fragment? = null, activity: Activity, requestCode: Int) {
        val intent = Intent("miui.intent.action.APP_PERM_EDITOR")
        var componentName: ComponentName
        componentName =
            ComponentName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity") //Permission settings
        intent.component = componentName
        if (intent.resolveActivity(activity.packageManager) == null) {
            componentName = ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.permissions.AppPermissionsEditorActivity"
            ) //Permission management
            intent.component = componentName
        }
        intent.putExtra("extra_pkgname", activity.packageName)
        try {
            start(intent, fragment, activity, requestCode)
        } catch (e: Exception) {
            e.printStackTrace()
            gotoMeizuPermission(fragment, activity, requestCode)
        }
    }

    /**
     * Jump to Meizu's permission management page
     */
    fun gotoMeizuPermission(fragment: Fragment? = null, activity: Activity, requestCode: Int) {
        val intent = Intent("com.meizu.safe.newpermission.ui.AppPermissionsActivity")
        intent.action = "com.meizu.safe.security.SHOW_APPSEC"
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        intent.putExtra("packageName", activity.packageName)
        try {
            start(intent, fragment, activity, requestCode)
        } catch (e: Exception) {
            e.printStackTrace()
            gotoHuaweiPermission(fragment, activity, requestCode)
        }
    }

    /**
     * Huawei's permission management page
     */
    fun gotoHuaweiPermission(fragment: Fragment? = null, activity: Activity, requestCode: Int) {
        try {
            val intent = Intent()
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.component = ComponentName(
                "com.android.packageinstaller",
                "com.android.packageinstaller.permission.ui.ManagePermissionsActivity"
            ) //Huawei permission settings
            if (intent.resolveActivity(activity.packageManager) == null) {
                intent.component =
                    ComponentName("com.huawei.systemmanager", "com.huawei.permissionmanager.ui.MainActivity") //Huawei permission Management
            }
            start(intent, fragment, activity, requestCode)
        } catch (e: Exception) {
            e.printStackTrace()
            goToDefaultSettings(fragment, activity, requestCode)
        }
    }

    /**
     * Get the App Details page intent
     *
     * @return
     */
    fun goToDefaultSettings(fragment: Fragment? = null, activity: Activity, requestCode: Int) {
        val intent = Intent()
            .setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    putExtra(Intent.EXTRA_PACKAGE_NAME, activity.packageName)
                }
            }
            .setData(Uri.fromParts("package", activity.packageName, null))
            .addCategory(Intent.CATEGORY_DEFAULT)
            .addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)

        start(intent, fragment, activity, requestCode)
    }

}