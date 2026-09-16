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

### Palette and core chart checks

`PaletteContrastTest` checks all 40 slots using WCAG relative luminance: text
on light/dark/pure-black cards, white on light fills, and `#212121` on widget
fills must reach 4.5:1. Text greys must reach 4.5:1 and inactive marks 3:1.
Widget alpha is composited onto its `#303030` card. Every hue must keep Deep
darker than Vibrant and Muted lower in OKLab chroma in both light and dark.
`PaletteColorTest` checks theme/export consistency and the default blue slot.

Core chart failures write actual, expected, and diff PNGs to
`uhabits-core/build/failed/views/`. Inspect all affected variants before copying
actuals into `uhabits-core/assets/test/views/`; never accept images solely to
make tests pass. CSV exporter mismatches also save the generated CSV under
`uhabits-core/build/failed/csv_export/` for comparison with its fixture.

Instrumented `PaletteConsistencyTest`, `PaletteRenderingTest`,
`HabitControlDialogsTest`, and `EditHabitActivityTest` cover color names,
Android greys, foreground contrast, toolbars, notification tints, wheel virtual
accessibility/keyboard/touch selection, confirmation/cancel, and remembered
creation colors. Compiling them does not replace running them on a device.

### Today column, progress subtitle, and filter cue

`HabitTest` and `HabitCardListCacheTest` cover the shared completion rule and
cached, filtered counts: manual/automatic checks, yes/no skips, numeric targets,
unknowns, toggles, and empty lists. Numerical "at most" remains never completed.

Device tests in `activities/habits/list/` assert subtitle text without golden
images (`ListHabitsRootViewTest`) and active/reset filter icon colors
(`ListHabitsMenuTest`). Under `views/`, panel tests assert today's flag across
offsets, reversal, and RTL; `TodayHighlightTest` checks shared tint and clearing
in all three themes. `HeaderViewTest` checks the inset, reversal/RTL, and removal
of tint at older offsets without extra drawer preference reads.

Run these on the configured test emulator:

```bash
./gradlew :uhabits-android:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=org.isoron.uhabits.activities.habits.list.ListHabitsRootViewTest,org.isoron.uhabits.activities.habits.list.ListHabitsMenuTest,org.isoron.uhabits.activities.habits.list.views.TodayHighlightTest,org.isoron.uhabits.activities.habits.list.views.HeaderViewTest,org.isoron.uhabits.activities.habits.list.views.EntryPanelViewTest,org.isoron.uhabits.activities.habits.list.views.NumberPanelViewTest,org.isoron.uhabits.activities.habits.list.views.EntryButtonViewTest,org.isoron.uhabits.activities.habits.list.views.NumberButtonViewTest
```

Device execution and reviewed golden updates remain deferred. Affected paths
relative to `uhabits-android/src/androidTest/assets/views/habits/list/`:

- New: `CheckmarkButtonView/render_today.png`, `NumberButtonView/render_today.png`.
- Invalidated: `HeaderView/render.png`, `HeaderView/render_reverse.png`,
  `CheckmarkPanelView/render.png`, `NumberPanelView/render.png`,
  `HabitCardView/render.png`, `HabitCardView/render_changed.png`,
  `HabitCardView/render_numerical.png`, `HabitCardView/render_selected.png`.

Do not create placeholder PNGs or accept unreviewed outputs. Verify the header
and card tint align at the existing 3dp outer inset in light, dark, and
pure-black, including reversed/RTL layouts, and check toolbar subtitle
truncation at narrow widths and larger system fonts.

`ColorContrastTest` checks adjusted foregrounds across all 40 colors and three
themes, including tinted selected cards and rendering-rounding margins.
`TodayHighlightTest` checks them against rendered Android backgrounds: text
must reach 4.5:1 and inactive marks 3:1. Today keeps its 6% tint; saved colors
and the base palette are unchanged. Device execution remains deferred.

### Detail, editor, and widget pickers

`SubtitleCardViewTest` covers the hidden organization row, section/tag visibility
changes, and `habits/show/SubtitleCard/render_tags.png`.
`TagPickerDialogTest`, `EditHabitActivityTest`, and `EditHabitSectionTest` cover
checklist selection, case-insensitive reuse, validation, cancellation, clearing,
and draft/result delivery after rotation.

Editor tests cover direct-create defaults, both type drafts, numerical
validation, hidden type controls for existing habits, More options defaults and
rotation, and reminder/section preservation. Theme tests exercise type-button
and tag-dialog inflation in light, dark, and pure-black modes. Acceptance tests
use the direct editor flow and scroll to folded fields.

`HabitPickerDialogTest` covers multi-selection, Save/Cancel, list-order
persistence, section shortcuts, widget type restrictions, rotation, and recovery
when selected habits disappear or become ineligible. Verify the resulting stack
updates after changing a habit in the app.

These device tests are compiled, not executed. APK assembly and compilation
alone do not establish visual, accessibility, or interaction correctness.

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

The Linux wrapper pulls failed screenshots into `uhabits-android/build/outputs`.
Running Gradle directly does not perform that pull. Never accept images just
to make tests pass.

### Reviewing screenshot baselines on macOS

Use a disposable, clean emulator matching the Nexus 4 size, density, locale,
and disabled-animation settings above. On Apple Silicon, use an ARM64 image.
Select its serial with `adb devices`; the example below uses `emulator-5554`.

```bash
./gradlew :uhabits-android:connectedDebugAndroidTest
mkdir -p uhabits-android/build/outputs/golden-review
adb -s emulator-5554 pull \
  /sdcard/Android/data/org.isoron.uhabits/files/test-screenshots \
  uhabits-android/build/outputs/golden-review/
```

Use the actual path printed by a failing `BaseViewTest`: older writable-storage
devices may use `/sdcard/test-screenshots` instead. A mismatched image writes an
actual PNG and an `.expected.png`; a missing baseline writes only the actual.

Inspect each actual beside its expected image and diff. Fix rendering defects
before accepting anything. Copy only individually approved actuals into matching
paths under `uhabits-android/src/androidTest/assets/`; do not copy expected images
or bulk-sync unreviewed captures. For example, after approving this image:

```bash
cp uhabits-android/build/outputs/golden-review/test-screenshots/views/habits/list/HeaderView/render.png \
  uhabits-android/src/androidTest/assets/views/habits/list/HeaderView/render.png
./gradlew :uhabits-android:connectedDebugAndroidTest
```

Create the corresponding asset subdirectory for a new baseline. The second full
run must pass. The palette changes require review across all existing `common/`,
`habits/`, and `widgets/` view groups; `CanvasTest.png` is not affected.
These five new baselines still require capture and review, relative to `assets/views/`:

- `habits/list/CheckmarkButtonView/render_today.png`
- `habits/list/NumberButtonView/render_today.png`
- `habits/list/SectionHeaderView/render.png`
- `habits/list/SectionHeaderView/render_other.png`
- `habits/show/SubtitleCard/render_tags.png`

Run the schema-sensitive suites below and the acceptance, color picker, editor,
subtitle, grouped-list, tag-picker, and widget-picker suites explicitly as well.
Manually check all three themes, reversed/RTL lists, large fonts, section
assignment/deletion and drag limits, picker rotation, stack updates, and a
reminder notification. This verification and Android golden acceptance remain
deferred; no replacement PNGs are supplied without device rendering.

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

### Sections and grouping

`HabitCardListCacheGroupingTest` checks stable section partitioning across all
primary/secondary sorts, stable negative header IDs, notification-time item
counts, command-driven changes, filtering, completion totals, removal,
same-section reordering, and cancellation of stale refreshes. Preference tests
cover persisted grouping and six-/seven-field saved-filter compatibility.
`ShowHabitStateTest` checks section-name resolution without changing detail UI.

Device coverage: `SectionHeaderViewTest`, `ListHabitsRootViewTest`,
`EditHabitSectionTest`, `HabitSectionDialogsTest`, and `SectionsDialogTest`.
These cover header accessibility/contrast, habit-only subtitle counts, inert
headers, assignment/validation, bulk moves, manager commands and recreation.
They are compiled locally, not device-verified. A later emulator review must
check grouped drag/drop, long names and large fonts, RTL, and Settings navigation.

Deferred new goldens under `views/habits/list/SectionHeaderView/`:
`render.png` and `render_other.png`. No existing HabitCardView golden changes
are introduced by grouping. Do not create replacement images without rendering
them on the configured emulator.

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
