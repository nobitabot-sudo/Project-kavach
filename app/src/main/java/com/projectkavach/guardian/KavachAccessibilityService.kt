package com.projectkavach.guardian

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.*

class KavachAccessibilityService : AccessibilityService() {

    private lateinit var overlayManager: ScamOverlayManager
    private lateinit var engine: LocalInferenceEngine
    private lateinit var dbHelper: ScamDatabaseHelper
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onServiceConnected() {
        overlayManager = ScamOverlayManager(this)
        engine = LocalInferenceEngine(this)
        dbHelper = ScamDatabaseHelper(this)
        overlayManager.showShieldActive()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val text = event.text.toString()
        serviceScope.launch {
            val result = withContext(Dispatchers.Default) {
                engine.analyzeForScam(text)
            }
            if (result.isThreat) {
                dbHelper.insertThreatAlert(result.scamType, System.currentTimeMillis().toString())
                overlayManager.showCriticalAlert(result.scamType)
            }
        }
    }

    override fun onInterrupt() {
        overlayManager.cleanup()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
