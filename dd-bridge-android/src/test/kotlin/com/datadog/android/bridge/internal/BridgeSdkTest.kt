package com.datadog.android.bridge.internal

import android.content.Context
import com.datadog.android.DatadogEndpoint
import com.datadog.android.bridge.DdSdkConfiguration
import com.datadog.android.core.configuration.BatchSize
import com.datadog.android.core.configuration.Configuration
import com.datadog.android.core.configuration.Credentials
import com.datadog.android.core.configuration.UploadFrequency
import com.datadog.android.plugin.DatadogPlugin
import com.datadog.android.privacy.TrackingConsent
import com.datadog.tools.unit.GenericAssert.Companion.assertThat
import com.datadog.tools.unit.forge.BaseConfigurator
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.same
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.xgouchet.elmyr.annotation.AdvancedForgery
import fr.xgouchet.elmyr.annotation.Forgery
import fr.xgouchet.elmyr.annotation.MapForgery
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.annotation.StringForgeryType
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions.assertThat
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
internal class BridgeSdkTest {

    lateinit var testedBridgeSdk: BridgeSdk

    @Mock
    lateinit var mockContext: Context

    @Mock
    lateinit var mockDatadog: DatadogWrapper

    @BeforeEach
    fun `set up`() {
        whenever(mockContext.applicationContext) doReturn mockContext
        testedBridgeSdk = BridgeSdk(mockContext, mockDatadog)
    }

    @Test
    fun `𝕄 initialize native SDK 𝕎 initialize() {native crash report enabled, site = null}`(
        @Forgery configuration: DdSdkConfiguration
    ) {
        // Given
        val bridgeConfiguration = configuration.copy(nativeCrashReportEnabled = true, site = null)
        val credentialCaptor = argumentCaptor<Credentials>()
        val configCaptor = argumentCaptor<Configuration>()
        val expectedRumSampleRate = bridgeConfiguration.sampleRate?.toFloat() ?: 100f

        // When
        testedBridgeSdk.initialize(bridgeConfiguration)

        // Then
        inOrder(mockDatadog) {
            verify(mockDatadog).initialize(
                same(mockContext),
                credentialCaptor.capture(),
                configCaptor.capture(),
                eq(TrackingConsent.GRANTED)
            )
            verify(mockDatadog).registerRumMonitor(any())
        }
        assertThat(configCaptor.firstValue)
            .hasField("coreConfig") {
                it.hasFieldEqualTo("needsClearTextHttp", false)
                it.hasFieldEqualTo("firstPartyHosts", emptyList<String>())
                it.hasFieldEqualTo("batchSize", BatchSize.MEDIUM)
                it.hasFieldEqualTo("uploadFrequency", UploadFrequency.AVERAGE)
            }
            .hasField("logsConfig") {
                it.hasFieldEqualTo("endpointUrl", DatadogEndpoint.LOGS_US)
                it.hasFieldEqualTo("plugins", emptyList<DatadogPlugin>())
            }
            .hasField("tracesConfig") {
                it.hasFieldEqualTo("endpointUrl", DatadogEndpoint.TRACES_US)
                it.hasFieldEqualTo("plugins", emptyList<DatadogPlugin>())
            }
            .hasField("crashReportConfig") {
                it.hasFieldEqualTo("endpointUrl", DatadogEndpoint.LOGS_US)
                it.hasFieldEqualTo("plugins", emptyList<DatadogPlugin>())
            }
            .hasField("rumConfig") {
                it.hasFieldEqualTo("endpointUrl", DatadogEndpoint.RUM_US)
                it.hasFieldEqualTo("plugins", emptyList<DatadogPlugin>())
                it.hasFieldEqualTo("samplingRate", expectedRumSampleRate)
            }
        val credentials = credentialCaptor.firstValue
        assertThat(credentials.clientToken).isEqualTo(configuration.clientToken)
        assertThat(credentials.envName).isEqualTo(configuration.env)
        assertThat(credentials.rumApplicationId).isEqualTo(configuration.applicationId)
        assertThat(credentials.variant).isEqualTo("")
    }

    @Test
    fun `𝕄 initialize native SDK 𝕎 initialize() {native crash report disabled, site = null}`(
        @Forgery configuration: DdSdkConfiguration
    ) {
        // Given
        val bridgeConfiguration = configuration.copy(nativeCrashReportEnabled = false, site = null)
        val credentialCaptor = argumentCaptor<Credentials>()
        val configCaptor = argumentCaptor<Configuration>()
        val expectedRumSampleRate = bridgeConfiguration.sampleRate?.toFloat() ?: 100f

        // When
        testedBridgeSdk.initialize(bridgeConfiguration)

        // Then
        inOrder(mockDatadog) {
            verify(mockDatadog).initialize(
                same(mockContext),
                credentialCaptor.capture(),
                configCaptor.capture(),
                eq(TrackingConsent.GRANTED)
            )
            verify(mockDatadog).registerRumMonitor(any())
        }
        assertThat(configCaptor.firstValue)
            .hasField("coreConfig") {
                it.hasFieldEqualTo("needsClearTextHttp", false)
                it.hasFieldEqualTo("firstPartyHosts", emptyList<String>())
                it.hasFieldEqualTo("batchSize", BatchSize.MEDIUM)
                it.hasFieldEqualTo("uploadFrequency", UploadFrequency.AVERAGE)
            }
            .hasField("logsConfig") {
                it.hasFieldEqualTo("endpointUrl", DatadogEndpoint.LOGS_US)
                it.hasFieldEqualTo("plugins", emptyList<DatadogPlugin>())
            }
            .hasField("tracesConfig") {
                it.hasFieldEqualTo("endpointUrl", DatadogEndpoint.TRACES_US)
                it.hasFieldEqualTo("plugins", emptyList<DatadogPlugin>())
            }
            .hasFieldEqualTo("crashReportConfig", null)
            .hasField("rumConfig") {
                it.hasFieldEqualTo("endpointUrl", DatadogEndpoint.RUM_US)
                it.hasFieldEqualTo("plugins", emptyList<DatadogPlugin>())
                it.hasFieldEqualTo("samplingRate", expectedRumSampleRate)
            }
        val credentials = credentialCaptor.firstValue
        assertThat(credentials.clientToken).isEqualTo(configuration.clientToken)
        assertThat(credentials.envName).isEqualTo(configuration.env)
        assertThat(credentials.rumApplicationId).isEqualTo(configuration.applicationId)
        assertThat(credentials.variant).isEqualTo("")
    }

    @Test
    fun `𝕄 initialize native SDK 𝕎 initialize() {native crash report null, site = null}`(
        @Forgery configuration: DdSdkConfiguration
    ) {
        // Given
        val bridgeConfiguration = configuration.copy(nativeCrashReportEnabled = null, site = null)
        val credentialCaptor = argumentCaptor<Credentials>()
        val configCaptor = argumentCaptor<Configuration>()
        val expectedRumSampleRate = bridgeConfiguration.sampleRate?.toFloat() ?: 100f

        // When
        testedBridgeSdk.initialize(bridgeConfiguration)

        // Then
        inOrder(mockDatadog) {
            verify(mockDatadog).initialize(
                same(mockContext),
                credentialCaptor.capture(),
                configCaptor.capture(),
                eq(TrackingConsent.GRANTED)
            )
            verify(mockDatadog).registerRumMonitor(any())
        }
        assertThat(configCaptor.firstValue)
            .hasField("coreConfig") {
                it.hasFieldEqualTo("needsClearTextHttp", false)
                it.hasFieldEqualTo("firstPartyHosts", emptyList<String>())
                it.hasFieldEqualTo("batchSize", BatchSize.MEDIUM)
                it.hasFieldEqualTo("uploadFrequency", UploadFrequency.AVERAGE)
            }
            .hasField("logsConfig") {
                it.hasFieldEqualTo("endpointUrl", DatadogEndpoint.LOGS_US)
                it.hasFieldEqualTo("plugins", emptyList<DatadogPlugin>())
            }
            .hasField("tracesConfig") {
                it.hasFieldEqualTo("endpointUrl", DatadogEndpoint.TRACES_US)
                it.hasFieldEqualTo("plugins", emptyList<DatadogPlugin>())
            }
            .hasFieldEqualTo("crashReportConfig", null)
            .hasField("rumConfig") {
                it.hasFieldEqualTo("endpointUrl", DatadogEndpoint.RUM_US)
                it.hasFieldEqualTo("plugins", emptyList<DatadogPlugin>())
                it.hasFieldEqualTo("samplingRate", expectedRumSampleRate)
            }
        val credentials = credentialCaptor.firstValue
        assertThat(credentials.clientToken).isEqualTo(configuration.clientToken)
        assertThat(credentials.envName).isEqualTo(configuration.env)
        assertThat(credentials.rumApplicationId).isEqualTo(configuration.applicationId)
        assertThat(credentials.variant).isEqualTo("")
    }

    @Test
    fun `𝕄 initialize native SDK 𝕎 initialize() {US site}`(
        @Forgery configuration: DdSdkConfiguration
    ) {
        // Given
        val bridgeConfiguration = configuration.copy(site = "US")
        val credentialCaptor = argumentCaptor<Credentials>()
        val configCaptor = argumentCaptor<Configuration>()
        val expectedRumSampleRate = bridgeConfiguration.sampleRate?.toFloat() ?: 100f

        // When
        testedBridgeSdk.initialize(bridgeConfiguration)

        // Then
        inOrder(mockDatadog) {
            verify(mockDatadog).initialize(
                same(mockContext),
                credentialCaptor.capture(),
                configCaptor.capture(),
                eq(TrackingConsent.GRANTED)
            )
            verify(mockDatadog).registerRumMonitor(any())
        }
        assertThat(configCaptor.firstValue)
            .hasField("coreConfig") {
                it.hasFieldEqualTo("needsClearTextHttp", false)
                it.hasFieldEqualTo("firstPartyHosts", emptyList<String>())
                it.hasFieldEqualTo("batchSize", BatchSize.MEDIUM)
                it.hasFieldEqualTo("uploadFrequency", UploadFrequency.AVERAGE)
            }
            .hasField("logsConfig") {
                it.hasFieldEqualTo("endpointUrl", DatadogEndpoint.LOGS_US)
                it.hasFieldEqualTo("plugins", emptyList<DatadogPlugin>())
            }
            .hasField("tracesConfig") {
                it.hasFieldEqualTo("endpointUrl", DatadogEndpoint.TRACES_US)
                it.hasFieldEqualTo("plugins", emptyList<DatadogPlugin>())
            }
            .hasField("rumConfig") {
                it.hasFieldEqualTo("endpointUrl", DatadogEndpoint.RUM_US)
                it.hasFieldEqualTo("plugins", emptyList<DatadogPlugin>())
                it.hasFieldEqualTo("samplingRate", expectedRumSampleRate)
            }
        val credentials = credentialCaptor.firstValue
        assertThat(credentials.clientToken).isEqualTo(configuration.clientToken)
        assertThat(credentials.envName).isEqualTo(configuration.env)
        assertThat(credentials.rumApplicationId).isEqualTo(configuration.applicationId)
        assertThat(credentials.variant).isEqualTo("")
    }

    @Test
    fun `𝕄 initialize native SDK 𝕎 initialize() {EU site}`(
        @Forgery configuration: DdSdkConfiguration
    ) {
        // Given
        val bridgeConfiguration = configuration.copy(site = "EU")
        val credentialCaptor = argumentCaptor<Credentials>()
        val configCaptor = argumentCaptor<Configuration>()
        val expectedRumSampleRate = bridgeConfiguration.sampleRate?.toFloat() ?: 100f

        // When
        testedBridgeSdk.initialize(bridgeConfiguration)

        // Then
        inOrder(mockDatadog) {
            verify(mockDatadog).initialize(
                same(mockContext),
                credentialCaptor.capture(),
                configCaptor.capture(),
                eq(TrackingConsent.GRANTED)
            )
            verify(mockDatadog).registerRumMonitor(any())
        }
        assertThat(configCaptor.firstValue)
            .hasField("coreConfig") {
                it.hasFieldEqualTo("needsClearTextHttp", false)
                it.hasFieldEqualTo("firstPartyHosts", emptyList<String>())
                it.hasFieldEqualTo("batchSize", BatchSize.MEDIUM)
                it.hasFieldEqualTo("uploadFrequency", UploadFrequency.AVERAGE)
            }
            .hasField("logsConfig") {
                it.hasFieldEqualTo("endpointUrl", DatadogEndpoint.LOGS_EU)
                it.hasFieldEqualTo("plugins", emptyList<DatadogPlugin>())
            }
            .hasField("tracesConfig") {
                it.hasFieldEqualTo("endpointUrl", DatadogEndpoint.TRACES_EU)
                it.hasFieldEqualTo("plugins", emptyList<DatadogPlugin>())
            }
            .hasField("rumConfig") {
                it.hasFieldEqualTo("endpointUrl", DatadogEndpoint.RUM_EU)
                it.hasFieldEqualTo("plugins", emptyList<DatadogPlugin>())
                it.hasFieldEqualTo("samplingRate", expectedRumSampleRate)
            }
        val credentials = credentialCaptor.firstValue
        assertThat(credentials.clientToken).isEqualTo(configuration.clientToken)
        assertThat(credentials.envName).isEqualTo(configuration.env)
        assertThat(credentials.rumApplicationId).isEqualTo(configuration.applicationId)
        assertThat(credentials.variant).isEqualTo("")
    }

    @Test
    fun `𝕄 initialize native SDK 𝕎 initialize() {GOV site}`(
        @Forgery configuration: DdSdkConfiguration
    ) {
        // Given
        val bridgeConfiguration = configuration.copy(site = "GOV")
        val credentialCaptor = argumentCaptor<Credentials>()
        val configCaptor = argumentCaptor<Configuration>()
        val expectedRumSampleRate = bridgeConfiguration.sampleRate?.toFloat() ?: 100f

        // When
        testedBridgeSdk.initialize(bridgeConfiguration)

        // Then
        inOrder(mockDatadog) {
            verify(mockDatadog).initialize(
                same(mockContext),
                credentialCaptor.capture(),
                configCaptor.capture(),
                eq(TrackingConsent.GRANTED)
            )
            verify(mockDatadog).registerRumMonitor(any())
        }
        assertThat(configCaptor.firstValue)
            .hasField("coreConfig") {
                it.hasFieldEqualTo("needsClearTextHttp", false)
                it.hasFieldEqualTo("firstPartyHosts", emptyList<String>())
                it.hasFieldEqualTo("batchSize", BatchSize.MEDIUM)
                it.hasFieldEqualTo("uploadFrequency", UploadFrequency.AVERAGE)
            }
            .hasField("logsConfig") {
                it.hasFieldEqualTo("endpointUrl", DatadogEndpoint.LOGS_GOV)
                it.hasFieldEqualTo("plugins", emptyList<DatadogPlugin>())
            }
            .hasField("tracesConfig") {
                it.hasFieldEqualTo("endpointUrl", DatadogEndpoint.TRACES_GOV)
                it.hasFieldEqualTo("plugins", emptyList<DatadogPlugin>())
            }
            .hasField("rumConfig") {
                it.hasFieldEqualTo("endpointUrl", DatadogEndpoint.RUM_GOV)
                it.hasFieldEqualTo("plugins", emptyList<DatadogPlugin>())
                it.hasFieldEqualTo("samplingRate", expectedRumSampleRate)
            }
        val credentials = credentialCaptor.firstValue
        assertThat(credentials.clientToken).isEqualTo(configuration.clientToken)
        assertThat(credentials.envName).isEqualTo(configuration.env)
        assertThat(credentials.rumApplicationId).isEqualTo(configuration.applicationId)
        assertThat(credentials.variant).isEqualTo("")
    }

    @Test
    fun `𝕄 set native user info 𝕎 setUser()`(
        @MapForgery(
            key = AdvancedForgery(string = [StringForgery(StringForgeryType.NUMERICAL)]),
            value = AdvancedForgery(string = [StringForgery(StringForgeryType.ASCII)])
        ) extraInfo: Map<String, String>
    ) {
        // When
        testedBridgeSdk.setUser(extraInfo)

        // Then
        argumentCaptor<Map<String, Any?>> {
            verify(mockDatadog)
                .setUserInfo(
                    isNull(),
                    isNull(),
                    isNull(),
                    capture()
                )

            assertThat(firstValue)
                .containsAllEntriesOf(extraInfo)
                .hasSize(extraInfo.size)
        }
    }

    @Test
    fun `𝕄 set native user info 𝕎 setUser() {with id}`(
        @StringForgery id: String,
        @MapForgery(
            key = AdvancedForgery(string = [StringForgery(StringForgeryType.NUMERICAL)]),
            value = AdvancedForgery(string = [StringForgery(StringForgeryType.ASCII)])
        ) extraInfo: Map<String, String>
    ) {
        // Given
        val user = extraInfo.toMutableMap().also {
            it.put("id", id)
        }

        // When
        testedBridgeSdk.setUser(user)

        // Then
        argumentCaptor<Map<String, Any?>> {
            verify(mockDatadog)
                .setUserInfo(
                    eq(id),
                    isNull(),
                    isNull(),
                    capture()
                )

            assertThat(firstValue)
                .containsAllEntriesOf(extraInfo)
                .hasSize(extraInfo.size)
        }
    }

    @Test
    fun `𝕄 set native user info 𝕎 setUser() {with name}`(
        @StringForgery name: String,
        @MapForgery(
            key = AdvancedForgery(string = [StringForgery(StringForgeryType.NUMERICAL)]),
            value = AdvancedForgery(string = [StringForgery(StringForgeryType.ASCII)])
        ) extraInfo: Map<String, String>
    ) {
        // Given
        val user = extraInfo.toMutableMap().also {
            it.put("name", name)
        }

        // When
        testedBridgeSdk.setUser(user)

        // Then
        argumentCaptor<Map<String, Any?>> {
            verify(mockDatadog)
                .setUserInfo(
                    isNull(),
                    eq(name),
                    isNull(),
                    capture()
                )

            assertThat(firstValue)
                .containsAllEntriesOf(extraInfo)
                .hasSize(extraInfo.size)
        }
    }

    @Test
    fun `𝕄 set native user info 𝕎 setUser() {with email}`(
        @StringForgery(regex = "\\w+@\\w+\\.[a-z]{3}") email: String,
        @MapForgery(
            key = AdvancedForgery(string = [StringForgery(StringForgeryType.NUMERICAL)]),
            value = AdvancedForgery(string = [StringForgery(StringForgeryType.ASCII)])
        ) extraInfo: Map<String, String>
    ) {
        // Given
        val user = extraInfo.toMutableMap().also {
            it.put("email", email)
        }

        // When
        testedBridgeSdk.setUser(user)

        // Then
        argumentCaptor<Map<String, Any?>> {
            verify(mockDatadog)
                .setUserInfo(
                    isNull(),
                    isNull(),
                    eq(email),
                    capture()
                )

            assertThat(firstValue)
                .containsAllEntriesOf(extraInfo)
                .hasSize(extraInfo.size)
        }
    }

    @Test
    fun `𝕄 set native user info 𝕎 setUser() {with id, name and email}`(
        @StringForgery id: String,
        @StringForgery name: String,
        @StringForgery(regex = "\\w+@\\w+\\.[a-z]{3}") email: String,
        @MapForgery(
            key = AdvancedForgery(string = [StringForgery(StringForgeryType.NUMERICAL)]),
            value = AdvancedForgery(string = [StringForgery(StringForgeryType.ASCII)])
        ) extraInfo: Map<String, String>
    ) {
        // Given
        val user = extraInfo.toMutableMap().also {
            it.put("id", id)
            it.put("name", name)
            it.put("email", email)
        }

        // When
        testedBridgeSdk.setUser(user)

        // Then
        argumentCaptor<Map<String, Any?>> {
            verify(mockDatadog)
                .setUserInfo(
                    eq(id),
                    eq(name),
                    eq(email),
                    capture()
                )

            assertThat(firstValue)
                .containsAllEntriesOf(extraInfo)
                .hasSize(extraInfo.size)
        }
    }

    @Test
    fun `𝕄 set RUM attributes 𝕎 setAttributes`(
        @MapForgery(
            key = AdvancedForgery(string = [StringForgery(StringForgeryType.NUMERICAL)]),
            value = AdvancedForgery(string = [StringForgery(StringForgeryType.ASCII)])
        ) customAttributes: Map<String, String>
    ) {
        // Given

        // When
        testedBridgeSdk.setAttributes(customAttributes)

        // Then
        verify(mockDatadog).addRumGlobalAttributes(customAttributes)
    }
}
