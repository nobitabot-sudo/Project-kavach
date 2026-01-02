package com.projectkavach.guardian

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.*

/**
 * Project Kavach - Accessibility Service (The 'Eyes')
 * 
 * This service monitors screen content in real-time using Android's Accessibility APIs.
 * It implements a battery-efficient "Observer Pattern" that remains dormant until
 * hot keywords or sensitive UI elements are detected.
 * 
 * PRIVACY GUARANTEE: All processing happens on-device. Zero data leaves the device.
 */
class KavachAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "KavachService"
        
        // Monitored app packages - Only process events from these apps
        private val MONITORED_PACKAGES = setOf(
            "com.whatsapp",
            "com.whatsapp.w4b",
            "org.telegram.messenger",
            "com.google.android.apps.messaging", // Google Messages
            "com.phonepe.app",
            "com.google.android.apps.nbu.paisa.user", // Google Pay
            "in.org.npci.upiapp", // BHIM
            "com.paytm",
            "net.one97.paytm",
            "com.android.chrome",
            "com.android.vending", // Play Store (for malicious APK warnings)
            "com.anydesk.anydeskandroid",
            "com.teamviewer.teamviewer.market.mobile",
            "com.rustdesk.rustdesk"
        )
        
        // Hot Keywords - Trigger AI analysis when detected
        private val HOT_KEYWORDS = setOf(
            // Financial
            "otp", "pin", "mpin", "password", "cvv", "upi",
            "bank", "account", "refund", "payment",
            
            // Authority/Fear
            "cbi", "police", "customs", "investigation", "arrest",
            "illegal", "parcel", "money laundering", "cyber crime",
            
            // Utility Threats
            "electricity", "gas", "sim", "kyc", "blocked", "suspended",
            
            // Technical
            "anydesk", "teamviewer", "rustdesk", "screen share",
            "remote", "apk", "download app",
            
            // Job/Investment Scams
            "work from home", "earn money", "like youtube", "telegram task",
            "crypto", "trading", "double your money", "guaranteed returns",
            
            // Social Engineering
            "emergency", "urgent", "immediately", "verify", "confirm identity"
        )
        
        // Screen-sharing apps that trigger CRITICAL alerts
        private val SCREEN_SHARE_APPS = setOf(
            "com.anydesk.anydeskandroid",
            "com.teamviewer.teamviewer.market.mobile",
            "com.rustdesk.rustdesk"
        )
        
        // Banking/UPI apps that require extra protection
        private val FINANCIAL_APPS = setOf(
            "com.phonepe.app",
            "com.google.android.apps.nbu.paisa.user",
            "in.org.npci.upiapp",
            "com.paytm",
            "net.one97.paytm"
        )
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private var overlayManager: ScamOverlayManager? = null
    private var inferenceEngine: LocalInferenceEngine? = null
    private var scamDatabase: ScamDatabaseHelper? = null
    
    // Battery optimization - Track last analysis time
    private var lastAnalysisTime: Long = 0
    private val MIN_ANALYSIS_INTERVAL_MS = 2000 // 2 seconds cooldown
    
    // Current context tracking
    private var currentPackage: String = ""
    private var isScreenShareActive = false
    private var isFinancialAppActive = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Kavach Service Connected - Guardian Active")
        
        // Configure accessibility service
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                        AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
            
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            
            // Only monitor specific packages to save battery
            packageNames = MONITORED_PACKAGES.toTypedArray()
            
            notificationTimeout = 100
        }
        serviceInfo = info
        
        // Initialize components
        overlayManager = ScamOverlayManager(this)
        inferenceEngine = LocalInferenceEngine(this)
        scamDatabase = ScamDatabaseHelper(this)
        
        // Show shield active indicator
        overlayManager?.showShieldActive()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        
        val packageName = event.packageName?.toString() ?: return
        
        // Ignore events from non-monitored packages (battery optimization)
        if (!MONITORED_PACKAGES.contains(packageName)) {
            return
        }
        
        // Update current context
        currentPackage = packageName
        isScreenShareActive = SCREEN_SHARE_APPS.contains(packageName)
        isFinancialAppActive = FINANCIAL_APPS.contains(packageName)
        
        // CRITICAL: Screen-share + Financial app = Immediate alert
        if (isScreenShareActive && detectFinancialAppInBackground()) {
            triggerCriticalAlert(
                title = "CRITICAL SECURITY THREAT",
                message = "Screen-sharing app detected while banking apps are running. This is a common scam technique.",
                reasoning = "Scammers use remote access apps to see your OTPs and passwords. Close the screen-sharing app immediately.",
                threatLevel = ThreatLevel.CRITICAL
            )
            return
        }
        
        // Extract text from accessibility event (Battery-efficient approach)
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                val currentTime = System.currentTimeMillis()
                
                // Rate limiting - Don't analyze too frequently
                if (currentTime - lastAnalysisTime < MIN_ANALYSIS_INTERVAL_MS) {
                    return
                }
                
                // Extract screen text
                val screenText = extractTextFromEvent(event)
                
                // PHASE 1: Keyword Scanning (Low-power, fast check)
                val detectedKeywords = detectHotKeywords(screenText)
                
                if (detectedKeywords.isNotEmpty()) {
                    // Keywords detected - Wake up AI engine
                    lastAnalysisTime = currentTime
                    
                    Log.d(TAG, "Hot keywords detected: $detectedKeywords")
                    
                    // PHASE 2: AI Inference (Only when triggered)
                    performIntelligentAnalysis(screenText, detectedKeywords, packageName)
                }
            }
        }
    }

    /**
     * Extract all visible text from the accessibility event
     */
    private fun extractTextFromEvent(event: AccessibilityEvent): String {
        val textBuilder = StringBuilder()
        
        // Extract text from the event itself
        event.text?.forEach { text ->
            textBuilder.append(text).append(" ")
        }
        
        // Extract text from the root node (more comprehensive)
        rootInActiveWindow?.let { root ->
            extractTextFromNode(root, textBuilder)
            root.recycle()
        }
        
        return textBuilder.toString().lowercase()
    }

    /**
     * Recursively extract text from accessibility node tree
     */
    private fun extractTextFromNode(node: AccessibilityNodeInfo, builder: StringBuilder) {
        // Get text from current node
        node.text?.let { builder.append(it).append(" ") }
        node.contentDescription?.let { builder.append(it).append(" ") }
        
        // Check for sensitive fields (password, OTP)
        if (node.isPassword || isOTPField(node)) {
            // Ghost-masking logic will be handled separately
            builder.append("[SENSITIVE_FIELD] ")
        }
        
        // Recursively process child nodes
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                extractTextFromNode(child, builder)
                child.recycle()
            }
        }
    }

    /**
     * Check if a node is an OTP input field
     */
    private fun isOTPField(node: AccessibilityNodeInfo): Boolean {
        val hint = node.hintText?.toString()?.lowercase() ?: ""
        val text = node.text?.toString()?.lowercase() ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
        
        val otpIndicators = listOf("otp", "verification code", "6 digit", "4 digit")
        
        return otpIndicators.any { 
            hint.contains(it) || text.contains(it) || contentDesc.contains(it)
        }
    }

    /**
     * PHASE 1: Fast keyword detection (Battery-efficient)
     */
    private fun detectHotKeywords(text: String): Set<String> {
        val lowerText = text.lowercase()
        return HOT_KEYWORDS.filter { keyword ->
            lowerText.contains(keyword)
        }.toSet()
    }

    /**
     * PHASE 2: Intelligent AI analysis (Only when keywords detected)
     */
    private fun performIntelligentAnalysis(
        screenText: String,
        keywords: Set<String>,
        packageName: String
    ) {
        serviceScope.launch {
            try {
                // Analyze using local AI (Gemini Nano/AICore)
                val analysisResult = inferenceEngine?.analyzeForScam(
                    text = screenText,
                    keywords = keywords,
                    packageName = packageName,
                    context = buildContextData()
                )
                
                analysisResult?.let { result ->
                    if (result.isThreat && result.confidence > 0.7) {
                        // Store in local database
                        scamDatabase?.insertThreatAlert(result)
                        
                        // Show overlay warning
                        withContext(Dispatchers.Main) {
                            showScamWarning(result)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Analysis error: ${e.message}")
            }
        }
    }

    /**
     * Build context data for AI analysis
     */
    private fun buildContextData(): AnalysisContext {
        return AnalysisContext(
            isScreenShareActive = isScreenShareActive,
            isFinancialAppActive = isFinancialAppActive,
            currentPackage = currentPackage,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Detect if any financial app is running in background
     */
    private fun detectFinancialAppInBackground(): Boolean {
        // Check recent tasks for financial apps
        // This would use UsageStatsManager in a real implementation
        return isFinancialAppActive
    }

    /**
     * Show scam warning overlay
     */
    private fun showScamWarning(result: ThreatAnalysisResult) {
        overlayManager?.showScamAlert(
            category = result.category,
            title = result.title,
            message = result.message,
            reasoning = result.reasoning,
            psychology = result.psychology,
            preventionTips = result.preventionTips,
            threatLevel = result.threatLevel,
            confidence = result.confidence
        )
    }

    /**
     * Trigger critical alert (immediate, full-screen)
     */
    private fun triggerCriticalAlert(
        title: String,
        message: String,
        reasoning: String,
        threatLevel: ThreatLevel
    ) {
        mainHandler.post {
            overlayManager?.showCriticalAlert(title, message, reasoning, threatLevel)
        }
        
        // Store in database
        serviceScope.launch {
            scamDatabase?.insertThreatAlert(
                ThreatAnalysisResult(
                    category = "technical-access",
                    title = title,
                    message = message,
                    reasoning = reasoning,
                    psychology = "Scammers use screen-sharing to bypass security",
                    preventionTips = listOf("Close screen-sharing app immediately"),
                    threatLevel = threatLevel,
                    confidence = 1.0,
                    isThreat = true,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Kavach Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        overlayManager?.cleanup()
        scamDatabase?.close()
        Log.d(TAG, "Kavach Service Destroyed")
    }
}

/**
 * Data classes for threat analysis
 */
data class ThreatAnalysisResult(
    val category: String,
    val title: String,
    val message: String,
    val reasoning: String,
    val psychology: String,
    val preventionTips: List<String>,
    val threatLevel: ThreatLevel,
    val confidence: Double,
    val isThreat: Boolean,
    val timestamp: Long
)

data class AnalysisContext(
    val isScreenShareActive: Boolean,
    val isFinancialAppActive: Boolean,
    val currentPackage: String,
    val timestamp: Long
)

enum class ThreatLevel {
    CRITICAL,  // Red, full-screen alert
    HIGH,      // Yellow, prominent warning
    MEDIUM,    // Cyan, informational notice
    LOW        // Minimal notification
}
