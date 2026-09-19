<h1 align="center">Loop Habit Tracker (Enhanced Edition)</h1>

<p align="center">
  <a href="https://github.com/Maurs9/Habit-tracker/releases/latest">
    <img alt="Latest Release" src="https://img.shields.io/github/v/release/Maurs9/Habit-tracker?label=Release&color=0099cc" />
  </a>
  <img alt="Platform" src="https://img.shields.io/badge/Platform-Android%209.0%2B-brightgreen" />
  <a href="LICENSE.txt">
    <img alt="License: GPL v3" src="https://img.shields.io/badge/License-GPLv3-blue.svg" />
  </a>
</p>

<p align="center">
  An enhanced, modern fork of <a href="https://github.com/iSoron/uhabits">Loop Habit Tracker</a> designed for superior organization, advanced scheduling, and data safety.
  <br>
  <strong>100% Offline • Completely Ad-Free • Zero Trackers • Open Source</strong>
</p>

<p align="center">
  <a href="https://github.com/Maurs9/Habit-tracker/releases/latest">
    <img src="https://img.shields.io/badge/📥%20Download-Latest%20APK-2ea44f?style=for-the-badge&logo=android&logoColor=white" alt="Download APK" />
  </a>
</p>

---

## ⚖️ Upstream vs. Enhanced Edition

| Feature / Area | Base App (`iSoron/uhabits`) | Enhanced Edition (`Maurs9/Habit-tracker`) |
| :--- | :--- | :--- |
| **Habit Organization** | Flat list only; comma-separated text tags | **Habit Sections** with collapsible headers, live completion counts, and an interactive **Tag Picker dialog** |
| **Habit Reordering** | Manual position numbers or static sorting | **Smooth long-press drag-and-drop** reordering within sections with atomic SQLite persistence |
| **Measurable Habits** | Daily targets only; fractional frequencies unsupported | **Full frequency parity** (e.g., 3x/week, 5x/month) with automatic rest-day checkmarks (`YES_AUTO`) |
| **Color System** | Basic 19-color flat picker | **4-Ring Donut Wheel (52 slots)** across Deep, Vibrant, Soft, and Pastel tones; calibrated for Light, Dark & AMOLED |
| **Reminders** | 1 notification per habit | **Multiple daily reminders** per habit across selected weekdays with snooze picker actions |
| **Startup & Speed** | Synchronous UI-thread database loading (risk of launch freezes / ANRs) | **Background Startup Coordinator** offloading migrations and history computation behind lifecycle-safe gates |
| **Accessibility** | Canvas charts unannounced to screen readers | **Full TalkBack exploration** across all 5 charts, keyboard navigation, and descriptive action labels |
| **Data Safety & Math** | Skips collided with `0.003` measurements; "at most" completion inconsistencies | **Schema 29 & 30 migrations**, atomic entry updates, wide-precision math, and accurate "at most" scoring |
| **Main View Density** | Standard spacing only | **Streamlined compact layout** (1dp spacing, 48dp touch targets) and subtle **"Today" column highlight** |

---

## 🌟 Key Enhancements

### 📁 1. Habit Sections & Interactive Tags
* **Group by Routine**: Organize habits into custom sections (*Morning Routine*, *Work & Focus*, *Evening Wind-Down*).
* **Live Progress Badges**: Section headers display live *done / total* progress counts.
* **Tag Checklist**: Multi-select tag checklist dialog with instant tag creation replaces comma-separated text inputs.

### 🎯 2. Measurable Habits with Full Scheduling
* **Target Scheduling**: Track numerical goals on fractional frequencies (e.g., gym 4x/week, read 10 pages every 2 days).
* **Automatic Rest Days**: Non-scheduled days display outline checkmarks (`YES_AUTO`) without hurting scores or streaks.
* **At-Most Precision**: Habits with limits (e.g., caffeine, screentime) cleanly register as completed whenever within limits.
* **Focused Check-in**: Clean entry dialogs show only the active target or limit alongside notes and quick actions.

### 🎨 3. 52-Color Harmonic Wheel & AMOLED Theming
* **4 Concentric Tone Rings**: Choose from 52 colors structured into Deep, Vibrant, Soft, and Pastel tones, plus neutral swatches.
* **High Contrast Everywhere**: Single readable color harmonization across habit titles, score rings, and buttons in Light, Dark, and Pure Black AMOLED themes.
* **Visual Anchor**: Subtle vertical highlight on the "Today" column keeps your current check-in column clear.

### ⚡ 4. Background Startup & Rock-Solid Safety
* **Zero-Lag Cold Starts**: Database migrations and history calculations run off the UI thread via `StartupCoordinator`.
* **Atomic Persistence**: Fail-safe database writes and atomic entry replacement protect historical data from corruption.
* **Schema Evolution**: Database version 29 separates skip markers from measurements; database version 30 remaps colors onto the 52-slot harmonic palette.

### ♿ 5. Accessible & Keyboard-Ready
* **TalkBack Chart Exploration**: Audio exploration across Frequency, Score, History, Streaks, and Target charts.
* **Physical Keyboard Navigation**: Navigate dates and reorder habits via standard hardware keyboard shortcuts.

---

## 📱 Screenshots

[![Main screen](screenshots/1.thumb.png)](screenshots/1.png)
[![Edit habit](screenshots/2.thumb.png)](screenshots/2.png)
[![Color picker](screenshots/3.thumb.png)](screenshots/3.png)
[![Night mode](screenshots/4.thumb.png)](screenshots/4.png)
[![Habit statistics](screenshots/5.thumb.png)](screenshots/5.png)
[![Home screen widgets](screenshots/6.thumb.png)](screenshots/6.png)

---

## 📥 Installation

Install the APK directly from GitHub Releases:

1. Open the [**Releases Page**](https://github.com/Maurs9/Habit-tracker/releases/latest).
2. Download the latest `uhabits-v...apk` file.
3. Tap the file on your Android device to install (grant *"Install unknown apps"* if prompted).

> [!NOTE]
> The upstream version of Loop Habit Tracker is available on Google Play and F-Droid, but it does **not** include Habit Sections, the 52-color Donut Wheel, Measurable Frequencies, the Tag Picker dialog, or the performance and safety improvements of this edition.

---

## 🛠️ Building from Source

### Prerequisites
* **Java Development Kit (JDK)**: 17
* **Android SDK**: API level 36 (Android 16), Build-Tools 35.0.0
* **Minimum Supported Device**: Android 9.0 (API level 28)

### Build Commands

```bash
# Clone the repository
git clone https://github.com/Maurs9/Habit-tracker.git
cd Habit-tracker

# Build debug APK (Windows)
.\gradlew.bat :uhabits-android:assembleDebug

# Build debug APK (Linux / macOS)
./gradlew :uhabits-android:assembleDebug

# Run test suite
.\gradlew.bat :uhabits-core:jvmTest :uhabits-android:testDebugUnitTest
```

The compiled APK will be at:
`uhabits-android/build/outputs/apk/debug/uhabits-android-debug.apk`

---

## 📄 License & Credits

* Based on [Loop Habit Tracker](https://github.com/iSoron/uhabits) by Álinson Santos Xavier.
* Licensed under the [GNU General Public License v3.0 (GPLv3)](LICENSE.txt).
