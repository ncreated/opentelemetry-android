/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.demo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_MESSAGE
import io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_STACKTRACE
import io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_TYPE
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.opentelemetry.android.demo.about.AboutActivity
import io.opentelemetry.android.demo.theme.DemoAppTheme
import io.opentelemetry.android.demo.shop.ui.AstronomyShopActivity
import io.opentelemetry.api.logs.Severity
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import com.datadog.android.okhttp.DatadogInterceptor

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<DemoViewModel>()
    private val tracedHosts = listOf("httpbin.org")
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(DatadogInterceptor.Builder(tracedHosts).build())
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // SDKs are already initialized by LaunchActivity by the time this Activity is created.
        val rumApplicationId = OtelDemoApplication.config?.rumApplicationId
            ?: io.opentelemetry.android.agent.BuildConfig.RUM_APPLICATION_ID

        enableEdgeToEdge()
        setContent {
            DemoAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Row(
                            Modifier.padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            val onBackground = MaterialTheme.colorScheme.onBackground
                            CenterText(
                                fontSize = 40.sp,
                                text =
                                    buildAnnotatedString {
                                        withStyle(style = SpanStyle(color = Color(0xFFF5A800))) {
                                            append("Open")
                                        }
                                        withStyle(style = SpanStyle(color = Color(0xFF425CC7))) {
                                            append("Telemetry")
                                        }
                                        withStyle(style = SpanStyle(color = onBackground)) {
                                            append(" Android Demo")
                                        }
                                        toAnnotatedString()
                                    },
                            )
                        }
                        SessionId(
                            viewModel.sessionIdState,
                            applicationId = rumApplicationId,
                        )
                        MainOtelButton(
                            painterResource(id = R.drawable.otel_icon),
                        )
                        val context = LocalContext.current
                        LauncherButton(text = "Go shopping", onClick = {
                            context.startActivity(Intent(this@MainActivity, AstronomyShopActivity::class.java))
                        })
                        LauncherButton(text = "Learn more", onClick = {
                            context.startActivity(Intent(this@MainActivity, AboutActivity::class.java))
                        })
                        LauncherButton(text = "Test Network", onClick = {
                            makeHttpBinRequest()
                        })
                        LauncherButton(text = "Test Crash", onClick = {
                            triggerCrash()
                        })
                        LauncherButton(text = "OkHttp instrumentation", onClick = {
                            context.startActivity(Intent(this@MainActivity, OkHttpDemoActivity::class.java))
                        })
                        LauncherButton(text = "Test Regular Logs", onClick = {
                            triggerRegularLogs()
                        })
                        LauncherButton(text = "Test Caught Exception", onClick = {
                            triggerCaughtException()
                        })

                    }
                }
                Log.d(TAG, "Main Activity started ")
            }
        }
        viewModel.sessionIdState.value = OtelDemoApplication.rum?.sessionProvider?.getSessionId() ?: error("Session ID is null")

        // Request the correct phone state permission based on API level
        // This permission is needed for gathering certain network information like
        // carrier name and network subtype (LTE, 4G) on certain API levels.
        val phoneStatePermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_BASIC_PHONE_STATE
        } else {
            Manifest.permission.READ_PHONE_STATE
        }

        if (ContextCompat.checkSelfPermission(this, phoneStatePermission)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(
                this,
                arrayOf(phoneStatePermission),
                100
            )
        }
    }

    /**
     * Makes a simple HTTP request to httpbin.org to demonstrate OkHttp instrumentation.
     * This request will be automatically traced by the OkHttp instrumentation.
     */
    private fun makeHttpBinRequest() {
        val request = Request.Builder()
            .url("https://httpbin.org/json")
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "HTTPBin request failed", e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        Log.d(TAG, "HTTPBin response: ${body?.take(100)}...")
                    } else {
                        Log.e(TAG, "HTTPBin request unsuccessful: ${response.code}")
                    }
                }
            }
        })
    }

    /**
     * Triggers an uncaught exception to demonstrate the crash instrumentation.
     * This will generate a device.crash event with exception details in OpenTelemetry.
     */
    private fun triggerCrash() {
        Log.w(TAG, "Intentionally triggering a crash for testing...")
        throw RuntimeException("Test crash triggered from demo app - this is intentional!")
    }

    /**
     * Demonstrates sending regular logs with different severity levels using OpenTelemetry logger API.
     * This creates log events for each severity level to test the logging functionality.
     */
    private fun triggerRegularLogs() {
        val rum = OtelDemoApplication.rum
        if (rum != null) {
            val logger = rum.openTelemetry.logsBridge
                .loggerBuilder("io.opentelemetry.demo.logs")
                .build()

            // Send logs with each severity level
            logger.logRecordBuilder()
                .setSeverity(Severity.TRACE)
                .setBody("This is a TRACE level log message")
                .emit()

            logger.logRecordBuilder()
                .setSeverity(Severity.DEBUG)
                .setBody("This is a DEBUG level log message")
                .emit()

            logger.logRecordBuilder()
                .setSeverity(Severity.INFO)
                .setBody("This is an INFO level log message")
                .emit()

            logger.logRecordBuilder()
                .setSeverity(Severity.WARN)
                .setBody("This is a WARN level log message")
                .emit()

            logger.logRecordBuilder()
                .setSeverity(Severity.ERROR)
                .setBody("This is an ERROR level log message")
                .emit()

            logger.logRecordBuilder()
                .setSeverity(Severity.FATAL)
                .setBody("This is a FATAL level log message")
                .emit()

            Log.d(TAG, "Sent logs with all severity levels: TRACE, DEBUG, INFO, WARN, ERROR, FATAL")
        } else {
            Log.e(TAG, "RUM not initialized, cannot send logs")
        }
    }

    /**
     * Demonstrates how to catch and track an exception using OpenTelemetry logger API.
     * This creates a structured log event with exception attributes using the OTel logger.
     */
    private fun triggerCaughtException() {
        try {
            // Simulate some operation that throws an exception
            throw IllegalStateException("Test caught exception - demonstrating exception tracking via OTel logger")
        } catch (e: Exception) {
            // Get the OpenTelemetry logger
            val rum = OtelDemoApplication.rum
            if (rum != null) {
                val logger = rum.openTelemetry.logsBridge
                    .loggerBuilder("io.opentelemetry.demo.exception")
                    .build()

                // Build exception attributes
                val attributes = Attributes.builder()
                    .put(EXCEPTION_TYPE, e.javaClass.name)
                    .put(EXCEPTION_MESSAGE, e.message ?: "")
                    .put(EXCEPTION_STACKTRACE, e.stackTraceToString())
                    .build()

                // Create and emit a log record with exception details
                logger.logRecordBuilder()
                    .setEventName("exception")
                    .setSeverity(Severity.ERROR)
                    .setAllAttributes(attributes)
                    .emit()

                Log.d(TAG, "Caught exception tracked successfully as OTel log event")
            } else {
                Log.e(TAG, "RUM not initialized, cannot track exception")
            }
        }
    }
}
