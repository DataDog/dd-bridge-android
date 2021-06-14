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
import com.datadog.android.tracing.AndroidTracer
import com.datadog.tools.unit.GenericAssert.Companion.assertThat
import com.datadog.tools.unit.forge.BaseConfigurator
import com.datadog.tools.unit.getStaticValue
import com.datadog.tools.unit.setStaticValue
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.same
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.annotation.AdvancedForgery
import fr.xgouchet.elmyr.annotation.Forgery
import fr.xgouchet.elmyr.annotation.MapForgery
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.annotation.StringForgeryType
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import io.opentracing.Tracer
import io.opentracing.noop.NoopTracer
import io.opentracing.noop.NoopTracerFactory
import io.opentracing.util.GlobalTracer
import java.util.Locale
import org.assertj.core.api.Assertions.assertThat
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

    @AfterEach
    fun `tear down`() {
        GlobalTracer::class.java.setStaticValue("isRegistered", false)
        GlobalTracer::class.java.setStaticValue("tracer", NoopTracerFactory.create())
    }

    @Test
    fun `𝕄 initialize native SDK 𝕎 initialize() {native crash report enabled, site = null}`(
        @Forgery configuration: DdSdkConfiguration
    ) {
        // Given
        val bridgeConfiguration = configuration.copy(
            nativeCrashReportEnabled = true,
            site = null,
            manualTracingEnabled = true
        )
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
                eq(configuration.trackingConsent.asTrackingConsent())
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
            .hasFieldEqualTo(
                "additionalConfig",
                configuration.additionalConfig?.filterValues { it != null }
            )
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
        val bridgeConfiguration = configuration.copy(
            nativeCrashReportEnabled = false,
            site = null,
            manualTracingEnabled = true
        )
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
                eq(configuration.trackingConsent.asTrackingConsent())
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
            .hasFieldEqualTo(
                "additionalConfig",
                configuration.additionalConfig?.filterValues { it != null }
            )
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
        val bridgeConfiguration = configuration.copy(
            nativeCrashReportEnabled = null,
            site = null,
            manualTracingEnabled = true
        )
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
                eq(configuration.trackingConsent.asTrackingConsent())
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
            .hasFieldEqualTo(
                "additionalConfig",
                configuration.additionalConfig?.filterValues { it != null }
            )
        val credentials = credentialCaptor.firstValue
        assertThat(credentials.clientToken).isEqualTo(configuration.clientToken)
        assertThat(credentials.envName).isEqualTo(configuration.env)
        assertThat(credentials.rumApplicationId).isEqualTo(configuration.applicationId)
        assertThat(credentials.variant).isEqualTo("")
    }

    @Test
    fun `𝕄 initialize native SDK 𝕎 initialize() {additionalConfig is null}`(
        @Forgery configuration: DdSdkConfiguration
    ) {
        // Given
        val bridgeConfiguration = configuration.copy(
            additionalConfig = null,
            nativeCrashReportEnabled = false,
            site = null,
            manualTracingEnabled = true
        )
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
                eq(configuration.trackingConsent.asTrackingConsent())
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
            .hasFieldEqualTo("additionalConfig", emptyMap<String, Any>())
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
        val bridgeConfiguration = configuration.copy(site = "US", manualTracingEnabled = true)
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
                eq(configuration.trackingConsent.asTrackingConsent())
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
        val bridgeConfiguration = configuration.copy(site = "EU", manualTracingEnabled = true)
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
                eq(configuration.trackingConsent.asTrackingConsent())
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
        val bridgeConfiguration = configuration.copy(site = "GOV", manualTracingEnabled = true)
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
                eq(configuration.trackingConsent.asTrackingConsent())
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
    fun `𝕄 enable tracer 𝕎 initialize() {manualTracingEnabled = true}`(
        @Forgery configuration: DdSdkConfiguration
    ) {
        // Given
        val bridgeConfiguration = configuration.copy(manualTracingEnabled = true)
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
                eq(configuration.trackingConsent.asTrackingConsent())
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

        val tracer: Tracer = GlobalTracer::class.java.getStaticValue("tracer")
        assertThat(tracer).isInstanceOf(AndroidTracer::class.java)
    }

    @Test
    fun `𝕄 use no-op tracer + disable traces 𝕎 initialize() {manualTracingEnabled = false}`(
        @Forgery configuration: DdSdkConfiguration
    ) {
        // Given
        val bridgeConfiguration = configuration.copy(manualTracingEnabled = false)
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
                eq(configuration.trackingConsent.asTrackingConsent())
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
            .hasFieldEqualTo("tracesConfig", null)
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

        val tracer: Tracer = GlobalTracer::class.java.getStaticValue("tracer")
        assertThat(tracer).isInstanceOf(NoopTracer::class.java)
    }

    @Test
    fun `𝕄 use no-op tracer + disableTraces 𝕎 initialize() {manualTracingEnabled = null}`(
        @Forgery configuration: DdSdkConfiguration
    ) {
        // Given
        val bridgeConfiguration = configuration.copy(manualTracingEnabled = null)
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
                eq(configuration.trackingConsent.asTrackingConsent())
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
            .hasFieldEqualTo("tracesConfig", null)
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

        val tracer: Tracer = GlobalTracer::class.java.getStaticValue("tracer")
        assertThat(tracer).isInstanceOf(NoopTracer::class.java)
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

    @Test
    fun `𝕄 build Granted consent 𝕎 buildTrackingConsent {granted}`(forge: Forge) {

        // When
        val consent = testedBridgeSdk.buildTrackingConsent(
            forge.anElementFrom("granted", "GRANTED")
        )

        // Then
        assertThat(consent).isEqualTo(TrackingConsent.GRANTED)
    }

    @Test
    fun `𝕄 build Pending consent 𝕎 buildTrackingConsent {pending}`(forge: Forge) {

        // When
        val consent = testedBridgeSdk.buildTrackingConsent(
            forge.anElementFrom("pending", "PENDING")
        )

        // Then
        assertThat(consent).isEqualTo(TrackingConsent.PENDING)
    }

    @Test
    fun `𝕄 build Granted consent 𝕎 buildTrackingConsent {not_granted}`(forge: Forge) {

        // When
        val consent = testedBridgeSdk.buildTrackingConsent(
            forge.anElementFrom("not_granted", "NOT_GRANTED")
        )

        // Then
        assertThat(consent).isEqualTo(TrackingConsent.NOT_GRANTED)
    }

    @Test
    fun `𝕄 build default Pending consent 𝕎 buildTrackingConsent {any}`(forge: Forge) {

        // When
        val consent = testedBridgeSdk.buildTrackingConsent(
            forge.anElementFrom(null, "some-type")
        )

        // Then
        assertThat(consent).isEqualTo(TrackingConsent.PENDING)
    }

    @Test
    fun `𝕄 call setTrackingConsent 𝕎 setTrackingConsent ()`(forge: Forge) {

        // Given
        val consent = forge.anElementFrom("pending", "granted", "not_granted")

        // When
        testedBridgeSdk.setTrackingConsent(consent)

        // Then
        verify(mockDatadog).setTrackingConsent(consent.asTrackingConsent())
    }

    // region Internal

    private fun String?.asTrackingConsent(): TrackingConsent {
        return when (this?.toLowerCase(Locale.US)) {
            "pending" -> TrackingConsent.PENDING
            "granted" -> TrackingConsent.GRANTED
            "not_granted" -> TrackingConsent.NOT_GRANTED
            else -> TrackingConsent.PENDING
        }
    }

    // endregion
}
