package com.datadog.android.bridge.internal

import android.content.Context
import com.datadog.android.core.configuration.Configuration
import com.datadog.android.core.configuration.Credentials
import com.datadog.android.privacy.TrackingConsent
import com.datadog.android.rum.RumMonitor

internal interface DatadogWrapper {

    fun setVerbosity(level: Int)

    fun initialize(
        context: Context,
        credentials: Credentials,
        configuration: Configuration,
        consent: TrackingConsent
    )

    fun setUserInfo(
        id: String?,
        name: String?,
        email: String?,
        extraInfo: Map<String, Any?>
    )

    fun registerRumMonitor(rumMonitor: RumMonitor)

    fun addRumGlobalAttributes(attributes: Map<String, Any?>)
}
