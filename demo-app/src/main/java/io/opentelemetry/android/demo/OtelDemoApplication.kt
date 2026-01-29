/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.demo

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import io.opentelemetry.android.Incubating
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.agent.OpenTelemetryRumInitializer
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.logs.LogRecordBuilder
import io.opentelemetry.api.logs.LoggerProvider
import io.opentelemetry.api.metrics.LongCounter
import io.opentelemetry.api.trace.Tracer
import com.datadog.android.Datadog
import com.datadog.android.DatadogSite
import com.datadog.android.core.configuration.Configuration
import com.datadog.android.privacy.TrackingConsent
import com.datadog.android.rum.Rum
import com.datadog.android.rum.RumConfiguration
import com.datadog.android.rum.tracking.FragmentViewTrackingStrategy

const val TAG = "otel.demo"

enum class OtelEnvironment {
    STAGING,
    PRODUCTION,
    LOCALHOST
}

class OtelDemoApplication : Application() {

    // Change this to switch between environments
    private val environment = OtelEnvironment.PRODUCTION

    @OptIn(Incubating::class)
    @SuppressLint("RestrictedApi")
    override fun onCreate() {
        super.onCreate()

        Log.i(TAG, "Initializing Datadog RUM")
        initializeDatadog()

        Log.i(TAG, "Initializing the opentelemetry-android-agent")

        try {
            rum = OpenTelemetryRumInitializer.initialize(
                context = this@OtelDemoApplication,
                configuration = {
                    diskBuffering {
                        enabled(false)
                    }
                    httpExport {
                        when (environment) {
                            OtelEnvironment.STAGING -> {
                                spans {
                                    url = BuildConfig.STAGING_SPANS_URL
                                    headers = mapOf(
                                        "dd-api-key" to BuildConfig.CLIENT_TOKEN,
                                        "dd-otlp-source" to "datadog"
                                    )
                                }
                                logs {
                                    url = BuildConfig.STAGING_LOGS_URL
                                    headers = mapOf("dd-api-key" to BuildConfig.CLIENT_TOKEN)
                                }
                                metrics {
                                    url = BuildConfig.STAGING_METRICS_URL
                                    headers = mapOf("dd-api-key" to BuildConfig.CLIENT_TOKEN)
                                }
                            }
                            OtelEnvironment.PRODUCTION -> {
                                spans {
                                    url = BuildConfig.PRODUCTION_SPANS_URL
                                    headers = mapOf(
                                        "dd-api-key" to BuildConfig.CLIENT_TOKEN,
                                        "dd-otlp-source" to "datadog"
                                    )
                                }
                                logs {
                                    url = BuildConfig.PRODUCTION_LOGS_URL
                                    headers = mapOf("dd-api-key" to BuildConfig.CLIENT_TOKEN)
                                }
                                metrics {
                                    url = BuildConfig.PRODUCTION_METRICS_URL
                                    headers = mapOf("dd-api-key" to BuildConfig.CLIENT_TOKEN)
                                }
                            }
                            // 10.0.2.2 is a special binding to the host running the emulator:
                            OtelEnvironment.LOCALHOST -> {
                                spans {
                                    url = "http://10.0.2.2:8000/v1/spans"
                                }
                                logs {
                                    url = "http://10.0.2.2:8000/v1/logs"
                                }
                                metrics {
                                    url = "http://10.0.2.2:8000/v1/metrics"
                                }
                            }
                        }
                    }
                    globalAttributes {
                        Attributes.of(stringKey("my-custom-attr"), "the value 42")
                    }
                }
            )
            Log.d(TAG, "RUM session started: " + rum?.getRumSessionId())

        } catch (e: Exception) {
            Log.e(TAG, "Oh no!", e)
        }
    }

    private fun initializeDatadog() {
        val environment = when (this.environment) {
            OtelEnvironment.PRODUCTION -> "production"
            OtelEnvironment.STAGING -> "staging"
            OtelEnvironment.LOCALHOST -> "localhost"
        }
        val variant = BuildConfig.BUILD_TYPE

        val datadogSite = if (this.environment == OtelEnvironment.STAGING) {
            DatadogSite.STAGING
        } else {
            DatadogSite.US1
        }

        val config = Configuration.Builder(
            clientToken = BuildConfig.CLIENT_TOKEN,
            env = environment,
            variant = variant
        )
        .useSite(datadogSite)
        .build()

        Datadog.initialize(
            context = this,
            configuration = config,
            trackingConsent = TrackingConsent.GRANTED
        )

        initializeDatadogRUMFeature()
    }

    private fun initializeDatadogRUMFeature() {
        val sessionSampleRate: Float = 100f
        val rumConfigBuilder = RumConfiguration.Builder(io.opentelemetry.android.agent.BuildConfig.RUM_APPLICATION_ID)
            .useViewTrackingStrategy(FragmentViewTrackingStrategy(true))
            .trackUserInteractions()
            .setSessionSampleRate(sessionSampleRate)

        if (this.environment == OtelEnvironment.LOCALHOST) {
            rumConfigBuilder.useCustomEndpoint("http://10.0.2.2:8000")
        }

        val rumConfig = rumConfigBuilder.build()
        Rum.enable(rumConfig)
    }

    companion object {
        var rum: OpenTelemetryRum? = null

        fun tracer(name: String): Tracer? {
            return rum?.openTelemetry?.tracerProvider?.get(name)
        }

        fun counter(name: String): LongCounter? {
            return rum?.openTelemetry?.meterProvider?.get("demo.app")?.counterBuilder(name)
                ?.build()
        }

        fun eventBuilder(scopeName: String, eventName: String): LogRecordBuilder {
            if (rum == null) {
                return LoggerProvider.noop().get("noop").logRecordBuilder()
            }
            val logger = rum!!.openTelemetry.logsBridge.loggerBuilder(scopeName).build()
            return logger.logRecordBuilder().setEventName(eventName)
        }
    }
}
