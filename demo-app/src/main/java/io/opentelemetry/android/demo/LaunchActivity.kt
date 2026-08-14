/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.demo

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * The app's launcher Activity. Initializes Datadog RUM and the OpenTelemetry Android agent from
 * this Intent's extras (used by the synthetic test to target different orgs/DCs), then hands off
 * to [MainActivity].
 *
 * SDK init must happen here rather than in MainActivity.onCreate(): Rum.enable() registers
 * ActivityLifecycleCallbacks that FragmentViewTrackingStrategy relies on to track views. If
 * initialization happens inside an Activity's own onCreate(), those callbacks are registered too
 * late to observe that same Activity's onCreate()/onStart(), so its view is never tracked. Since
 * Application.onCreate() runs before any Intent is available, this dedicated launch Activity is
 * the earliest point where both the Intent extras and the callback registration timing line up.
 */
class LaunchActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = DemoAppConfig.fromIntent(intent)
        OtelDemoApplication.initializeIfNeeded(applicationContext, config)

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
