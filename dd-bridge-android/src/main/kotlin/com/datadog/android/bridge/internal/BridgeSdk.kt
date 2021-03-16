/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.bridge.internal

import android.content.Context
import android.util.Log
import com.datadog.android.bridge.DdSdk
import com.datadog.android.bridge.DdSdkConfiguration
import com.datadog.android.core.configuration.Configuration
import com.datadog.android.core.configuration.Credentials
import com.datadog.android.privacy.TrackingConsent
import com.datadog.android.rum.GlobalRum
import com.datadog.android.rum.RumMonitor

internal class BridgeSdk(
    context: Context,
    private val datadog: DatadogWrapper
) : DdSdk {

    internal val appContext: Context = context.applicationContext

    // region DdSdk

    override fun initialize(configuration: DdSdkConfiguration) {
        val credentials = buildCredentials(configuration)
        val nativeConfiguration = buildConfiguration(configuration)

        datadog.setVerbosity(Log.VERBOSE)
        datadog.initialize(appContext, credentials, nativeConfiguration, TrackingConsent.GRANTED)

        GlobalRum.registerIfAbsent(RumMonitor.Builder().build())
    }

    override fun setUser(user: Map<String, Any?>) {
        val extraInfo = user.toMutableMap()
        val id = extraInfo.remove("id")?.toString()
        val name = extraInfo.remove("name")?.toString()
        val email = extraInfo.remove("email")?.toString()
        datadog.setUserInfo(id, name, email, extraInfo)
    }

    override fun setAttributes(attributes: Map<String, Any?>) {
        TODO("Not yet implemented")
    }

    // endregion

    // region Internal

    private fun buildConfiguration(configuration: DdSdkConfiguration): Configuration {
        return Configuration.Builder(
            logsEnabled = true,
            tracesEnabled = true,
            crashReportsEnabled = configuration.nativeCrashReportEnabled ?: false,
            rumEnabled = true
        )
            .sampleRumSessions(configuration.sampleRate?.toFloat() ?: 100f)
            .build()
    }

    private fun buildCredentials(configuration: DdSdkConfiguration): Credentials {
        return Credentials(
            clientToken = configuration.clientToken,
            envName = configuration.env,
            rumApplicationId = configuration.applicationId,
            variant = ""
        )
    }

    // endregion
}
