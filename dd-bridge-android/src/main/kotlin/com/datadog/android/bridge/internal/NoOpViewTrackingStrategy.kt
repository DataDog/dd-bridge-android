package com.datadog.android.bridge.internal

import android.content.Context
import com.datadog.android.rum.tracking.ViewTrackingStrategy

/**
 * No-op implementation of the [ViewTrackingStrategy].
 */
object NoOpViewTrackingStrategy : ViewTrackingStrategy {
    override fun register(context: Context) {
        // No-op
    }

    override fun unregister(context: Context?) {
        // No-op
    }
}
