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
import com.datadog.android.rum.RumMonitor
import com.datadog.android.tracing.AndroidTracer
import io.opentracing.util.GlobalTracer
import java.util.Locale

internal class BridgeSdk(
    context: Context,
    private val datadog: DatadogWrapper
) : DdSdk {

    internal val appContext: Context = context.applicationContext

    // region DdSdk

    override fun initialize(configuration: DdSdkConfiguration) {
        val credentials = buildCredentials(configuration)
        val nativeConfiguration = buildConfiguration(configuration)
        val trackingConsent = buildTrackingConsent(configuration.trackingConsent)

        datadog.setVerbosity(Log.VERBOSE)
        datadog.initialize(appContext, credentials, nativeConfiguration, trackingConsent)

        datadog.registerRumMonitor(RumMonitor.Builder().build())

        if (configuration.manualTracingEnabled == true) {
            GlobalTracer.registerIfAbsent(AndroidTracer.Builder().build())
        }
    }

    override fun setUser(user: Map<String, Any?>) {
        val extraInfo = user.toMutableMap()
        val id = extraInfo.remove("id")?.toString()
        val name = extraInfo.remove("name")?.toString()
        val email = extraInfo.remove("email")?.toString()
        datadog.setUserInfo(id, name, email, extraInfo)
    }

    override fun setAttributes(attributes: Map<String, Any?>) {
        datadog.addRumGlobalAttributes(attributes)
    }

    override fun setTrackingConsent(trackingConsent: String) {
        datadog.setTrackingConsent(buildTrackingConsent(trackingConsent))
    }

    // endregion

    // region Internal

    private fun buildConfiguration(configuration: DdSdkConfiguration): Configuration {
        val configBuilder = Configuration.Builder(
            logsEnabled = true,
            tracesEnabled = configuration.manualTracingEnabled ?: false,
            crashReportsEnabled = configuration.nativeCrashReportEnabled ?: false,
            rumEnabled = true
        )
            .setAdditionalConfiguration(
                configuration.additionalConfig
                    ?.filterValues { it != null }
                    ?.mapValues { it.value!! } ?: emptyMap()
            )
        if (configuration.sampleRate != null) {
            configBuilder.sampleRumSessions(configuration.sampleRate.toFloat())
        }
        if (configuration.site.equals("US", ignoreCase = true)) {
            configBuilder.useUSEndpoints()
        } else if (configuration.site.equals("EU", ignoreCase = true)) {
            configBuilder.useEUEndpoints()
        } else if (configuration.site.equals("GOV", ignoreCase = true)) {
            configBuilder.useGovEndpoints()
        }
        return configBuilder.build()
    }

    private fun buildCredentials(configuration: DdSdkConfiguration): Credentials {
        return Credentials(
            clientToken = configuration.clientToken,
            envName = configuration.env,
            rumApplicationId = configuration.applicationId,
            variant = ""
        )
    }

    internal fun buildTrackingConsent(trackingConsent: String?): TrackingConsent {
        return when (trackingConsent?.toLowerCase(Locale.US)) {
            "pending" -> TrackingConsent.PENDING
            "granted" -> TrackingConsent.GRANTED
            "not_granted" -> TrackingConsent.NOT_GRANTED
            else -> {
                Log.w(
                    BridgeSdk::class.java.canonicalName,
                    "Unknown consent given: $trackingConsent, " +
                        "using ${TrackingConsent.PENDING} as default"
                )
                TrackingConsent.PENDING
            }
        }
    }

    // endregion
}
