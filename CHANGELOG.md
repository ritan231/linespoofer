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

