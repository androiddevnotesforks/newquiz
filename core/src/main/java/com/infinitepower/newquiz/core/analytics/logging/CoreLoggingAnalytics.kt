package com.infinitepower.newquiz.core.analytics.logging

interface CoreLoggingAnalytics {
    fun logScreenView(
        screenName: String,
        screenClass: String? = null
    )
}