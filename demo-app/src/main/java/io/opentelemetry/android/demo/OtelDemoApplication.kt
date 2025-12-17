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
import io.opentelemetry.android.instrumentation.view.click.ViewClickInstrumentation
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.logs.LogRecordBuilder
import io.opentelemetry.api.logs.LoggerProvider
import io.opentelemetry.api.metrics.LongCounter
import io.opentelemetry.api.trace.Tracer

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

        Log.i(TAG, "Initializing the opentelemetry-android-agent")

        try {
            rum = OpenTelemetryRumInitializer.initialize(
                context = this@OtelDemoApplication,
                configuration = {
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
                    instrumentations {
                        suppressing (
                            "unwanted.instrumentation.name",
                            "something.unwanted"
                        )
                        screenOrientation {
                            enabled(false)
                        }
                    }
                },
            )
            ViewClickInstrumentation().install(this, rum!!)
            Log.d(TAG, "RUM session started: " + rum?.sessionProvider?.getSessionId())
        } catch (e: Exception) {
            Log.e(TAG, "Oh no!", e)
        }
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
