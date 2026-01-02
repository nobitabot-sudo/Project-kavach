package com.projectkavach.guardian
import android.content.Context

class ScamOverlayManager(private val context: Context) {
    fun showShieldActive() {}
    fun cleanup() {}
    
    // Simplified to accept any number of arguments to prevent build errors
    fun showScamAlert(vararg args: Any?) {}
    fun showCriticalAlert(vararg args: Any?) {}
}
