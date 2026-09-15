# Build the project

This page describes how to download and build the app from source. If you are having trouble building the project, please open an issue with the failing command and its output (without credentials).

The project uses **JDK 17**, the checked-in **Gradle 8.11.1 wrapper**, and **Android SDK platform 36**. The app supports devices running **Android 9 (API 28) or later**. Build requirements and the minimum supported device version are different.

## Contents

* [Build using Android Studio](#build-using-android-studio)
* [Build from the command line](#build-from-the-command-line)

## Build using Android Studio

### Step 1: Install git

The package `git` is required for downloading the source code of the app and submitting changes GitHub. Please see [the git book](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git) for further instructions. If you are planning to submit pull requests in the future, it is recommended to [generate and configure your SSH keys](https://help.github.com/en/github/authenticating-to-github/generating-a-new-ssh-key-and-adding-it-to-the-ssh-agent).

### Step 2: Download and install Android Studio

Although Android Studio can be downloaded [from their official website](https://developer.android.com/studio/), a much better option is to install it through [JetBrains Toolbox](https://www.jetbrains.com/toolbox-app/). This tool, developed by the same developers of Android Studio, allows you to easily upgrade and downgrade the IDE, or switch between stable, beta and canary versions. After downloading and installing JetBrains Toolbox, simply click the install button near Android Studio to install the newest stable version of IDE. Beta and canary versions have not been tested and may not work correctly.

After installation, launch Android Studio and complete the setup wizard. Use a stable version that supports Android Gradle Plugin 8.9.2. In **Settings > Build, Execution, Deployment > Build Tools > Gradle**, select a **JDK 17** installation as the Gradle JDK. Do not assume that a newer bundled IDE JDK is the same as the project's configured Java toolchain.

In **Tools > SDK Manager**, install Android SDK Platform 36, Android SDK Build-Tools 35.0.0 (the default used by the pinned Android Gradle Plugin), Android SDK Platform-Tools, and Android SDK Command-line Tools (latest). Install Android Emulator and a compatible system image if you intend to use an emulator.

### Step 3: Download the source code

To create a complete copy of the source code repository, open the terminal (Linux/macOS) or Git Bash (Windows), navigate to the desired folder, then run:
```bash
git clone https://github.com/iSoron/uhabits.git
```
The repository will be downloaded to the directory `uhabits`.

### Step 4: Open and run the project on Android Studio

1. Launch Android Studio and select "Open an existing Android Studio project".
2. When the IDE asks you for the project location, select `uhabits` and click "Ok".
3. Android Studio will spend some time indexing the project. When this is complete, click the toolbar icon "Sync Project with Gradle File", located near the right corner of the top toolbar.
4. If Gradle reports missing SDK components, install them using SDK Manager and sync again.
5. To run the application, create a virtual Android device using **Tools > Device Manager**, with API 28 or later. Automated screenshot tests have stricter device requirements; see [Testing the project](TEST.md).
6. Click the menu "Run" and "uhabits-android". The application should launch.


## Build from the command line

The package-manager examples below are for Ubuntu/Debian. The Gradle commands also work on macOS. On Windows, use `gradlew.bat` instead of `./gradlew` in PowerShell or Command Prompt.

### Step 1: Install basic packages

Install Git and JDK 17. On Ubuntu/Debian:

```bash
sudo apt-get update
sudo apt-get install -y git openjdk-17-jdk-headless
```

On macOS or Windows, install a JDK 17 distribution, such as [Eclipse Temurin](https://adoptium.net/temurin/releases/?version=17), and Git for your platform. Set `JAVA_HOME` to the JDK installation directory and add its `bin` directory to `PATH`. On macOS, a registered JDK can be selected with `export JAVA_HOME=$(/usr/libexec/java_home -v 17)`.

Check `java -version` and, after cloning, `./gradlew --version`. Both should use Java 17. A separate global Gradle installation is not required.

### Step 2: Install Android SDK tools

The Android SDK tools contains many necessary tools for developing and debugging Android applications. It can be obtained as part of Android Studio, but, for simple command line usage, it can also be downloaded individually.

1. Download the current **Command line tools only** archive for your operating system from <https://developer.android.com/studio/#command-tools>. Extract its contents into `ANDROID_HOME/cmdline-tools/latest`, so that `cmdline-tools/latest/bin/sdkmanager` exists beneath the SDK root. Do not use the obsolete `sdk-tools-*-4333796.zip` package.

2. Set the SDK location in your shell profile. This example uses a user-writable Linux SDK directory; use your actual SDK directory if Android Studio already installed one:
```bash
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
```

   The usual Android Studio SDK directory is `$HOME/Library/Android/sdk` on macOS and `%LOCALAPPDATA%\Android\Sdk` on Windows. On Windows, set `ANDROID_HOME` and the corresponding `PATH` entries in Environment Variables, and use `sdkmanager.bat`. Restart your terminal after editing its profile or environment. Alternatively, set `sdk.dir=/absolute/path/to/sdk` in the untracked `local.properties` file for Gradle; `build.sh` still needs `ANDROID_HOME`.

3. Review and accept the Android SDK licenses, then install the build prerequisites:
```bash
sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-36" "build-tools;35.0.0"
```

   Building APKs and running JVM tests do not require an emulator. For instrumented tests, also install `emulator` and the system images described in [Testing the project](TEST.md).

### Step 3: Download the source code

To create a complete copy of the source code repository, navigate to your home directory and run:
```bash
git clone https://github.com/iSoron/uhabits.git
```
The repository will be downloaded to the directory `uhabits`.

### Step 4: Compile the source code

1. Navigate to the directory `uhabits`
2. Run `./gradlew :uhabits-android:assembleDebug --stacktrace`

If the compilation is successful, a debug APK will be generated somewhere inside the folder `uhabits-android/build/`. Currently, the full path is `./uhabits-android/build/outputs/apk/debug/uhabits-android-debug.apk`, but it may change in the future.

Install the APK on a connected device or running emulator with:

```bash
adb install -r uhabits-android/build/outputs/apk/debug/uhabits-android-debug.apk
```

Gradle downloads dependencies over HTTPS on its first run. If certificate validation fails on a managed network, configure your JDK to trust your organization's approved certificate chain; do not disable TLS or certificate validation.
