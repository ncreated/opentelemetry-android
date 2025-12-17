#!/bin/bash

# Script to build and export APK for the OpenTelemetry Android Demo App
# This APK can be uploaded to Datadog Mobile Synthetics

set -e  # Exit on error

echo "========================================"
echo "Building OpenTelemetry Android Demo APK"
echo "========================================"
echo ""

# Navigate to the demo-app directory (in case script is run from elsewhere)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Constants
EXPORT_APK_NAME="opentelemetry-android-demo.apk"

# Check Java version
echo "🔍 Checking Java version..."
if ! command -v java &> /dev/null; then
    echo "❌ Error: Java is not installed"
    echo ""
    echo "Install Java 21 with: brew install openjdk@21"
    exit 1
fi

# Extract Java version (handle both old and new version formats)
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)

# For Java 9+, version is just the major number (e.g., "17" or "21")
# For Java 8 and below, it's like "1.8.0", so we need the second part
if [ "$JAVA_VERSION" = "1" ]; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f2)
fi

echo "   Java version detected: $JAVA_VERSION"

if [ "$JAVA_VERSION" -lt 21 ]; then
    echo ""
    echo "❌ Error: Java 21 or higher is required"
    echo "   Current version: Java $JAVA_VERSION"
    exit 1
fi

echo "✅ Java $JAVA_VERSION detected"
echo ""

# Clean previous builds
echo "🧹 Cleaning previous builds..."
rm -f "$EXPORT_APK_NAME"
./gradlew clean

# Build the release APK
echo ""
echo "🔨 Building release APK..."
./gradlew assembleRelease

# Define the output directory and APK paths
OUTPUT_DIR="build/outputs/apk/release"
APK_NAME="opentelemetry-android-demo-release.apk"
APK_PATH="$OUTPUT_DIR/$APK_NAME"

# Check if the APK was created successfully
if [ -f "$APK_PATH" ]; then
    echo ""
    echo "✅ APK built successfully!"
    echo ""
    echo "📦 APK Details:"
    echo "   Location: $APK_PATH"
    echo "   Size: $(du -h "$APK_PATH" | cut -f1)"
    echo ""

    # Copy APK to demo-app directory for easy access
    cp "$APK_PATH" "$EXPORT_APK_NAME"
    echo "📤 Exported APK to: demo-app/$EXPORT_APK_NAME"
    echo ""
    echo "========================================"
    echo "✨ Build complete!"
    echo "========================================"
    echo ""
    echo "Next steps:"
    echo "  - Upload '$EXPORT_APK_NAME' to Datadog Mobile Synthetics"
    echo "  - Application ID: io.opentelemetry.android.demo"
    echo ""
else
    echo ""
    echo "❌ Error: APK not found at expected location: $APK_PATH"
    exit 1
fi

