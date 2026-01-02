package com.projectkavach.guardian
import android.content.Context

class LocalInferenceEngine(context: Context) {
    fun analyzeForScam(text: String): String? {
        if (text.contains("cbi", ignoreCase = true)) return "Fake Official"
        return null
    }
}
