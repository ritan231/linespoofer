# Changelog — LineSpoofer

All notable changes to this project will be documented in this file.

## [Unreleased] — LineSpoofer (Maintained by Code Line)

### Changed
- Rebranded project from SpoofMyDevice to **LineSpoofer**.
- Credited **Code Line** as creator & maintainer.
- Updated app labels and localization strings (`values`, `values-ja`, `values-ko`, `values-zh-rCN`).
- Retained full Material Design (Material 3 Expressive) UI integrity.
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

