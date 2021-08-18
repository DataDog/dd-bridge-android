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
import com.datadog.android.rum.tracking.ActivityViewTrackingStrategy
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

        configureSdkVerbosity(configuration)

        datadog.initialize(appContext, credentials, nativeConfiguration, trackingConsent)

        datadog.registerRumMonitor(RumMonitor.Builder().build())
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

    private fun configureSdkVerbosity(configuration: DdSdkConfiguration) {
        val verbosityConfig = configuration.additionalConfig?.get(DD_SDK_VERBOSITY) as? String
        val verbosity = when (verbosityConfig?.toLowerCase(Locale.US)) {
            "debug" -> Log.DEBUG
            "info" -> Log.INFO
            "warn" -> Log.WARN
            "error" -> Log.ERROR
            else -> null
        }
        if (verbosity != null) {
            datadog.setVerbosity(verbosity)
        }
    }

    private fun buildConfiguration(configuration: DdSdkConfiguration): Configuration {
        val configBuilder = Configuration.Builder(
            logsEnabled = true,
            tracesEnabled = true,
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

        val viewTracking = configuration.additionalConfig?.get(DD_NATIVE_VIEW_TRACKING) as? Boolean
        if (viewTracking == true) {
            // Use sensible default
            configBuilder.useViewTrackingStrategy(ActivityViewTrackingStrategy(false))
        } else {
            configBuilder.useViewTrackingStrategy(NoOpViewTrackingStrategy)
        }
        return configBuilder.build()
    }

    private fun buildCredentials(configuration: DdSdkConfiguration): Credentials {
        val serviceName = configuration.additionalConfig?.get(DD_SERVICE_NAME) as? String
        return Credentials(
            clientToken = configuration.clientToken,
            envName = configuration.env,
            rumApplicationId = configuration.applicationId,
            variant = "",
            serviceName = serviceName
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

    companion object {
        internal const val DD_NATIVE_VIEW_TRACKING = "_dd.native_view_tracking"
        internal const val DD_SDK_VERBOSITY = "_dd.sdk_verbosity"
        internal const val DD_SERVICE_NAME = "_dd.service_name"
    }
}
