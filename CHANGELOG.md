# Changelog — LineSpoofer

All notable changes to this project will be documented in this file.

## [Unreleased] — LineSpoofer (Maintained by Code Line)

### Changed
- Rebranded project from SpoofMyDevice to **LineSpoofer**.
- Credited **Code Line** as creator & maintainer.
- Updated app labels and localization strings (`values`, `values-ja`, `values-ko`, `values-zh-rCN`).
- Retained full Material Design (Material 3 Expressive) UI integrity.
- **Milestone 11 (HyperOS & Vector / Modern LSPosed Integration)**:
  - Vector Scope Auto-Check: Added `META-INF/xposed/scope.list` and updated `xposed_scope` string array with `android` (System Framework), enabling automatic scope check in Vector/LSPosed upon module activation (RAM checker `com.trigger_soft.monitor` strictly excluded).
  - Launcher Icon & HyperOS Visibility: Added adaptive launcher icons (`res/mipmap-anydpi-v26/ic_launcher.xml`, `ic_launcher_round.xml`, `ic_launcher_background.xml`, `ic_launcher_foreground.xml`) and configured `android:icon` / `android:roundIcon` in `AndroidManifest.xml`, resolving `icon=0x0` launcher rejection.
  - Anti-Self-Hiding: Removed `my.shanunid.linespoofer` from `PackageManagerHooks.HIDDEN_PACKAGES` so Vector and HyperOS launcher can inspect the package without `NameNotFoundException`.
  - Companion App Activation Status: Implemented `XposedServiceHelper` listener and dual-channel activation hook in `MainActivity.kt` and `MainHook.java` to reflect live active module state.
  - UI Smoothness & Jank Elimination: Removed startup network remote preset fetching in `SpoofAppState.initialize()` and increased `AppIconLoader` LRU cache to 500 with clone app package normalization (`#999`).
- **Milestone 1**: Refactored package name, namespace, and applicationId to `my.shanunid.linespoofer`.
- **Milestone 1**: Moved source directories to `my/shanunid/linespoofer` for main and test suites.
- **Milestone 1**: Updated ContentProvider authority to `my.shanunid.linespoofer.configprovider`.
- **Milestone 1**: Updated IPC broadcast action and internal thread naming to LineSpoofer.
- **Milestone 1**: Updated Xposed asset `xposed_init` to `my.shanunid.linespoofer.MainHook`.
- **Milestone 1**: Renamed application themes and styles to `LineSpoofer`.
- **Milestone 2 (Core Spoofing & Bug Fixes)**: Fixed critical memory overwrite order in `HardwareHooks.java` and restored proportional `availMem` calculations.
- **Milestone 2 (Core Spoofing & Bug Fixes)**: Connected RAM capacity (`getTotalRamBytes()`) and CPU cores (`getCpuCoreCount()`) dynamically to `ConfigManager` instead of hardcoded Pixel 7 Pro specs.
- **Milestone 2 (Core Spoofing & Bug Fixes)**: Implemented full procfs `/proc/cpuinfo` and `/proc/meminfo` interception across Java readers (`BufferedReader`, `FileReader`, `RandomAccessFile`) and shell commands (`ProcessBuilder`, `Runtime.exec`) to prevent real SoC and memory leakage.
- **Milestone 2 (Core Spoofing & Bug Fixes)**: Hardened ABI parsing and fallback in `BuildHooks.java` with safe 64-bit defaults.
- **Milestone 3 (Security & Network Hooks)**: Implemented Developer Options & ADB anti-detection in `SettingsHooks.java` (mocking `adb_enabled` and `development_settings_enabled` to 0).
- **Milestone 3 (Security & Network Hooks)**: Created `NetworkSecurityHooks.java` hiding VPN transports (`TRANSPORT_VPN`), asserting `NET_CAPABILITY_NOT_VPN`, masking proxy settings, and hiding tun/tap virtual interfaces.
- **Milestone 3 (Security & Network Hooks)**: Implemented Play Store installer spoofing in `PackageManagerHooks.java` (`getInstallerPackageName` and Android 11+ `InstallSourceInfo` set to `com.android.vending`).
- **Milestone 4 (Telephony/SIM Lanjutan & Block Local Socket)**: Implemented selective localhost socket scan blocking in `SocketSecurityHooks.java` (throwing `ConnectException` on sensitive reverse-engineering probe ports: Frida 27042/range, GDB, LLDB, IDA, and local proxies).
- **Milestone 4 (Telephony/SIM Lanjutan & Block Local Socket)**: Enhanced `TelephonyHooks.java` with advanced telephony hooks for network type (LTE/5G), connected data state, and non-roaming status.
- **Milestone 4 (Telephony/SIM Lanjutan & Block Local Socket)**: Added on/off toggles for `Block Local Socket / LAN Scan` and `Spoof SIM and Telephony` in `ConfigManager.java` and UI `DeviceSettingsScreen.kt`.
- **Milestone 5 (Profile System & Logger UI)**: Implemented centralized thread-safe, asynchronous file logger `AppLogger.java` with automatic 2 MB log rotation and dual logging to logcat/XposedBridge.
- **Milestone 5 (Profile System & Logger UI)**: Wired `AppLogger` into `ConfigProvider.java`, `MainHook.java`, and `ConfigManager.java` for comprehensive IPC and hook tracking.
- **Milestone 5 (Profile System & Logger UI)**: Built Log Viewer & Diagnostics UI in `DiagnosticsScreen.kt` featuring live log preview, Android Share Sheet export with `FileProvider`, clear log functionality, and refresh controls.
- **Milestone 5 (Profile System & Logger UI)**: Hardened `DevicePresetCatalog.java` with offline-first loading and bundled realistic presets (Pixel 7 Pro, Galaxy S23 Ultra, POCO F6 / malachite, Xiaomi 13), eliminating mandatory GitHub API dependencies.
- **Milestone 5 (Profile System & Logger UI)**: Added offline JSON profile export and import methods in `ConfigFileManager.java`.
- **Milestone 5 (Profile System & Logger UI)**: Added `FileProvider` in `AndroidManifest.xml` and complete localization strings across English, Japanese, Korean, and Simplified Chinese.
- **Milestone 6 (Sensors & Fingerprint Hooks)**: Created `SensorHooks.java` masking emulator/generic sensors to tier-1 vendors (Bosch Sensortec, STMicroelectronics) and injecting natural micro-jitter to Accelerometer, Gyroscope, and Magnetometer streams to defeat bot flat-surface detection.
- **Milestone 6 (Sensors & Fingerprint Hooks)**: Enhanced `WebViewHooks.java` to inject unforgeable WebGL parameters (GL_VENDOR, GL_RENDERER, UNMASKED_VENDOR_WEBGL, UNMASKED_RENDERER_WEBGL) dynamically aligned with the spoofed SoC profile (Adreno / Mali) and subtle micro-variance on canvas data reads.
- **Milestone 7 (Stealth Core & System Identity)**: Created `BatteryHooks.java` spoofing BatteryManager metrics (capacity, discharge rate, status, temperature, voltage) and sticky `ACTION_BATTERY_CHANGED` broadcast intent to eliminate static emulator bot flags.
- **Milestone 7 (Stealth Core & System Identity)**: Created `TimeHooks.java` simulating realistic boot times and elapsed real-time/uptime via `SystemClock` to neutralize zero-uptime telemetry flags.
- **Milestone 7 (Stealth Core & System Identity)**: Created `StorageHooks.java` masking `StatFs` total capacity (256 GB / 512 GB) and available disk blocks to mirror high-end target ROM variants.
- **Milestone 7 (Stealth Core & System Identity)**: Created `AudioHooks.java` spoofing AudioManager HAL sample rates (48 kHz) and buffer sizes.
- **Milestone 7 (Stealth Core & System Identity)**: Added multi-user / clone space awareness (`UserHandle.myUserId()`) in `ConfigProvider.java` and `AppProfileStore.java` to isolate profiles for Dual Apps / Parallel Space / Work Profiles.
- **Milestone 8 (Hardware & Graphics Identity)**: Created `GpuHooks.java` spoofing OpenGL ES 1.0/2.0/3.0 and GL10 renderer strings (GL_RENDERER, GL_VENDOR, GL_VERSION) to match Snapdragon 8s Gen 3 Adreno 735 / Adreno 740 / Mali specs with risk-gated UI switches.
- **Milestone 8 (Hardware & Graphics Identity)**: Enhanced `DisplayHooks.java` with 120Hz display refresh rate override (`getRefreshRate`).
- **Milestone 8 (Hardware & Graphics Identity)**: Created `BluetoothHooks.java` spoofing adapter name and hardware MAC address.
- **Milestone 9 (Network & Location Signals)**: Created `LocationHooks.java` spoofing GPS/Fused locations and stripping `Location.isFromMockProvider()` / `isMock()` flags to defeat mock location detectors in ride-hailing and anti-cheat apps.
- **Milestone 9 (Network & Location Signals)**: Created `WifiHooks.java` spoofing Wi-Fi connection metadata (SSID, BSSID, RSSI, Frequency, MAC address).
- **Milestone 10 (On-Device WebUI & Headless Control)**: Integrated embedded standalone dark-mode Material 3 dashboard (`assets/webui/index.html`) with interactive matrix toggles and live log telemetry.
- **Milestone 10 (On-Device WebUI & Headless Control)**: Created local loopback daemon `WebUiServer.java` (strictly binding to `127.0.0.1:9229`) for headless browser access and KernelSU / APatch WebUI manager support.
- **Milestone 6 (Sensors & Fingerprint Hooks)**: Added user toggles with Material 3 Switches in `DeviceSettingsScreen.kt` and `ConfigManager.java` for `Simulate Natural Sensor Jitter` and `Spoof WebView & WebGL Fingerprint`.
- **Milestone 6 (Sensors & Fingerprint Hooks)**: Registered `SensorHooks.hook(lpparam)` in `MainHook.java` with centralized `AppLogger` tracking.
- **Milestone 6 (Sensors & Fingerprint Hooks)**: Added localization strings for sensor and fingerprint controls across English, Japanese, Korean, and Simplified Chinese.
- **Post-Audit Hardening (Fixes)**:
  - Hardened callback exception handling in `AdvertisingIdHooks.java` and `MediaDrmHooks.java` with defensive try-catches to protect target app stability.
  - Converted `DiagnosticsScreen.kt` log reading to asynchronous background coroutines (`Dispatchers.IO`) with `LaunchedEffect` to prevent UI thread frame drops.
  - Added keep rule for `my.shanunid.linespoofer.data.**` in `app/proguard-rules.pro`.
  - Removed obsolete dead code `BuildSerialHooks.java` (superseded by `BuildHooks.java`).
  - Updated project reference URL in `RealInfoActivity.kt`.
- **Post-Audit Hardening M1–M10 (Fixes)**:
  - Secured risk-gated toggle defaults (`isGpuSpoofEnabled` and `isLocationSpoofEnabled` default to `false` in `ConfigManager.java` to prevent unintended 3D shader crashes or anti-teleport fraud flags).
  - Fixed UI profile persistence in `SpoofAppState.kt` by registering all new toggle keys into `toggleFields` and `allFields`, ensuring UI switches save and reload accurately.
  - Added modern Android 13-16 export-flagged `registerReceiver` overloads in `BatteryHooks.java`.
  - Hardened socket lifecycle in `WebUiServer.java` with `setReuseAddress(true)` and a managed 2-worker `ExecutorService`.
- **Target Device Optimization & Cloud CI/CD (POCO X7)**:
  - Integrated dedicated preset for **POCO X7** (`2412DPC0AG`, codename `malachite`, MediaTek Dimensity 7300-Ultra / `mt6878`, ARM Mali-G615 MC2, 1220x2712 120Hz display, 446 DPI, HyperOS Android 14/15, and SuSFS 2.3.0 GKI synergy).
  - Adjusted `compileSdk` and `targetSdk` to `35` (Android 15 stable target) to guarantee rock-solid build reproducibility.
  - Implemented GitHub Actions Cloud Workflow (`.github/workflows/build.yml`) to compile and publish `LineSpoofer-Debug-APK` artifacts on cloud runners (16 GB RAM, 4 vCPU) with zero host laptop memory overhead.

### Planned
- **M1 (Foundation)**: Refactor package name and unified authorities, establish offline preset catalog and build configuration.
- **M2 (Core Spoofing)**: Fix critical memory calculation bug in `HardwareHooks`, dynamic RAM/CPU profile integration, and procfs CPU interception.
- **M3 (Security & Network)**: Anti-detection hooks for Developer Options / ADB, VPN masking, and Play Store installer spoofing.
- **M4 (Advanced)**: Package manager hiding (hide root & Xposed managers) and advanced SIM parameters.
- **M5 (Profile System & Logger UI)**: Centralized local logging system with one-click log export and offline backup/restore.
- **M6 (Sensors & Fingerprint)**: Dynamic motion sensor simulation and browser fingerprint hooks.

---

## [2.0.1] - 2026-03-23 (Upstream Base: SpoofMyDevice)
- Initial fork base from BuSung-dev/SpoofMyDevice release v2.0.1 (commit `ca78ffa`).
- Legacy Xposed API v82 compatibility.
- Material 3 Expressive UI in Jetpack Compose.

