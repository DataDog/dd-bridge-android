package com.datadog.android.bridge.internal

import android.content.Context
import com.datadog.android.Datadog
import com.datadog.android.core.configuration.Configuration
import com.datadog.android.core.configuration.Credentials
import com.datadog.android.privacy.TrackingConsent
import com.datadog.android.rum.GlobalRum
import com.datadog.android.rum.RumMonitor

internal class DatadogSDKWrapper : DatadogWrapper {

    override fun setVerbosity(level: Int) {
        Datadog.setVerbosity(level)
    }

    override fun initialize(
        context: Context,
        credentials: Credentials,
        configuration: Configuration,
        consent: TrackingConsent
    ) {
        Datadog.initialize(context, credentials, configuration, consent)
    }

    override fun setUserInfo(
        id: String?,
        name: String?,
        email: String?,
        extraInfo: Map<String, Any?>
    ) {
        Datadog.setUserInfo(id, name, email, extraInfo)
    }

    override fun registerRumMonitor(rumMonitor: RumMonitor) {
        GlobalRum.registerIfAbsent(rumMonitor)
    }

    override fun addRumGlobalAttributes(attributes: Map<String, Any?>) {
        attributes.forEach {
            GlobalRum.addAttribute(it.key, it.value)
        }
    }
}
