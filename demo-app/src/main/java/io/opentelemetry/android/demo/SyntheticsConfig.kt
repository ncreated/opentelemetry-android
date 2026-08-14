/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.demo

import android.content.Intent

/**
 * Raw values read from the launching Intent's extras (as set by the synthetic test), before any
 * defaulting/parsing into a [DemoAppConfig]. Kept as a separate step so what the Intent actually
 * carried can be logged and inspected independently of the resolved config.
 */
data class SyntheticsConfig(
    val environment: String?,
    val clientToken: String?,
    val rumApplicationId: String?,
    val datadogSite: String?,
    val spansUrl: String?,
    val logsUrl: String?,
    val metricsUrl: String?,
    val useDatadog: Boolean?,
) {
    companion object {
        fun fromIntent(intent: Intent): SyntheticsConfig {
            return SyntheticsConfig(
                environment = intent.getStringExtra(DemoAppConfig.EXTRA_ENVIRONMENT),
                clientToken = intent.getStringExtra(DemoAppConfig.EXTRA_CLIENT_TOKEN),
                rumApplicationId = intent.getStringExtra(DemoAppConfig.EXTRA_RUM_APPLICATION_ID),
                datadogSite = intent.getStringExtra(DemoAppConfig.EXTRA_DATADOG_SITE),
                spansUrl = intent.getStringExtra(DemoAppConfig.EXTRA_SPANS_URL),
                logsUrl = intent.getStringExtra(DemoAppConfig.EXTRA_LOGS_URL),
                metricsUrl = intent.getStringExtra(DemoAppConfig.EXTRA_METRICS_URL),
                // Datadog's mobile synthetic test runner injects Initial Intent Extras as
                // Strings, so accept "true"/"false" in addition to a native boolean extra.
                useDatadog = when (val raw = intent.extras?.get(DemoAppConfig.EXTRA_USE_DATADOG)) {
                    is Boolean -> raw
                    is String -> raw.equals("true", ignoreCase = true)
                    else -> null
                },
            )
        }
    }
}
