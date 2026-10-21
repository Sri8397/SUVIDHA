---
name: deploy-all-devices
description: >-
  Build the Android app and install, update, and restart it across all connected ADB devices (emulators, Wi-Fi/TLS devices, and physical USB phones). Use when the user requests to build and deploy to all devices, update all connected devices, or run the app on all ADB devices.
---

# Deploy App to All Connected ADB Devices

This skill handles building the debug APK and installing, updating, and restarting the application across **all currently connected and active ADB devices** (including physical devices over USB/Wi-Fi and emulators).

---

## Quick Execution

Run the bundled deployment script from the project root:

```bash
./scripts/deploy_all_devices.sh
```

### Available Options:
- **Skip Gradle Build** (re-use latest existing APK):
  ```bash
  ./scripts/deploy_all_devices.sh --skip-build
  ```
- **Custom Package / Activity**:
  ```bash
  ./scripts/deploy_all_devices.sh --package <package_name> --activity <component_name>
  ```

---

## Step-by-Step Manual Procedure

If performing individual steps manually:

### 1. Build the Debug APK
```bash
./gradlew assembleDebug
```
Output path: `app/build/outputs/apk/debug/app-debug.apk`

### 2. List Connected & Active Devices
```bash
~/Library/Android/sdk/platform-tools/adb devices -l
```
Filter active devices:
```bash
~/Library/Android/sdk/platform-tools/adb devices | awk '$2=="device" {print $1}'
```

### 3. Install APK on Every Device
For each device serial / network ID (e.g., `emulator-5554`, `adb-..._tcp`, `192.168.1.15:41235`):
```bash
~/Library/Android/sdk/platform-tools/adb -s "<DEVICE_ID>" install -r app/build/outputs/apk/debug/app-debug.apk
```

### 4. Restart App on Every Device
Force-stop existing instance and start the main launcher activity:
```bash
~/Library/Android/sdk/platform-tools/adb -s "<DEVICE_ID>" shell am force-stop com.just_sri.suvidha.expenses
~/Library/Android/sdk/platform-tools/adb -s "<DEVICE_ID>" shell am start -n com.just_sri.suvidha.expenses/.MainActivity
```
