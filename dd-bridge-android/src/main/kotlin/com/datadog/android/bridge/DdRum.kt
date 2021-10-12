/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.bridge

/**
 * The entry point to use Datadog's RUM feature.
 */
interface DdRum {

    /**
     * Start tracking a RUM View.
     */
    fun startView(key: String, name: String, context: Map<String, Any?>, timestampMs: Long): Unit

    /**
     * Stop tracking a RUM View.
     */
    fun stopView(key: String, context: Map<String, Any?>, timestampMs: Long): Unit

    /**
     * Start tracking a RUM Action.
     */
    fun startAction(type: String, name: String, context: Map<String, Any?>, timestampMs: Long): Unit

    /**
     * Stop tracking the ongoing RUM Action.
     */
    fun stopAction(context: Map<String, Any?>, timestampMs: Long): Unit

    /**
     * Add a RUM Action.
     */
    fun addAction(type: String, name: String, context: Map<String, Any?>, timestampMs: Long): Unit

    /**
     * Start tracking a RUM Resource.
     */
    fun startResource(
        key: String,
        method: String,
        url: String,
        context: Map<String, Any?>,
        timestampMs: Long
    ): Unit

    /**
     * Stop tracking a RUM Resource.
     */
    @Suppress("LongParameterList")
    fun stopResource(
        key: String,
        statusCode: Long,
        kind: String,
        size: Long,
        context: Map<String, Any?>,
        timestampMs: Long
    ): Unit

    /**
     * Add a RUM Error.
     */
    fun addError(
        message: String,
        source: String,
        stacktrace: String,
        context: Map<String, Any?>,
        timestampMs: Long
    ): Unit

    /**
     * Adds a specific timing in the active View. The timing duration will be computed as the difference between the time the View was started and the time this function was called.
     */
    fun addTiming(name: String): Unit
}
