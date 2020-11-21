package com.ivan200.photoadapter.permission

import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.Serializable

/**
 * Параметры разрешений до и после их запроса
 */
@Suppress("MemberVisibilityCanBePrivate")
data class PermissionState(val permission: String) : Serializable {
    var beforeRat: Boolean? = null
    var afterRat: Boolean? = null


    fun setBefore(activity: Activity) {
        beforeRat = ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    fun setAfter(activity: Activity) {
        afterRat = ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }


    fun hasPermission(activity: Activity): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getState(activity: Activity): StateEnum {
        val state = when {
            beforeRat == null || afterRat == null -> StateEnum.UNKNOWN

            //И до и после запроса пермишены были запрещены
            beforeRat!! && afterRat!! -> {
                StateEnum.REJECTED_ALL
            }

            //толко что были разрешены, в первый раз
            beforeRat!! && !afterRat!! -> {
                if (!hasPermission(activity)) {
                    StateEnum.FIRST_NEVER_ASK
                } else {
                    StateEnum.FIRST_GRANTED
                }
            }
            !beforeRat!! && afterRat!! -> {
                if (!hasPermission(activity)) {
                    //только что были запрещены
                    StateEnum.FIRST_DENIED
                } else {
                    //были запрещены, и теперь юзер разрешил
                    StateEnum.SECOND_GRANTED
                }
            }
            //и до и после не нужно показывать диалог
            !beforeRat!! && !afterRat!! -> {
                if (!hasPermission(activity)) {
                    StateEnum.REJECTED_NEVER_ASK
                } else {
                    StateEnum.ALWAYS_GRANTED
                }
            }
            else -> throw Exception()
        }
        Log.w(
            this::class.java.simpleName,
            "state for ${permission.split(".").last()}=${state.name}"
        )

        return state
    }

    fun isDenied(activity: Activity): Boolean {
        val state = getState(activity)

        return state == StateEnum.REJECTED_ALL
                || state == StateEnum.FIRST_DENIED
                || state == StateEnum.FIRST_NEVER_ASK
                || state == StateEnum.REJECTED_NEVER_ASK
    }
}