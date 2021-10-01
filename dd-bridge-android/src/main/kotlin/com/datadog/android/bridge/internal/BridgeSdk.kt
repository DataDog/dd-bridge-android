/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.bridge.internal

import android.content.Context
import android.util.Log
import com.datadog.android.DatadogSite
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
        attributes.forEach { (k, v) ->
            GlobalState.addAttribute(k, v)
        }
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

        configBuilder.useSite(buildSite(configuration.site))

        val viewTracking = configuration.additionalConfig?.get(DD_NATIVE_VIEW_TRACKING) as? Boolean
        if (viewTracking == true) {
            // Use sensible default
            configBuilder.useViewTrackingStrategy(ActivityViewTrackingStrategy(false))
        } else {
            configBuilder.useViewTrackingStrategy(NoOpViewTrackingStrategy)
        }

        val longTask = configuration.additionalConfig?.get(DD_LONG_TASK_THRESHOLD) as? Long
        if (longTask != null) {
            configBuilder.trackLongTasks(longTask)
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

    private fun buildSite(site: String?): DatadogSite {
        val siteLower = site?.toLowerCase(Locale.US)
        return when (siteLower) {
            "us1", "us" -> DatadogSite.US1
            "eu1", "eu" -> DatadogSite.EU1
            "us3" -> DatadogSite.US3
            "us5" -> DatadogSite.US5
            "us1_fed", "gov" -> DatadogSite.US1_FED
            else -> DatadogSite.US1
        }
    }

    // endregion

    companion object {
        internal const val DD_NATIVE_VIEW_TRACKING = "_dd.native_view_tracking"
        internal const val DD_SDK_VERBOSITY = "_dd.sdk_verbosity"
        internal const val DD_SERVICE_NAME = "_dd.service_name"
        internal const val DD_LONG_TASK_THRESHOLD = "_dd.long_task.threshold"
    }
}
