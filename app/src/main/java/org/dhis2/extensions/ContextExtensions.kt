package org.dhis2.extensions

import android.content.Context
import android.content.Context.MODE_PRIVATE
import org.dhis2.utils.Constants.SHARE_PREFS

inline fun <reified T> Context.getDhisPreferences(key: String, defaultValue: T): T {
    val prefs = getSharedPreferences(SHARE_PREFS, MODE_PRIVATE)
    return when(T::class) {
        Boolean::class -> prefs.getBoolean(key, defaultValue as Boolean) as T
        Float::class -> prefs.getFloat(key, defaultValue as Float) as T
        Int::class -> prefs.getInt(key, defaultValue as Int) as T
        Long::class -> prefs.getLong(key, defaultValue as Long) as T
        String::class -> prefs.getString(key, defaultValue as String) as T
        else -> {
            if (defaultValue is Set<*>) {
                prefs.getStringSet(key, defaultValue as Set<String>) as T
            } else {
                setOf<String>() as T
            }
        }
    }
}

inline fun <reified T> Context.setDhisPreferences(key: String, value: T) {
    val editor = getSharedPreferences(SHARE_PREFS, MODE_PRIVATE).edit()
    when(T::class) {
        Boolean::class -> editor.putBoolean(key, value as Boolean)
        Float::class -> editor.putFloat(key, value as Float)
        Int::class -> editor.putInt(key, value as Int)
        Long::class -> editor.putLong(key, value as Long)
        String::class -> editor.putString(key, value as String)
        else -> {
            if (value is Set<*>) {
                editor.putStringSet(key, value as Set<String>)
            }
        }
    }
    editor.apply()
}