#!/usr/bin/env bash
#
# Build, install and watch onboarding on a USB-connected device.
#
#   ./scripts/test-onboarding.sh          # install and watch
#   ./scripts/test-onboarding.sh --fresh  # ALSO wipe app data first
#
# --fresh deletes Hook's local data (settings, onboarding progress, the install
# id the free allowance is keyed to). That's what you want to see onboarding
# from the top; it is not what you want on a phone you actually use Hook on.
#
# The log filter is the point: it shows the keyboard-setup signals changing in
# real time, so "it didn't detect the switch" becomes a line you can paste.

set -euo pipefail

PKG=com.tom7.hook
ACTIVITY=com.tomfricks.hook.MainActivity
FRESH=0
[[ "${1:-}" == "--fresh" ]] && FRESH=1

cd "$(dirname "$0")/../app"

if ! command -v adb >/dev/null; then
    echo "adb not on PATH. It ships with the Android SDK platform-tools:"
    echo "  export PATH=\$PATH:\$HOME/Library/Android/sdk/platform-tools   # macOS"
    echo "  export PATH=\$PATH:\$HOME/Android/Sdk/platform-tools           # Linux"
    exit 1
fi

if [ -z "$(adb devices | sed '1d' | grep -w device || true)" ]; then
    echo "No device in 'adb devices'. Check the cable, and that USB debugging"
    echo "is on and this computer is authorised on the phone."
    adb devices
    exit 1
fi

echo "==> Building and installing"
./gradlew :app:installDebug

if [ "$FRESH" = "1" ]; then
    echo "==> Wiping app data (onboarding will start from the first slide)"
    adb shell pm clear "$PKG"
fi

echo "==> Launching"
adb logcat -c
adb shell am start -n "$PKG/$ACTIVITY" >/dev/null

echo
echo "==> Watching. Ctrl-C to stop."
echo "    KeyboardSetup lines show enabled/selected/photos as they change —"
echo "    switch keyboards and you should see one appear within half a second."
echo
adb logcat \
    KeyboardSetup:D \
    HookKeyboard:D \
    ScreenshotDetection:D \
    ApiService:D \
    OnboardingSync:D \
    AndroidRuntime:E \
    '*:S'
