package com.tomfricks.hook.utils

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager

object PermissionUtils {
    
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
