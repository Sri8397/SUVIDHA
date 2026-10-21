#!/usr/bin/env bash
set -eo pipefail

# ==============================================================================
# Script: deploy_all_devices.sh
# Purpose: Builds the debug APK and installs/restarts the app on ALL connected ADB devices
# ==============================================================================

# Default configurations
DEFAULT_PACKAGE="com.just_sri.suvidha.expenses"
DEFAULT_ACTIVITY="com.just_sri.suvidha.expenses/.MainActivity"
DEFAULT_APK="app/build/outputs/apk/debug/app-debug.apk"

# Locate project root (assuming script is in <root>/scripts/ or <root>/.agents/skills/.../scripts/)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Find project root containing gradlew
PROJECT_ROOT="$SCRIPT_DIR"
while [ "$PROJECT_ROOT" != "/" ] && [ ! -f "$PROJECT_ROOT/gradlew" ]; do
    PROJECT_ROOT="$(dirname "$PROJECT_ROOT")"
done

if [ ! -f "$PROJECT_ROOT/gradlew" ]; then
    echo "❌ Error: Could not locate project root containing gradlew." >&2
    exit 1
fi

cd "$PROJECT_ROOT"

# Detect ADB
ADB=""
if [ -x "$HOME/Library/Android/sdk/platform-tools/adb" ]; then
    ADB="$HOME/Library/Android/sdk/platform-tools/adb"
elif [ -n "$ANDROID_HOME" ] && [ -x "$ANDROID_HOME/platform-tools/adb" ]; then
    ADB="$ANDROID_HOME/platform-tools/adb"
elif command -v adb >/dev/null 2>&1; then
    ADB="$(command -v adb)"
else
    echo "❌ Error: adb executable not found in ~/Library/Android/sdk/platform-tools/adb or PATH." >&2
    exit 1
fi

# Options
SKIP_BUILD=false
PACKAGE="$DEFAULT_PACKAGE"
ACTIVITY="$DEFAULT_ACTIVITY"
APK_PATH="$DEFAULT_APK"

while [[ $# -gt 0 ]]; do
    case "$1" in
        --skip-build|-s)
            SKIP_BUILD=true
            shift
            ;;
        --apk|-a)
            APK_PATH="$2"
            shift 2
            ;;
        --package|-p)
            PACKAGE="$2"
            shift 2
            ;;
        --activity)
            ACTIVITY="$2"
            shift 2
            ;;
        --help|-h)
            echo "Usage: $0 [options]"
            echo ""
            echo "Options:"
            echo "  -s, --skip-build    Skip Gradle build step and use existing APK"
            echo "  -a, --apk <path>    Custom APK path (default: $DEFAULT_APK)"
            echo "  -p, --package <pkg> App package name (default: $DEFAULT_PACKAGE)"
            echo "  --activity <act>    Activity component (default: $DEFAULT_ACTIVITY)"
            echo "  -h, --help          Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option: $1" >&2
            exit 1
            ;;
    esac
done

echo "🔍 Using ADB: $ADB"

# 1. Fetch connected devices
echo "📡 Checking connected ADB devices..."
DEVICES=($("$ADB" devices | awk '$2=="device" {print $1}'))

if [ ${#DEVICES[@]} -eq 0 ]; then
    echo "⚠️  No authorized/active devices found."
    echo "Current ADB status:"
    "$ADB" devices -l
    echo ""
    echo "Tip: Ensure device has USB/Wireless debugging enabled and authorized."
    exit 1
fi

echo "📱 Found ${#DEVICES[@]} connected device(s):"
for dev in "${DEVICES[@]}"; do
    MODEL=$("$ADB" -s "$dev" shell getprop ro.product.model 2>/dev/null | tr -d '\r\n')
    echo "  • $dev (${MODEL:-Unknown Model})"
done
echo ""

# 2. Build APK (if not skipped)
if [ "$SKIP_BUILD" = false ]; then
    echo "🔨 Building debug APK (./gradlew assembleDebug)..."
    ./gradlew assembleDebug
else
    echo "⏩ Skipping build step (--skip-build specified)."
fi

if [ ! -f "$APK_PATH" ]; then
    echo "❌ Error: APK not found at $APK_PATH" >&2
    exit 1
fi

echo "📦 Target APK: $APK_PATH ($(du -h "$APK_PATH" | cut -f1))"
echo ""

# 3. Install and Launch on each device
FAILED_COUNT=0
SUCCESS_COUNT=0

for dev in "${DEVICES[@]}"; do
    MODEL=$("$ADB" -s "$dev" shell getprop ro.product.model 2>/dev/null | tr -d '\r\n')
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "📲 Deploying to: $dev (${MODEL:-Device})"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

    echo "  📥 Installing APK..."
    if ! "$ADB" -s "$dev" install -r "$APK_PATH"; then
        echo "  ❌ Failed to install on $dev" >&2
        FAILED_COUNT=$((FAILED_COUNT + 1))
        continue
    fi

    echo "  ⏹️  Stopping existing process ($PACKAGE)..."
    "$ADB" -s "$dev" shell am force-stop "$PACKAGE" || true

    echo "  ▶️  Launching activity ($ACTIVITY)..."
    if "$ADB" -s "$dev" shell am start -n "$ACTIVITY"; then
        echo "  ✅ Success on $dev"
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
    else
        echo "  ⚠️ App installed, but failed to start on $dev" >&2
        FAILED_COUNT=$((FAILED_COUNT + 1))
    fi
    echo ""
done

echo "=================================================="
echo "🎉 Deployment Summary:"
echo "   Total Devices: ${#DEVICES[@]}"
echo "   Successful:    $SUCCESS_COUNT"
echo "   Failed:        $FAILED_COUNT"
echo "=================================================="

if [ "$FAILED_COUNT" -gt 0 ]; then
    exit 1
fi
