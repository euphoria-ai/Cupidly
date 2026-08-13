package com.tomfricks.hook.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat

object PermissionUtils {

    /**
     * The permission that lets Hook read the screenshot the user just took.
     *
     * Android 13 split media out of storage; below that, images come with the
     * broad storage permission.
     */
    val photoPermission: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    /** True once Hook can read screenshots — without it, detection sees nothing. */
    fun hasPhotoAccess(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, photoPermission) ==
            PackageManager.PERMISSION_GRANTED

    /** Hook's own entry in the system app-settings screen. */
    fun openAppSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android.net.Uri.fromParts("package", context.packageName, null)
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent)
    }

    /**
     * Check if Hook keyboard is enabled in system settings
     */
    fun isKeyboardEnabled(context: Context): Boolean {
        val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val enabledInputMethods = inputMethodManager.enabledInputMethodList
        val packageName = context.packageName
        
        return enabledInputMethods.any { it.packageName == packageName }
    }
    
    /**
     * Check if Hook keyboard is currently selected
     */
    fun isKeyboardSelected(context: Context): Boolean {
        val defaultKeyboard = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD
        )
        return defaultKeyboard?.contains(context.packageName) == true
    }
    
    /**
     * Open keyboard settings
     */
    fun openKeyboardSettings(context: Context) {
        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    
    /**
     * Show input method picker
     */
    fun showKeyboardPicker(context: Context) {
        val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showInputMethodPicker()
    }
}
