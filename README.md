# LineSpoofer

LineSpoofer is an advanced Xposed module and companion application for constructing, managing, and applying spoofed Android device profiles to targeted applications.

Maintained and developed by **Code Line**. Forked from SpoofMyDevice v2.0.1 (originally developed by BuSung-dev).

The companion UI is built with Jetpack Compose using Material 3 Expressive components, maintaining full Material Design compliance.

## Features

- Reusable preset-based and fully editable custom profiles
- Independent app-to-profile assignments
- Dedicated profile management and per-app profile assignment screens
- Live profile propagation to already hooked scoped apps
- Per-field enable controls and one-tap randomization for advanced identifiers
- Safe Mode that keeps real `Build.VERSION` and `ro.*.build.version.*` values for selected apps
- Model, fingerprint, telephony, and identifier hooks remain enabled in Safe Mode
- Android System Framework is always excluded from hooking
- Optional resolution, density, window metrics, and configuration spoofing
- Locale, timezone, Build ID, and Display ID spoofing
- English, Korean, Japanese, and Simplified Chinese UI localization
- Runtime diagnostics and app information screens
- Rooted LSPosed and non-root patch-manager workflows

## Requirements

- Android 8.0 (API 26) up to Android 16 (API 36) / Android 17 (API 37)
- One of the following module runtimes:
  - LSPosed on a rooted device (KernelSU, Magisk, APatch)
  - A non-root patch manager implementing the Xposed API (LSPatch / NPatch)

## Rooted LSPosed Setup

1. Install the LineSpoofer APK and open it once.
2. Enable LineSpoofer in LSPosed.
3. Add only the target apps to the module scope. Do not select Android System Framework.
4. Open **Profiles**, create a profile, then select a preset or edit its custom values and save it.
5. Open **Apps**, select a target app, then choose one of the saved profiles in its assignment screen.
6. Force stop and relaunch newly assigned target apps.

## Non-root Setup

1. Install the LineSpoofer APK normally and open it once.
2. In LSPatch or NPatch, register LineSpoofer as the Xposed module for the target APK.
3. Patch the target APK and install the patched result.
4. Create and save a profile in **Profiles**, then open **Apps**, select the patched app, and assign that profile.
5. Relaunch the patched target app.

## Safe Mode

Safe Mode exists to prevent crashes when the spoofed profile reports a newer Android version than the physical device. For selected apps it bypasses only:
- `Build.VERSION` fields
- `ro.*.build.version.*` properties

Model, fingerprint, network, telephony, and identifier spoofing still applies. Android System Framework is never hooked, regardless of Safe Mode selection.

## Maintainer & Credits

- **Creator & Maintainer**: Code Line
- **Original Base**: SpoofMyDevice by BuSung-dev
- **Community Credits**: LSPosed and Xposed communities

## Disclaimer

This project is intended for research, testing, privacy auditing, and authorized security evaluation.
