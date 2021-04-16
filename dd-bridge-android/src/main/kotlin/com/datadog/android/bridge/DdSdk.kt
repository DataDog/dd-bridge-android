/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.bridge

import android.content.Context

/**
 * The entry point to initialize Datadog's features.
 */
interface DdSdk {

    /**
     * Initializes Datadog's features.
     */
    fun initialize(configuration: DdSdkConfiguration): Unit

    /**
     * Sets the global context (set of attributes) attached with all future Logs, Spans and RUM events.
     */
    fun setAttributes(attributes: Map<String, Any?>): Unit

    /**
     * Set the user information.
     */
    fun setUser(user: Map<String, Any?>): Unit

    /**
     * Set the tracking consent regarding the data collection.
     */
    fun setTrackingConsent(trackingConsent: String): Unit

}
