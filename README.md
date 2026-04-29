# NetGuardPro Mobile

Android cybersecurity suite with VPN, Firewall, DNS Filtering, and Device Cleaner.

## Features

- **VPN Protection** - WireGuard-based VPN with server selection, connection stats, and history
- **Firewall** - Per-app network access control for WiFi and mobile data
- **DNS Filtering** - Ad, tracker, malware, and phishing domain blocking with customizable blocklists
- **Device Cleaner** - Cache, APK, large file, and temp file scanning and cleaning

## Tech Stack

- Kotlin 1.9.22
- Jetpack Compose with Material 3
- Navigation Compose
- Room Database
- DataStore Preferences
- WireGuard Tunnel Library
- MVVM Architecture

## Build

```bash
./gradlew assembleDebug
```

Requires Android SDK with compileSdk 34 and minSdk 26.

## Package

`com.netguardpro.mobile`

---

### Method

Android port of the NetGuardPro suite. Built over **3 days in late March 2026** with **Claude (Opus 4.6)** as paired implementation and audit partner. Each commit cross-audited: code review, dependency check, security pass on the WireGuard tunnel and per-app firewall layer.

---
