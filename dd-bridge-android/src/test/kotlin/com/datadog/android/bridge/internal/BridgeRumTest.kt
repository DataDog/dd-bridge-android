/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.bridge.internal

import com.datadog.android.bridge.DdRum
import com.datadog.android.rum.GlobalRum
import com.datadog.android.rum.RumActionType
import com.datadog.android.rum.RumAttributes
import com.datadog.android.rum.RumErrorSource
import com.datadog.android.rum.RumMonitor
import com.datadog.android.rum.RumResourceKind
import com.datadog.tools.unit.forge.BaseConfigurator
import com.datadog.tools.unit.getStaticValue
import com.datadog.tools.unit.setStaticValue
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.annotation.Forgery
import fr.xgouchet.elmyr.annotation.IntForgery
import fr.xgouchet.elmyr.annotation.LongForgery
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.annotation.StringForgeryType
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
@ForgeConfiguration(BaseConfigurator::class)
internal class BridgeRumTest {

    // TODO: 14/12/2020 RUMM-925 Add the relevant unit tests after merging the feature/bridge branch

    lateinit var testedDdRum: DdRum

    @Mock
    lateinit var mockRumMonitor: RumMonitor

    lateinit var fakeContext: Map<String, Any?>

    @LongForgery(1000000000000, 2000000000000)
    var fakeTimestamp: Long = 0L

    @BeforeEach
    fun `set up`(forge: Forge) {
        GlobalRum.registerIfAbsent(mockRumMonitor)

        fakeContext = forge.aMap {
            anAlphabeticalString() to aNullable {
                anElementFrom(
                    anHexadecimalString(),
                    anInt(),
                    aLong(),
                    getForgery<Date>(),
                    getForgery<Throwable>()
                )
            }
        }

        testedDdRum = BridgeRum()
    }

    @AfterEach
    fun `tear down`() {
        GlobalRum.javaClass.setStaticValue("monitor", mock<RumMonitor>())
        GlobalRum.javaClass.getStaticValue<GlobalRum, AtomicBoolean>("isRegistered").set(false)
    }

    @Test
    fun `M call startView W startView()`(
        @StringForgery key: String,
        @StringForgery name: String
    ) {
        // Given
        val updatedContext = fakeContext + (RumAttributes.INTERNAL_TIMESTAMP to fakeTimestamp)

        // When
        testedDdRum.startView(key, name, fakeContext, fakeTimestamp)

        // Then
        verify(mockRumMonitor).startView(key, name, updatedContext)
    }

    @Test
    fun `M call stopView W stopView()`(
        @StringForgery key: String
    ) {
        // Given
        val updatedContext = fakeContext + (RumAttributes.INTERNAL_TIMESTAMP to fakeTimestamp)

        // When
        testedDdRum.stopView(key, fakeContext, fakeTimestamp)

        // Then
        verify(mockRumMonitor).stopView(key, updatedContext)
    }

    @Test
    fun `M call addAction W addAction()`(
        @StringForgery name: String,
        @Forgery type: RumActionType
    ) {
        // Given
        val updatedContext = fakeContext + (RumAttributes.INTERNAL_TIMESTAMP to fakeTimestamp)

        // When
        testedDdRum.addAction(type.name, name, fakeContext, fakeTimestamp)

        // Then
        verify(mockRumMonitor).addUserAction(type, name, updatedContext)
    }

    @Test
    fun `M call addAction W addAction() with invalid type`(
        @StringForgery name: String,
        @StringForgery(StringForgeryType.HEXADECIMAL) type: String
    ) {
        // Given
        val updatedContext = fakeContext + (RumAttributes.INTERNAL_TIMESTAMP to fakeTimestamp)

        // When
        testedDdRum.addAction(type, name, fakeContext, fakeTimestamp)

        // Then
        verify(mockRumMonitor).addUserAction(RumActionType.CUSTOM, name, updatedContext)
    }

    @Test
    fun `M call startAction W startAction()`(
        @Forgery type: RumActionType,
        @StringForgery name: String
    ) {
        // Given
        val updatedContext = fakeContext + (RumAttributes.INTERNAL_TIMESTAMP to fakeTimestamp)

        // When
        testedDdRum.startAction(type.name, name, fakeContext, fakeTimestamp)

        // Then
        verify(mockRumMonitor).startUserAction(type, name, updatedContext)
    }

    @Test
    fun `M call startAction W startAction() with invalid type`(
        @StringForgery name: String,
        @StringForgery(StringForgeryType.HEXADECIMAL) type: String
    ) {
        // Given
        val updatedContext = fakeContext + (RumAttributes.INTERNAL_TIMESTAMP to fakeTimestamp)

        // When
        testedDdRum.startAction(type, name, fakeContext, fakeTimestamp)

        // Then
        verify(mockRumMonitor).startUserAction(RumActionType.CUSTOM, name, updatedContext)
    }

    @Test
    fun `M call stopAction W stopAction()`(
        @Forgery type: RumActionType,
        @StringForgery name: String
    ) {
        // Given
        val updatedContext = fakeContext + (RumAttributes.INTERNAL_TIMESTAMP to fakeTimestamp)

        // When
        testedDdRum.stopAction(type.name, name, fakeContext, fakeTimestamp)

        // Then
        verify(mockRumMonitor).stopUserAction(type, name, updatedContext)
    }

    @Test
    fun `M call stopAction W stopAction() with invalid type`(
        @StringForgery name: String,
        @StringForgery(StringForgeryType.HEXADECIMAL) type: String
    ) {
        // Given
        val updatedContext = fakeContext + (RumAttributes.INTERNAL_TIMESTAMP to fakeTimestamp)

        // When
        testedDdRum.stopAction(type, name, fakeContext, fakeTimestamp)

        // Then
        verify(mockRumMonitor).stopUserAction(RumActionType.CUSTOM, name, updatedContext)
    }

    @Test
    fun `M call startResource W startResource()`(
        @StringForgery key: String,
        @StringForgery(regex = "GET|POST|DELETE") method: String,
        @StringForgery(regex = "http(s?)://[a-z]+\\.com/\\w+") url: String
    ) {
        // Given
        val updatedContext = fakeContext + (RumAttributes.INTERNAL_TIMESTAMP to fakeTimestamp)

        // When
        testedDdRum.startResource(key, method, url, fakeContext, fakeTimestamp)

        // Then
        verify(mockRumMonitor).startResource(key, method, url, updatedContext)
    }

    @Test
    fun `M call stopResource W stopResource()`(
        @StringForgery key: String,
        @IntForgery(200, 600) statusCode: Int,
        @LongForgery(min = 0L) resourceSize: Long,
        @Forgery kind: RumResourceKind
    ) {
        // Given
        val updatedContext = fakeContext + (RumAttributes.INTERNAL_TIMESTAMP to fakeTimestamp)

        // When
        testedDdRum.stopResource(
            key,
            statusCode.toLong(),
            kind.toString(),
            resourceSize,
            fakeContext,
            fakeTimestamp
        )

        // Then
        verify(mockRumMonitor).stopResource(key, statusCode, resourceSize, kind, updatedContext)
    }

    @Test
    fun `M call stopResource W stopResource() with invalid kind`(
        @StringForgery key: String,
        @IntForgery(200, 600) statusCode: Int,
        @LongForgery(min = 0L) resourceSize: Long,
        @StringForgery(StringForgeryType.HEXADECIMAL) kind: String
    ) {
        // Given
        val updatedContext = fakeContext + (RumAttributes.INTERNAL_TIMESTAMP to fakeTimestamp)

        // When
        testedDdRum.stopResource(
            key,
            statusCode.toLong(),
            kind,
            resourceSize,
            fakeContext,
            fakeTimestamp
        )

        // Then
        verify(mockRumMonitor).stopResource(
            key,
            statusCode,
            resourceSize,
            RumResourceKind.UNKNOWN,
            updatedContext
        )
    }

    @Test
    fun `M call stopResource W stopResource() with missing resource size`(
        @StringForgery key: String,
        @IntForgery(200, 600) statusCode: Int,
        @StringForgery(StringForgeryType.HEXADECIMAL) kind: String
    ) {
        // Given
        val updatedContext = fakeContext + (RumAttributes.INTERNAL_TIMESTAMP to fakeTimestamp)

        // When
        testedDdRum.stopResource(
            key,
            statusCode.toLong(),
            kind,
            -1,
            fakeContext,
            fakeTimestamp
        )

        // Then
        verify(mockRumMonitor).stopResource(
            key,
            statusCode,
            null,
            RumResourceKind.UNKNOWN,
            updatedContext
        )
    }

    @Test
    fun `M call addError W addError()`(
        @StringForgery message: String,
        @Forgery source: RumErrorSource,
        @StringForgery stackTrace: String
    ) {
        // Given
        val updatedContext = fakeContext + (RumAttributes.INTERNAL_TIMESTAMP to fakeTimestamp)

        // When
        testedDdRum.addError(message, source.name, stackTrace, fakeContext, fakeTimestamp)

        // Then
        verify(mockRumMonitor).addErrorWithStacktrace(message, source, stackTrace, updatedContext)
    }

    @Test
    fun `M call addError W addError() with invalid source`(
        @StringForgery message: String,
        @StringForgery(StringForgeryType.HEXADECIMAL) source: String,
        @StringForgery stackTrace: String
    ) {
        // Given
        val updatedContext = fakeContext + (RumAttributes.INTERNAL_TIMESTAMP to fakeTimestamp)

        // When
        testedDdRum.addError(message, source, stackTrace, fakeContext, fakeTimestamp)

        // Then
        verify(mockRumMonitor).addErrorWithStacktrace(
            message,
            RumErrorSource.SOURCE,
            stackTrace,
            updatedContext
        )
    }

    @Test
    fun `M call addTiming W addTiming()`(@StringForgery timing: String) {

        // When
        testedDdRum.addTiming(timing)

        // Then
        verify(mockRumMonitor).addTiming(timing)
    }
}
