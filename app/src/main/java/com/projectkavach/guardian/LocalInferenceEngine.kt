package com.projectkavach.guardian
import android.content.Context

class LocalInferenceEngine(context: Context) {
    fun analyzeText(text: String): Boolean {
        val keywords = listOf("cbi", "police", "arrest", "bank", "otp", "video call")
        return keywords.any { text.contains(it, ignoreCase = true) }
    }
}
