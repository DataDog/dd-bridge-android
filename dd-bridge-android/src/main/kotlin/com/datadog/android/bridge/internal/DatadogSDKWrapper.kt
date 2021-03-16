package com.datadog.android.bridge.internal

import android.content.Context
import com.datadog.android.Datadog
import com.datadog.android.core.configuration.Configuration
import com.datadog.android.core.configuration.Credentials
import com.datadog.android.privacy.TrackingConsent

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
}
