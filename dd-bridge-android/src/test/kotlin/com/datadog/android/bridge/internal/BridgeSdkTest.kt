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
import org.assertj.core.api.Assertions.assertThat
import com.datadog.tools.unit.forge.BaseConfigurator
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.same
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.xgouchet.elmyr.annotation.Forgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
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
        whenever (mockContext.applicationContext) doReturn mockContext
        testedBridgeSdk = BridgeSdk(mockContext, mockDatadog)
    }

    @Test
    fun `𝕄 initialize native SDK 𝕎 initialize()`(
        @Forgery configuration: DdSdkConfiguration
    ) {
        // Given
        val credentialCaptor = argumentCaptor<Credentials>()
        val configCaptor = argumentCaptor<Configuration>()

        // When
        testedBridgeSdk.initialize(configuration)

        // Then
        verify(mockDatadog).initialize(
            same(mockContext),
            credentialCaptor.capture(),
            configCaptor.capture(),
            eq(TrackingConsent.GRANTED)
        )
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
                it.hasFieldEqualTo("samplingRate", 100f)
            }
        val credentials = credentialCaptor.firstValue
        assertThat(credentials.clientToken).isEqualTo(configuration.clientToken)
        assertThat(credentials.envName).isEqualTo(configuration.env)
        assertThat(credentials.rumApplicationId).isEqualTo(configuration.applicationId)
        assertThat(credentials.variant).isEqualTo("")
    }
}