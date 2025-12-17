import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(rootLibs.plugins.androidApp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.byteBuddy)
}

android {
    namespace = "io.opentelemetry.android.demo"
    compileSdk = 37

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

    coreLibraryDesugaring(libs.desugarJdkLibs)

    // These are sourced from local project dirs. See settings.gradle.kts for the
    // configured substitutions.
    implementation("io.opentelemetry.android:android-agent")    //parent dir
    implementation("io.opentelemetry.android.instrumentation:compose-click")
    implementation("io.opentelemetry.android.instrumentation:compose-navigation")
    implementation("io.opentelemetry.android.instrumentation:sessions")
    implementation("io.opentelemetry.android.instrumentation:okhttp3-library")
    byteBuddy("io.opentelemetry.android.instrumentation:okhttp3-agent")
    implementation("io.opentelemetry.android.instrumentation:view-click")
    implementation("io.opentelemetry.android.instrumentation:android-instrumentation")
    implementation(libs.okhttp)
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

