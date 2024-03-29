/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.bridge.internal

import com.datadog.android.bridge.DdLogs
import com.datadog.android.log.Logger
import com.nhaarman.mockitokotlin2.verify
import fr.xgouchet.elmyr.annotation.AdvancedForgery
import fr.xgouchet.elmyr.annotation.MapForgery
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.annotation.StringForgeryType
import fr.xgouchet.elmyr.junit5.ForgeExtension
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
internal class BridgeLogsTest {

    lateinit var testedLogs: DdLogs

    @Mock
    lateinit var mockLogger: Logger

    @StringForgery
    lateinit var fakeMessage: String

    @MapForgery(
        key = AdvancedForgery(string = [StringForgery()]),
        value = AdvancedForgery(string = [StringForgery(StringForgeryType.HEXADECIMAL)])
    )
    lateinit var fakeContext: Map<String, String>

    @MapForgery(
        key = AdvancedForgery(string = [StringForgery()]),
        value = AdvancedForgery(string = [StringForgery(StringForgeryType.HEXADECIMAL)])
    )
    lateinit var fakeGlobalState: Map<String, String>

    @BeforeEach
    fun `set up`() {
        testedLogs = BridgeLogs(mockLogger)
    }

    @AfterEach
    fun `tear down`() {
        GlobalState.globalAttributes.clear()
    }

    @Test
    fun `M forward debug log W debug()`() {
        // When
        testedLogs.debug(fakeMessage, fakeContext)

        // Then
        verify(mockLogger).d(fakeMessage, attributes = fakeContext)
    }

    @Test
    fun `M forward info log W info()`() {
        // When
        testedLogs.info(fakeMessage, fakeContext)

        // Then
        verify(mockLogger).i(fakeMessage, attributes = fakeContext)
    }

    @Test
    fun `M forward warning log W warn()`() {
        // When
        testedLogs.warn(fakeMessage, fakeContext)

        // Then
        verify(mockLogger).w(fakeMessage, attributes = fakeContext)
    }

    @Test
    fun `M forward error log W error()`() {
        // When
        testedLogs.error(fakeMessage, fakeContext)

        // Then
        verify(mockLogger).e(fakeMessage, attributes = fakeContext)
    }

    @Test
    fun `M forward debug log with global state W debug()`() {
        // Given
        fakeGlobalState.forEach { (k, v) ->
            GlobalState.globalAttributes[k] = v
        }
        val expectedAttributes = fakeContext + fakeGlobalState

        // When
        testedLogs.debug(fakeMessage, fakeContext)

        // Then
        verify(mockLogger).d(fakeMessage, attributes = expectedAttributes)
    }

    @Test
    fun `M forward info log with global state W info()`() {
        // Given
        fakeGlobalState.forEach { (k, v) ->
            GlobalState.globalAttributes[k] = v
        }
        val expectedAttributes = fakeContext + fakeGlobalState

        // When
        testedLogs.info(fakeMessage, fakeContext)

        // Then
        verify(mockLogger).i(fakeMessage, attributes = expectedAttributes)
    }

    @Test
    fun `M forward warning log with global state W warn()`() {
        // Given
        fakeGlobalState.forEach { (k, v) ->
            GlobalState.globalAttributes[k] = v
        }
        val expectedAttributes = fakeContext + fakeGlobalState

        // When
        testedLogs.warn(fakeMessage, fakeContext)

        // Then
        verify(mockLogger).w(fakeMessage, attributes = expectedAttributes)
    }

    @Test
    fun `M forward error log with global state W error()`() {
        // Given
        fakeGlobalState.forEach { (k, v) ->
            GlobalState.globalAttributes[k] = v
        }
        val expectedAttributes = fakeContext + fakeGlobalState

        // When
        testedLogs.error(fakeMessage, fakeContext)

        // Then
        verify(mockLogger).e(fakeMessage, attributes = expectedAttributes)
    }
}
