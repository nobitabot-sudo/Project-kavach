package com.projectkavach.guardian

data class ThreatAnalysisResult(
    val isThreat: Boolean = false,
    val scamType: String = "None",
    val confidence: Int = 0
)

class LocalInferenceEngine(private val context: android.content.Context) {
    fun analyzeForScam(text: String): ThreatAnalysisResult {
        if (text.contains("cbi", ignoreCase = true) || text.contains("police", ignoreCase = true)) {
            return ThreatAnalysisResult(true, "Fake Official", 95)
        }
        return ThreatAnalysisResult(false, "Safe", 0)
    }
}
