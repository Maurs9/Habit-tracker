# Testing the project

Loop Habit Tracker has automated tests to reduce the chance of bugs being silently introduced. Set up **JDK 17 and Android SDK 36** using [the build guide](BUILD.md) first. Tests fall into two categories:

- **Unit tests:** These tests run very quickly on the developer's computer, inside a JVM, and do not need an Android emulator or device. They typically test the correctness of core functions of the application, such as the computation of scores and streaks.
- **Instrumented tests:** These tests require an Android emulator or device. _Medium_ instrumented tests are still quite fast to run, since only individual classes are tested. The app itself does not need to be launched. Examples include _view tests_, which render our custom views on the device and compare them against prerendered images. _Large_ instrumented tests launch the application on an Android emulator and interact with it by touching the screen, much like a regular user.

## Running unit tests

Run both the Kotlin multiplatform core JVM tests and the Android module's local JVM tests explicitly:

```bash
./gradlew :uhabits-core:jvmTest :uhabits-android:testDebugUnitTest
```

No emulator is needed. Android Studio also supports running an individual test class or method. To run focused core model checks:

```bash
./gradlew :uhabits-core:jvmTest \
  --tests '*EntryListTest' \
  --tests '*RecomputeDeterminismTest' \
  --tests '*ScoreListTest' \
  --tests '*StreakListTest'
```

Run `./gradlew ktlintCheck` for Kotlin style checks. CI's `./build.sh build` additionally runs Android lint, the core build/tests, and assembles the app and instrumentation APKs. That script does **not** run Android JVM unit tests; CI runs `:uhabits-android:testDebugUnitTest` separately. Test reports are in each module's `build/reports/tests` directory.

## Running instrumented tests

The `build.sh` script runs medium and large tests on Linux with hardware-accelerated **x86_64 Google APIs** emulator images. It needs Bash, GNU `getopt`, `flock`, `timeout`, `ts` (from `moreutils`), Python 3, and the Android command-line tools, platform-tools, and emulator under `ANDROID_HOME`. On Ubuntu/Debian, the non-SDK tools are available through `util-linux`, `coreutils`, `moreutils`, and `python3`. The script assumes a dedicated test runner; its setup command deletes and recreates the named test AVD.

1. Run `./build.sh android-setup API` to provision the `uhabitsTestAPI` emulator and its `fresh-install` snapshot, where `API` is the desired API level.
2. Run `./build.sh build` to assemble the APKs.
3. Run `./build.sh android-tests API` to run the tests on a single API.
4. Run `./build.sh android-tests-parallel API API...` to run the tests on multiple APIs in parallel, subject to the machine's RAM and CPU capacity.

The self-hosted CI runner must have AVDs provisioned for **28, 29, 30, 31, 32, 33, 34, 35, and 36** before the workflow runs. CI retains the original six-API group and runs 31/35/36 in a separate, bounded three-emulator compatibility group, rather than starting nine emulators at once. New runners need the setup command for each API. These tests cover both the API 28 minimum and current target platform.

On macOS (especially Apple Silicon) or Windows, prefer Android Studio's Device Manager and test runner, or configure an emulator of the matching host architecture and use `./gradlew :uhabits-android:connectedDebugAndroidTest` (`gradlew.bat` on Windows). The Linux automation script is not a portable emulator launcher. The screenshot/device assumptions below still apply.

Note that instrumented tests are designed to run on a clean install, inside an emulator. They will not work on actual devices. All tests are also designed for a particular screen size, namely the Nexus 4 configuration (4.7" 768x1280 xhdpi), and a particular locale, namely English (US). Furthermore:

- No additional apps should be installed on the device;
- The homescreen must look exactly like it was when the emulator was originally created, with no additional icons or widgets;
- All animations must be disabled. `build.sh` disables the window, transition, and animator scales; disable them yourself when launching an emulator outside the script.

If there are failing view tests (that is, if some custom views do not render exactly like the prerendered images we have), then both the actual and expected images will be automatically downloaded from the device to the folder `uhabits-android/build/outputs`. After verifying the differences, if you feel that the actual images are actually fine and should replace the prerendered ones, then run `./build.sh android-accept-images`.

## Profiling full-history recomputation

The opt-in JVM harness uses the existing test runtime; it adds no benchmarking dependency and has **no wall-clock pass/fail threshold**:

```bash
./gradlew :uhabits-core:profileRecomputation \
  -Pprofile.warmups=5 -Pprofile.samples=20 -Pprofile.calls=3
```

Normal `jvmTest` and `build` runs exclude this harness. Each explicit profiling invocation executes again, rather than reusing cached results. Artifacts are written to `uhabits-core/build/reports/profile-recomputation/`:

- `environment.txt`: JVM, OS, architecture, heap, and run parameters.
- `results.csv`: fixture creation/initialization time separately from per-operation minimum, median, p95, and mean nanoseconds per call. Percentiles describe batch averages, not individual-call tail latency.

The deterministic fixtures cover 1, 5, and 10 years (365 days per year, ending January 1, 2025), daily/3-per-week/5-per-month schedules, boolean and both numerical target types, with and without skips. They also include unknown entries and notes. All 54 scenarios measure `getKnown`, entry recomputation, scores, streaks, and all three recomputation stages together. Fixtures are prepared outside timed operations; each operation has separate warmup and measured batches. Result consumption and CSV output are outside those batches.

Use the same JDK, hardware, heap, parameters, and otherwise-idle machine for comparisons. Save artifacts before another run overwrites them and repeat in fresh JVMs. This lightweight harness includes JVM allocation and garbage-collection effects, but does not measure allocation counts, database I/O, Android UI latency, or an end-to-end `Habit.recompute()` call. Use an allocation profiler or Android device profiling for those questions.

`RecomputeDeterminismTest` exercises the same full histories with shuffled insertion order and repeated recomputation. `EntryListTest` also compares ordering against the previous ascending-sort-plus-reverse implementation. Neither these correctness checks nor the existence of a profiling task establishes a latency improvement for full recomputation. No historical cutoff or incremental algorithm is introduced.

### Backup and import regression tests

Run the database tests on both API 28 (SQLite 3.22) and a current emulator:

```sh
./gradlew :uhabits-android:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=org.isoron.uhabits.database.DatabaseSnapshotTest,org.isoron.uhabits.database.AutoBackupTest,org.isoron.uhabits.database.BackupPreviewTest,org.isoron.uhabits.activities.settings.BackupViewModelTest
```

Snapshots use a no-write exclusive SQLite transaction, not `VACUUM INTO` or an
unlocked file copy. The actual journal mode must be DELETE, TRUNCATE or PERSIST;
WAL and nested transactions are rejected. The file is synced and validated before
an atomic same-directory rename. Only recognized backup filenames are pruned, and
only after a successful verified backup. Failed attempts do not replace the last
success status.

In Settings, exporting a database saves habit data (including habit reminders),
not app preferences. CSV exports are for spreadsheets and cannot be restored.
Use **Review local backups** to access retained backups without exposing the
app-private storage directory to a file picker. Import first validates a private
staged file and shows habit/entry counts and the
latest entry date for Loop databases. This date is not the backup creation date.
Confirmation preserves the existing merge-by-UUID behavior: matching habit details
and entries can be overwritten, while other habits remain. Canceling the picker
or preview returns to Settings without importing. Also verify rotation while
previewing, unavailable document providers, and a full destination volume.

A successful import with failed list/widget/reminder refresh shows a separate
warning that the data was imported and should not be imported again. Remaining
refresh operations are still attempted; fatal errors are not converted into
recoverable warnings.
