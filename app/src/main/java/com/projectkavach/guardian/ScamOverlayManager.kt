package com.projectkavach.guardian
import android.content.Context

class ScamOverlayManager(private val context: Context) {
    fun showShieldActive() {}
    fun showScamAlert(scamType: String) {}
    fun showCriticalAlert(scamType: String) {}
    fun cleanup() {}
}
