import java.io.FileInputStream
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.datadog.gradle.plugin.InstrumentationMode

buildscript {
    dependencies {
        classpath("com.datadoghq:dd-sdk-android-gradle-plugin:1.22.0")
    }
}

plugins {
    alias(rootLibs.plugins.androidApp)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.compose.compiler)
    id("net.bytebuddy.byte-buddy-gradle-plugin") version "1.17.7"
    id("com.datadoghq.dd-sdk-android-gradle-plugin") version "1.22.0"
}

val localProperties = Properties()
localProperties.load(FileInputStream(rootProject.file("local.properties")))

android {
    namespace = "io.opentelemetry.android.demo"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.opentelemetry.android.demo"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        all {
            val accessToken = localProperties["rum.access.token"] as String?
            resValue("string", "rum_access_token", accessToken ?: "fakebroken")
            manifestPlaceholders.put("appName", "OpenTelemetryDemoApp")

            // Read from ~/.gradle/gradle.properties
            val clientToken = project.findProperty("ANDROID_OTEL_DEMO_CLIENT_TOKEN") as String? ?: ""
            buildConfigField("String", "CLIENT_TOKEN", "\"$clientToken\"")

            val stagingSpansUrl = project.findProperty("ANDROID_OTEL_STAGING_SPANS_URL") as String? ?: ""
            buildConfigField("String", "STAGING_SPANS_URL", "\"$stagingSpansUrl\"")

            val stagingLogsUrl = project.findProperty("ANDROID_OTEL_STAGING_LOGS_URL") as String? ?: ""
            buildConfigField("String", "STAGING_LOGS_URL", "\"$stagingLogsUrl\"")

            val stagingMetricsUrl = project.findProperty("ANDROID_OTEL_STAGING_METRICS_URL") as String? ?: ""
            buildConfigField("String", "STAGING_METRICS_URL", "\"$stagingMetricsUrl\"")

            val productionSpansUrl = project.findProperty("ANDROID_OTEL_PRODUCTION_SPANS_URL") as String? ?: ""
            buildConfigField("String", "PRODUCTION_SPANS_URL", "\"$productionSpansUrl\"")

            val productionLogsUrl = project.findProperty("ANDROID_OTEL_PRODUCTION_LOGS_URL") as String? ?: ""
            buildConfigField("String", "PRODUCTION_LOGS_URL", "\"$productionLogsUrl\"")

            val productionMetricsUrl = project.findProperty("ANDROID_OTEL_PRODUCTION_METRICS_URL") as String? ?: ""
            buildConfigField("String", "PRODUCTION_METRICS_URL", "\"$productionMetricsUrl\"")
        }
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs["debug"]
        }
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    val javaVersion = JavaVersion.VERSION_11
    compileOptions {
        sourceCompatibility(javaVersion)
        targetCompatibility(javaVersion)
        isCoreLibraryDesugaringEnabled = true
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
}

datadog {
    composeInstrumentation = InstrumentationMode.AUTO
}

dependencies {
    implementation(libs.gson)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.opentelemetry.api.incubator)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.material.icons.core)

    // Datadog RUM SDK
    implementation("com.datadoghq:dd-sdk-android-rum:3.6.0")
    implementation("com.datadoghq:dd-sdk-android-okhttp:3.6.0")
    implementation("com.datadoghq:dd-sdk-android-compose:3.6.0")

    coreLibraryDesugaring(libs.desugarJdkLibs)

    // These are sourced from local project dirs. See settings.gradle.kts for the
    // configured substitutions.
    implementation("io.opentelemetry.android:android-agent")    //parent dir
    implementation("io.opentelemetry.android.instrumentation:compose-click")
    implementation("io.opentelemetry.android.instrumentation:sessions")

    // OkHttp instrumentation for network request example
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.14")
    implementation("io.opentelemetry.android.instrumentation:okhttp3-library")
    byteBuddy("io.opentelemetry.android.instrumentation:okhttp3-agent")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation(libs.opentelemetry.exporter.otlp)

    testImplementation(libs.bundles.junit)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

configurations.all {
    resolutionStrategy {
        eachDependency {
            if (requested.group == "com.squareup.okhttp3" && requested.name == "okhttp-jvm") {
                useTarget("com.squareup.okhttp3:okhttp:${requested.version}")
                because("choosing okhttp over okhttp-jvm")
            }
        }
    }
}

