/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.bridge

import android.content.Context
import com.datadog.android.bridge.internal.BridgeLogs
import com.datadog.android.bridge.internal.BridgeRum
import com.datadog.android.bridge.internal.BridgeSdk
import com.datadog.android.bridge.internal.BridgeTrace
import com.datadog.android.bridge.internal.DatadogSDKWrapper

/**
 * The main entry point for Bridge clients to access Bridge implementation classes.
 */
object DdBridge {

    /**
     * @param context the current application [Context]
     * @return an implementation of [DdSdk]
     */
    @JvmStatic
    fun getDdSdk(context: Context): DdSdk {
        return BridgeSdk(context, DatadogSDKWrapper())
    }

    /**
     * @param context the current application [Context]
     * @return an implementation of [DdLogs]
     */

    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun getDdLogs(context: Context): DdLogs {
        return BridgeLogs()
    }

    /**
     * @param context the current application [Context]
     * @return an implementation of [DdRum]
     */
    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun getDdRum(context: Context): DdRum {
        return BridgeRum()
    }

    /**
     * @param context the current application [Context]
     * @return an implementation of [DdTrace]
     */
    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    fun getDdTrace(context: Context): DdTrace {
        return BridgeTrace()
    }
}
