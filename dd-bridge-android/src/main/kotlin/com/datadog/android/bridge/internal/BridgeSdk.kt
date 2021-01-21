/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.bridge.internal

import android.content.Context
import android.util.Log
import com.datadog.android.Datadog as DatadogSDK
import com.datadog.android.bridge.DdSdk
import com.datadog.android.bridge.DdSdkConfiguration
import com.datadog.android.core.configuration.Configuration
import com.datadog.android.core.configuration.Credentials
import com.datadog.android.privacy.TrackingConsent
import com.datadog.android.rum.GlobalRum
import com.datadog.android.rum.RumMonitor

internal class BridgeSdk(context: Context) : DdSdk {

    internal val appContext: Context = context.applicationContext

    override fun initialize(configuration: DdSdkConfiguration) {
        val credentials = Credentials(
            clientToken = configuration.clientToken,
            envName = configuration.env,
            rumApplicationId = configuration.applicationId,
            variant = ""
        )
        val configBuilder = Configuration.Builder(
            logsEnabled = true,
            tracesEnabled = true,
            crashReportsEnabled = true,
            rumEnabled = true
        )

        DatadogSDK.setVerbosity(Log.VERBOSE)
        DatadogSDK.initialize(
            appContext,
            credentials,
            configBuilder.build(),
            TrackingConsent.GRANTED
        )
        GlobalRum.registerIfAbsent(RumMonitor.Builder().build())
    }
}
