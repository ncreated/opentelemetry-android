/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.demo

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.util.Log
import io.opentelemetry.android.Incubating
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.agent.OpenTelemetryRumInitializer
import io.opentelemetry.android.instrumentation.view.click.ViewClickInstrumentation
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.logs.LogRecordBuilder
import io.opentelemetry.api.logs.LoggerProvider
import io.opentelemetry.api.metrics.LongCounter
import io.opentelemetry.api.trace.Tracer
import com.datadog.android.Datadog
import com.datadog.android.core.configuration.Configuration
import com.datadog.android.privacy.TrackingConsent
import com.datadog.android.rum.Rum
import com.datadog.android.rum.RumConfiguration
import com.datadog.android.rum.tracking.FragmentViewTrackingStrategy
import com.datadog.android.compose.enableComposeActionTracking
import com.datadog.android.rum.GlobalRumMonitor
import com.datadog.android.rum.tracking.MixedViewTrackingStrategy
import java.util.logging.Level
import java.util.logging.Logger

const val TAG = "otel.demo"

enum class OtelEnvironment {
    STAGING,
    PRODUCTION,
    LOCALHOST
}

class OtelDemoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // SDK initialization is intentionally deferred until LaunchActivity.onCreate(), where the
        // launching Intent's extras (used by the synthetic test to target different orgs/DCs)
        // are first available. See OtelDemoApplication.initializeIfNeeded.
    }

    companion object {
        var rum: OpenTelemetryRum? = null
            private set

        var config: DemoAppConfig? = null
            private set

        /**
         * Initializes Datadog RUM and the OpenTelemetry Android agent using [config].
         * Safe to call multiple times; only the first call takes effect.
         */
        @OptIn(Incubating::class)
        @SuppressLint("RestrictedApi")
        @Synchronized
        fun initializeIfNeeded(context: Context, config: DemoAppConfig) {
            if (rum != null) {
                return
            }

            this.config = config

            // Android's default java.util.logging config caps everything at INFO; raise it so the
            // OTel SDK's internal diagnostics (e.g. HttpExporter export attempts/failures) show up
            // in Logcat under their own class-name tags (e.g. "HttpExporter").
            Logger.getLogger("io.opentelemetry").level = Level.FINE
            Logger.getLogger("").handlers.forEach { it.level = Level.FINE }

            logConfig(config)

            // Generate a shared correlation ID for both OTel and Datadog RUM
            // This is used to find the same session from Datadog RUM and OTel.
            val correlationId = java.util.UUID.randomUUID().toString()

            if (config.useDatadog) {
                Log.i(TAG, "Initializing Datadog RUM")
                initializeDatadog(context, config, correlationId)
            } else {
                Log.i(TAG, "Skipping Datadog RUM initialization (useDatadog=false)")
            }

            Log.i(TAG, "Initializing the opentelemetry-android-agent")

            try {
                rum = OpenTelemetryRumInitializer.initialize(
                    context = context,
                    configuration = {
                        this.correlationId = correlationId
                        diskBuffering {
                            enabled(false)
                        }
                        httpExport {
                            spans {
                                fullUrl = config.spansUrl
                                headers = mapOf(
                                    "dd-api-key" to config.clientToken,
                                    "dd-otlp-source" to "datadog"
                                )
                            }
                            logs {
                                fullUrl = config.logsUrl
                                headers = mapOf("dd-api-key" to config.clientToken)
                            }
                            metrics {
                                fullUrl = config.metricsUrl
                                headers = mapOf("dd-api-key" to config.clientToken)
                            }
                        }
                        globalAttributes {
                            Attributes.of(stringKey("my-custom-attr"), "the value 42")
                        }
                        instrumentations {
                            suppressing(
                                "unwanted.instrumentation.name",
                                "something.unwanted"
                            )
                            screenOrientation {
                                enabled(false)
                            }
                        }
                    },
                )
                ViewClickInstrumentation().install(context, rum!!)
                Log.d(TAG, "RUM session started: " + rum?.sessionProvider?.getSessionId())
            } catch (e: Exception) {
                Log.e(TAG, "Oh no!", e)
            }
        }

        private fun logConfig(config: DemoAppConfig) {
            Log.d(TAG, "environment=${config.environment}")
            Log.d(TAG, "USE_DATADOG=${config.useDatadog}")
            Log.d(TAG, "CLIENT_TOKEN=${config.clientToken}")
            Log.d(TAG, "RUM_APPLICATION_ID=${config.rumApplicationId}")
            Log.d(TAG, "DATADOG_SITE=${config.datadogSite}")
            Log.d(TAG, "SPANS_URL=${config.spansUrl}")
            Log.d(TAG, "LOGS_URL=${config.logsUrl}")
            Log.d(TAG, "METRICS_URL=${config.metricsUrl}")
        }

        private fun initializeDatadog(context: Context, config: DemoAppConfig, correlationId: String) {
            val environmentName = when (config.environment) {
                OtelEnvironment.PRODUCTION -> "production"
                OtelEnvironment.STAGING -> "staging"
                OtelEnvironment.LOCALHOST -> "localhost"
            }
            val variant = BuildConfig.BUILD_TYPE

            val ddConfig = Configuration.Builder(
                clientToken = config.clientToken,
                env = environmentName,
                variant = variant
            )
            .useSite(config.datadogSite)
            .build()

            Datadog.setVerbosity(android.util.Log.VERBOSE)

            Datadog.initialize(
                context = context,
                configuration = ddConfig,
                trackingConsent = TrackingConsent.GRANTED
            )

            initializeDatadogRUMFeature(config, correlationId)
        }

        private fun initializeDatadogRUMFeature(config: DemoAppConfig, correlationId: String) {
            val sessionSampleRate: Float = 100f
            val rumConfigBuilder = RumConfiguration.Builder(config.rumApplicationId)
                .useViewTrackingStrategy(MixedViewTrackingStrategy(true))
                .trackUserInteractions()
                .enableComposeActionTracking()
                .setSessionSampleRate(sessionSampleRate)

            if (config.environment == OtelEnvironment.LOCALHOST) {
                rumConfigBuilder.useCustomEndpoint("http://10.0.2.2:8000")
            }

            val rumConfig = rumConfigBuilder.build()
            Rum.enable(rumConfig)

            GlobalRumMonitor.get().addAttribute("correlation.id", correlationId)
        }

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
