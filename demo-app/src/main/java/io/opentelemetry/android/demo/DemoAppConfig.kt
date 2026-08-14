/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.demo

import android.content.Intent
import android.util.Log
import com.datadog.android.DatadogSite

/**
 * Configuration used to initialize OTel/Datadog RUM. Defaults come from build-time BuildConfig
 * values, but every field can be overridden via the launching Intent's extras (see [fromIntent]),
 * so a synthetic test can target a different org/DC without rebuilding the app.
 */
data class DemoAppConfig(
    val environment: OtelEnvironment = OtelEnvironment.PRODUCTION,
    val clientToken: String = BuildConfig.CLIENT_TOKEN,
    val rumApplicationId: String = io.opentelemetry.android.agent.BuildConfig.RUM_APPLICATION_ID,
    val datadogSite: DatadogSite = defaultDatadogSite(environment),
    val spansUrl: String = defaultSpansUrl(environment),
    val logsUrl: String = defaultLogsUrl(environment),
    val metricsUrl: String = defaultMetricsUrl(environment),
    val useDatadog: Boolean = BuildConfig.USE_DATADOG_SDK,
) {
    companion object {
        const val EXTRA_ENVIRONMENT = "otel_demo_environment"
        const val EXTRA_CLIENT_TOKEN = "otel_demo_client_token"
        const val EXTRA_RUM_APPLICATION_ID = "otel_demo_rum_application_id"
        const val EXTRA_DATADOG_SITE = "otel_demo_datadog_site"
        const val EXTRA_SPANS_URL = "otel_demo_spans_url"
        const val EXTRA_LOGS_URL = "otel_demo_logs_url"
        const val EXTRA_METRICS_URL = "otel_demo_metrics_url"
        const val EXTRA_USE_DATADOG = "otel_demo_use_datadog_sdk"

        fun fromIntent(intent: Intent): DemoAppConfig {
            val synthetics = SyntheticsConfig.fromIntent(intent)
            Log.d(TAG, "SyntheticsConfig: $synthetics")
            return fromSyntheticsConfig(synthetics)
        }

        fun fromSyntheticsConfig(synthetics: SyntheticsConfig): DemoAppConfig {
            val environment = synthetics.environment?.let { name ->
                OtelEnvironment.values().firstOrNull { it.name.equals(name, ignoreCase = true) }
            } ?: OtelEnvironment.PRODUCTION

            return DemoAppConfig(
                environment = environment,
                clientToken = synthetics.clientToken ?: BuildConfig.CLIENT_TOKEN,
                rumApplicationId = synthetics.rumApplicationId
                    ?: io.opentelemetry.android.agent.BuildConfig.RUM_APPLICATION_ID,
                datadogSite = synthetics.datadogSite?.let { name ->
                    runCatching { DatadogSite.valueOf(name) }.getOrNull()
                } ?: defaultDatadogSite(environment),
                spansUrl = synthetics.spansUrl ?: defaultSpansUrl(environment),
                logsUrl = synthetics.logsUrl ?: defaultLogsUrl(environment),
                metricsUrl = synthetics.metricsUrl ?: defaultMetricsUrl(environment),
                useDatadog = synthetics.useDatadog ?: BuildConfig.USE_DATADOG_SDK,
            )
        }
    }
}

private fun defaultDatadogSite(environment: OtelEnvironment): DatadogSite =
    if (environment == OtelEnvironment.STAGING) DatadogSite.STAGING else DatadogSite.US1

// 10.0.2.2 is a special binding to the host running the emulator
private fun defaultSpansUrl(environment: OtelEnvironment): String = when (environment) {
    OtelEnvironment.STAGING -> BuildConfig.STAGING_SPANS_URL
    OtelEnvironment.PRODUCTION -> BuildConfig.PRODUCTION_SPANS_URL
    OtelEnvironment.LOCALHOST -> "http://10.0.2.2:8000/v1/spans"
}

private fun defaultLogsUrl(environment: OtelEnvironment): String = when (environment) {
    OtelEnvironment.STAGING -> BuildConfig.STAGING_LOGS_URL
    OtelEnvironment.PRODUCTION -> BuildConfig.PRODUCTION_LOGS_URL
    OtelEnvironment.LOCALHOST -> "http://10.0.2.2:8000/v1/logs"
}

private fun defaultMetricsUrl(environment: OtelEnvironment): String = when (environment) {
    OtelEnvironment.STAGING -> BuildConfig.STAGING_METRICS_URL
    OtelEnvironment.PRODUCTION -> BuildConfig.PRODUCTION_METRICS_URL
    OtelEnvironment.LOCALHOST -> "http://10.0.2.2:8000/v1/metrics"
}
