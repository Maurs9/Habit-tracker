<h1 align="center">Loop Habit Tracker (Enhanced)</h1>

<p align="center">
  <a href="https://github.com/Maurs9/Habit-tracker/releases/latest">
    <img alt="Latest Release" src="https://img.shields.io/github/v/release/Maurs9/Habit-tracker?label=Release&color=0099cc" />
  </a>
  <img alt="Platform" src="https://img.shields.io/badge/Platform-Android%209.0%2B-brightgreen" />
  <a href="LICENSE">
    <img alt="License: GPL v3" src="https://img.shields.io/badge/License-GPLv3-blue.svg" />
  </a>
</p>

<p align="center">
  An enhanced, open-source habit tracking app for Android designed to help you create and maintain positive long-term habits.
  <br>
  <strong>Completely ad-free, tracker-free, and 100% offline.</strong>
</p>

<p align="center">
  <a href="https://github.com/Maurs9/Habit-tracker/releases/latest">
    <img src="https://img.shields.io/badge/📥%20Download-Latest%20APK-2ea44f?style=for-the-badge&logo=android&logoColor=white" alt="Download APK" />
  </a>
</p>

---

## 🌟 What's New in this Version

* **📁 Habit Sections**: Organize your daily routines by grouping habits into custom sections (e.g., Morning Routine, Health & Fitness, Evening & Learning). Customize section order, collapse or expand sections on the main list, and reassign habits with ease.
* **🎯 Full Frequency Parity for Measurable Habits**: Measurable habits now support the complete flexible frequency system previously exclusive to yes/no habits (e.g., 3 times per week, 5 times per month, every 2 days). Qualifying rest days display clean auto-completion indicators (`YES_AUTO`), streak calculation seamlessly bridges rest days, and habit score calculations dynamically reflect your target rate.
* **🎨 4-Ring Donut Color Wheel Picker**: Replaced the legacy flat color grid with an intuitive 4-ring non-overlapping color wheel based on harmonic color theory:
  * **12 Radial Hue Sectors** (spaced 30° apart across the color spectrum).
  * **4 Concentric Rings**: Outer (Deep tones), Middle-outer (Vibrant tones), Middle-inner (Soft tones), and Inner (Pastel tones).
  * **Center Hub**: 4 distinct neutral swatches (Light Gray, Gray, Slate, Charcoal).
  * High-contrast 52-color palette calibrated for both Light and Dark themes.
* **🏷️ Interactive Tag Picker**: Easily assign and manage tags with a multi-selection checklist dialog. Filter your habits and save custom views.
* **⏰ Multiple Daily Reminders**: Schedule multiple notification times for any habit across selected days of the week, with integrated snooze support directly from the notification shade.

---

## 📱 Screenshots

[![Main screen](screenshots/1.thumb.png)](screenshots/1.png)
[![Edit habit](screenshots/2.thumb.png)](screenshots/2.png)
[![Color picker](screenshots/3.thumb.png)](screenshots/3.png)
[![Night mode](screenshots/4.thumb.png)](screenshots/4.png)
[![Habit statistics](screenshots/5.thumb.png)](screenshots/5.png)
[![Home screen widgets](screenshots/6.thumb.png)](screenshots/6.png)

---

## ✨ Key Features

* **Minimalist & Fast**: Clean, modern interface optimized for speed and battery life without bloat.
* **Advanced Habit Scoring**: Scientifically-derived scoring formula calculates habit strength over time. Missing a single day won't destroy weeks of progress.
* **Flexible Schedules**: Daily habits, weekly quotas (e.g., 3 times per week), or repeat intervals (e.g., every 2 days).
* **Measurable Habits**: Track numerical targets (e.g., cups of water, pages read, workout minutes) in addition to yes/no habits.
* **Interactive Home Screen Widgets**: Check off habits or monitor scores directly from your Android home screen.
* **Total Privacy & Offline-First**: No accounts, no cloud dependencies, no analytics. Your data remains strictly on your device.
* **Export & Import Data**: Export your complete history anytime to CSV or raw SQLite database backups.

---

## 📥 Installation

Because this enhanced edition contains custom features (Sections, 4-Ring Color Wheel, Measurable Habit Frequencies, Tag Picker) not present in upstream app stores, install the APK directly from GitHub:

1. Go to the [**Releases Page**](https://github.com/Maurs9/Habit-tracker/releases/latest).
2. Download the latest `uhabits-v...apk` file.
3. On your Android device, tap the downloaded APK to install.
   *(If prompted, allow your browser or file manager permission to "Install unknown apps").*

> [!NOTE]
> The upstream version of Loop Habit Tracker is available on Google Play and F-Droid, but it does not include the custom Habit Sections, Measurable Habit Frequencies, Tag Picker dialog, or the Donut Color Wheel introduced in this repository.

---

## 🛠️ Building from Source

### Prerequisites
* **Java Development Kit (JDK)**: 17 or higher
* **Android SDK**: API level 36 (Android 16 / Build-tools 36.x)
* **Minimum Supported Device**: Android 9.0 (API level 28)

### Build Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/Maurs9/Habit-tracker.git
   cd Habit-tracker
   ```

2. Build the debug APK:
   ```bash
   # On Windows:
   .\gradlew.bat :uhabits-android:assembleDebug

   # On Linux / macOS:
   ./gradlew :uhabits-android:assembleDebug
   ```

3. The generated APK will be available at:
   ```
   uhabits-android/build/outputs/apk/debug/uhabits-android-debug.apk
   ```

4. Run unit tests:
   ```bash
   .\gradlew.bat :uhabits-core:test
   ```

---

## 📄 License & Credits

* Based on [Loop Habit Tracker](https://github.com/iSoron/uhabits) by Álinson Santos Xavier.
* Licensed under the [GNU General Public License v3.0 (GPLv3)](LICENSE).
