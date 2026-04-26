# Universal Remote — Redmi 12 IR Blaster App

A native Android app that uses the Redmi 12's built-in IR blaster
(ConsumerIrManager API) to control TV, A/C, Fan, Projector, DVD, and Audio.

## How to build & install

### Option A — Android Studio (easiest)

1. Install Android Studio: https://developer.android.com/studio
2. Open this folder as a project (File → Open → select UniversalRemote/)
3. Wait for Gradle sync to finish
4. Connect your Redmi 12 via USB, enable USB Debugging:
   Settings → About phone → tap MIUI version 7 times → Developer options → USB debugging ON
5. Click ▶ Run (green play button)
6. App installs directly on your phone

### Option B — Command line (Gradle)

```bash
# Set ANDROID_SDK_ROOT to your SDK path first
export ANDROID_SDK_ROOT=~/Android/Sdk

# Build debug APK
./gradlew assembleDebug

# Install on connected phone
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Option C — Build APK and sideload

```bash
./gradlew assembleRelease
# APK at: app/build/outputs/apk/release/app-release-unsigned.apk
# Transfer to phone, allow "Install from unknown sources" in settings
```

## How it works

- Uses `android.hardware.ConsumerIrManager` — the real IR blaster API
- Each button tap fires a raw IR pulse pattern to the hardware
- Patterns are stored as int[] arrays of alternating ON/OFF durations in microseconds
- Each button cycles through multiple protocol patterns per tap (NEC → Samsung → LG → Sony → etc.)
- Auto-scan fires all patterns automatically every 600ms — just point at device and watch

## Supported devices & protocols

| Device     | Protocols                                         |
|------------|---------------------------------------------------|
| TV         | NEC, Samsung, LG, Sony, Panasonic, Philips, Sharp, Toshiba, Hisense |
| A/C        | Daikin, Mitsubishi, Fujitsu, LG, Carrier, Gree, Haier, Midea       |
| Fan        | NEC, Hunter, Hampton Bay                          |
| Projector  | NEC, Epson, BenQ                                  |
| DVD/STB    | NEC, Sony                                         |
| Audio      | NEC, Yamaha, Denon                                |

## IR Blaster frequency

38,000 Hz (38 kHz) — standard for NEC, RC5, RC6, Samsung, LG, Panasonic, Daikin.
Sony uses 40 kHz (handled separately in IrPatterns.java).

## Files

```
app/src/main/java/com/redmi/universalremote/
  MainActivity.java   — UI + IR firing logic
  IrPatterns.java     — All raw IR pulse arrays

app/src/main/res/
  layout/activity_main.xml  — UI layout
  values/strings.xml
  values/themes.xml
  values/colors.xml
```
