package com.ivan200.photoadapter.permission

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import java.util.*

object PermissionSettingUtils {

    fun gotoPhonePermissionSettings(launcher: ActivityResultLauncher<Intent>, activity: Activity, onFail: () -> Unit) {
        val brand = try {
            Build.BRAND.lowercase(Locale.US)
        } catch (e: Exception) {
            null
        }
        when (brand) {
            "redmi", "xiaomi" -> gotoMiuiPermission(launcher, activity, onFail)
            "honor", "huawei" -> gotoHuaweiPermission(launcher, activity, onFail)
            "meizu" -> gotoMeizuPermission(launcher, activity, onFail)
            else -> goToDefaultSettings(launcher, activity, onFail)
        }
    }

    /**
     * Jump to miui's permission management page
     */
    private fun gotoMiuiPermission(launcher: ActivityResultLauncher<Intent>, activity: Activity, onFail: () -> Unit) {
        val intent = Intent("miui.intent.action.APP_PERM_EDITOR")
        var componentName: ComponentName
        componentName =
            ComponentName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity") // Permission settings
        intent.component = componentName
        if (intent.resolveActivity(activity.packageManager) == null) {
            componentName = ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.permissions.AppPermissionsEditorActivity"
            ) // Permission management
            intent.component = componentName
        }
        intent.putExtra("extra_pkgname", activity.packageName)
        try {
            launcher.launch(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            gotoMeizuPermission(launcher, activity, onFail)
        }
    }

    /**
     * Jump to Meizu's permission management page
     */
    private fun gotoMeizuPermission(launcher: ActivityResultLauncher<Intent>, activity: Activity, onFail: () -> Unit) {
        val intent = Intent("com.meizu.safe.newpermission.ui.AppPermissionsActivity")
        intent.action = "com.meizu.safe.security.SHOW_APPSEC"
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        intent.putExtra("packageName", activity.packageName)
        try {
            launcher.launch(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            gotoHuaweiPermission(launcher, activity, onFail)
        }
    }

    /**
     * Huawei's permission management page
     */
    private fun gotoHuaweiPermission(launcher: ActivityResultLauncher<Intent>, activity: Activity, onFail: () -> Unit) {
        try {
            val intent = Intent()
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.component = ComponentName(
                "com.android.packageinstaller",
                "com.android.packageinstaller.permission.ui.ManagePermissionsActivity"
            ) // Huawei permission settings
            if (intent.resolveActivity(activity.packageManager) == null) {
                intent.component =
                    ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.permissionmanager.ui.MainActivity"
                    ) // Huawei permission Management
            }
            launcher.launch(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            goToDefaultSettings(launcher, activity, onFail)
        }
    }

    /**
     * Get the App Details page intent
     *
     * @return
     */
    private fun goToDefaultSettings(launcher: ActivityResultLauncher<Intent>, activity: Activity, onFail: () -> Unit) {
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

        try {
            launcher.launch(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            onFail.invoke()
        }
    }
}
