# Android Release Signing Setup Guide

Complete guide for setting up Android release signing from scratch for your Expo React Native app.

---

## Step 0: Configure App Name and Package

**Before generating the android folder**, configure your app identity in `app.json`:

```json
{
  "expo": {
    "name": "Suvidha",
    "slug": "suvidha-app",
    "version": "1.0.0",
    "android": {
      "package": "com.just_sri.suvidhaapp"
    }
  }
}
```

> [!IMPORTANT]
> The `android.package` must be **inside** the `expo` object, not at root level!

| Field | Description | Can Change After Publishing? |
|-------|-------------|------------------------------|
| `expo.name` | Display name shown to users | ✅ Yes |
| `expo.slug` | URL-friendly identifier | ⚠️ Not recommended |
| `expo.android.package` | Unique app ID on Play Store | ❌ **No** - creates new app |

> [!WARNING]
> **Package Name** cannot be changed after publishing to Google Play. Changing it creates a completely different app.

---

## Prerequisites: Install Android SDK

Before building, you need Android SDK installed.

### Option 1: Install Android Studio (Recommended)
1. Download [Android Studio](https://developer.android.com/studio)
2. Install and open it
3. Go to **Settings** → **Languages & Frameworks** → **Android SDK**
4. Install SDK Platform (API 36) and Build Tools

### Option 2: Command Line Only
```bash
# Install via Homebrew (macOS)
brew install --cask android-commandlinetools

# Set environment variables in ~/.zshrc
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/emulator
export PATH=$PATH:$ANDROID_HOME/platform-tools
```

### Create local.properties
Create `android/local.properties` with your SDK path:
```
sdk.dir=/Users/srikant/Library/Android/sdk
```

---

## Step 1: Generate Android Folder

```bash
npx expo prebuild --platform android
```

> [!CAUTION]
> Running `expo prebuild` will **reset** your `build.gradle` and **delete** any keystore files in the android folder! Always backup your keystore first.

---

## Step 2: Create Upload Keystore

Generate a new keystore for signing your app:

```bash
keytool -genkey -v -keystore upload-keystore.jks \
  -alias upload \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass srikant \
  -keypass srikant \
  -dname "CN=Srikant Agrawal, OU=Suvidha, O=Suvidha, L=Pune, ST=Maharashtra, C=IN"
```

> [!IMPORTANT]
> Replace `srikant` with a secure password and save it somewhere safe!

---

## Step 3: Move Keystore to Android App Folder

```bash
mv upload-keystore.jks android/app/
```

---

## Step 4: Generate Upload Certificate

Export the certificate for Google Play:

```bash
keytool -export -rfc \
  -keystore android/app/upload-keystore.jks \
  -alias upload \
  -file upload_certificate.pem \
  -storepass srikant
```

---

## Step 5: Configure build.gradle

Edit `android/app/build.gradle` and update the `signingConfigs` section:

```groovy
signingConfigs {
    debug {
        storeFile file('debug.keystore')
        storePassword 'android'
        keyAlias 'androiddebugkey'
        keyPassword 'android'
    }
    release {
        storeFile file('upload-keystore.jks')
        storePassword 'srikant'
        keyAlias 'upload'
        keyPassword 'srikant'
    }
}
```

Update the `release` buildType:

```groovy
buildTypes {
    release {
        signingConfig signingConfigs.release  // <- Change from debug to release
        // ... rest of config
    }
}
```

> [!WARNING]
> **After running `expo prebuild`**, you must:
> 1. Move the keystore back to `android/app/`
> 2. Re-add the `release` signing config to `build.gradle`
> 3. Update `buildTypes.release` to use `signingConfigs.release`

---

## Step 6: Build Release AAB

```bash
cd android
./gradlew bundleRelease
```

Output: `android/app/build/outputs/bundle/release/app-release.aab`

---

## Step 7: Upload to Google Play Console

### For New Apps:
1. Go to [Google Play Console](https://play.google.com/console)
2. Create your app
3. Go to **Setup** → **App signing**
4. Upload `upload_certificate.pem` when prompted
5. Upload your AAB in **Production** or **Testing** tracks

### For Existing Apps (Key Reset):
1. Go to **Setup** → **App signing**
2. Click **Request upload key reset**
3. Wait for Google approval (1-2 days)
4. Upload new `upload_certificate.pem`

---

## Backup Checklist

> [!CAUTION]
> Keep these files safe **outside** the android folder! If lost, you'll need to request an upload key reset.

- [ ] `upload-keystore.jks` (backup outside project)
- [ ] Keystore password noted securely
- [ ] `upload_certificate.pem`
