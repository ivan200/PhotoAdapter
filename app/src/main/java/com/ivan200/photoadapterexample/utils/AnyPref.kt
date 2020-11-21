package com.ivan200.photoadapterexample.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import kotlin.reflect.KProperty

//
// Created by Ivan200 on 15.11.2019.
//
/**
 * Non-nullable default preferences
 */
class AnyPref<T : Any>(val context: Context, val default: T) {
    @Suppress("UNCHECKED_CAST")
    operator fun getValue(thisRef: Any, property: KProperty<*>) =
        PreferenceManager.getDefaultSharedPreferences(context).get(property.name, default) as T

    operator fun setValue(thisRef: Any, property: KProperty<*>, value: T) =
        PreferenceManager.getDefaultSharedPreferences(context).put(property.name, value)

    private fun SharedPreferences.smartEdit(func: SharedPreferences.Editor.() -> Unit) {
        val editor = edit()
        editor.func()
        editor.apply()
    }

    private fun SharedPreferences.put(key: String, value: Any) = smartEdit {
        when (value) {
            is String -> putString(key, value)
            is Long -> putLong(key, value)
            is Int -> putInt(key, value)
            is Boolean -> putBoolean(key, value)
            is Float -> putFloat(key, value)
            is MutableSet<*> -> putStringSet(
                key, (value as? MutableSet<*>)
                    ?.filterIsInstance<String>()
                    ?.toMutableSet() ?: setOf()
            )
            else -> throw NotImplementedError()
        }
    }

    private fun SharedPreferences.get(key: String, defaultValue: Any): Any {
        return when (defaultValue) {
            is String -> getString(key, defaultValue)
            is Long -> getLong(key, defaultValue)
            is Int -> getInt(key, defaultValue)
            is Boolean -> getBoolean(key, defaultValue)
            is Float -> getFloat(key, defaultValue)
            is MutableSet<*> -> getStringSet(
                key, defaultValue
                    .filterIsInstance<String>()
                    .toMutableSet()
            )
            else -> throw NotImplementedError()
        } ?: defaultValue
    }
}