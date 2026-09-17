---
target: Android daily habit list and check-in flows
total_score: 23
max_score: 40
na_heuristics: 
p0_count: 0
p1_count: 3
timestamp: 2026-09-17T03-52-59Z
slug: abits-activities-habits-list-listhabitsrootview-kt
---
Method: dual-agent (A: 731d0b55-53b6-41ff-8d67-651e3d1f123a · B: b8d243a0-a2a6-4ffc-b7ac-e992313af33d)

## Verdict

**Keep the visual identity; improve check-in clarity and recovery.** The habit-by-day matrix is genuinely suited to this product. The biggest weaknesses appear when opening an entry, interpreting progress, and returning after an interruption - not in the palette or overall composition.

**Scope:** Android daily habit list and its directly connected entry/edit flows, anchored to `uhabits-android/src/main/java/org/isoron/uhabits/activities/habits/list/ListHabitsRootView.kt`. Current source plus limited golden/historical imagery were reviewed. No live Android session was available; scores are provisional design judgments, not runtime certification.

## Design health: 23/40 - Acceptable

| # | Heuristic | Score | Key issue |
|---|-----------|-------|-----------|
| 1 | Visibility of system status | 2/4 | Done, entered, and filtered emptiness can disagree. |
| 2 | Match with the real world | 3/4 | Familiar language; limit-based progress needs explanation. |
| 3 | User control and freedom | 2/4 | Unfinished check-ins and deliberately exited editor drafts are not recovered. |
| 4 | Consistency and standards | 2/4 | Strong list accessibility does not carry through to popup controls. |
| 5 | Error prevention | 2/4 | Entry dialogs omit habit/date/unit context. |
| 6 | Recognition rather than recall | 2/4 | Users must remember which cell they opened. |
| 7 | Flexibility and efficiency | 3/4 | Densities, bulk actions, filters, and quick-toggle options help. |
| 8 | Aesthetic and minimalist design | 3/4 | Purposeful density and quiet grouping. |
| 9 | Error recovery | 2/4 | Useful numerical validation, but weak interruption recovery. |
| 10 | Help and documentation | 2/4 | Help and hints exist; local explanations remain thin. |
| **Total** | | **23/40** | **Interaction improvements matter more than restyling.** |

## Design specificity and strengths

This feels authored for habit tracking, not like a generic dashboard. Preserve:

- **The date matrix and today column:** fast vertical scanning without hiding history.
- **Restrained sections and density choices:** the compact-light golden retains clear section hierarchy and readable value/unit separation. Compact keeps 48dp entry targets; it is not a tiny-target mode.
- **Existing native accessibility:** daily cells describe habit, date, state, and values; section headers expose heading semantics. Extend that foundation rather than replacing it.

**Deterministic scan:** the detector returned zero findings because it scanned **zero files**: all 45 XML layouts, as well as Kotlin sources, are unsupported. This is a coverage gap, not a clean bill of health. Native source review supplied the useful technical evidence. Browser overlays are inapplicable.

## Priority issues

### 1. [P1] Check-in dialogs lose context and meaningful control names

The list passes value/notes into a popup without the habit name or entry date. Numerical editing lacks unit/target context, while yes/no choices use private-use icon glyphs without equivalent meaningful action names or selected-state semantics.

**Impact:** users must remember which habit/date they opened; assistive-technology users encounter weaker semantics precisely when taking action. Exact TalkBack behavior remains unverified.

**Fix:** show a compact habit/date header, a unit and target beside numerical input, and localized native button labels and state. Preserve quick-toggle behavior.

**Evidence:** [ListHabitsScreen.kt:274-305](uhabits-android/src/main/java/org/isoron/uhabits/activities/habits/list/ListHabitsScreen.kt#L274-L305), [checkmark_popup.xml:22-110](uhabits-android/src/main/res/layout/checkmark_popup.xml#L22-L110), [CheckmarkDialog.kt:44-75](uhabits-android/src/main/java/org/isoron/uhabits/activities/common/dialogs/CheckmarkDialog.kt#L44-L75).

**Commands:** `/impeccable clarify`, `/impeccable harden`.

### 2. [P1] App interruption dismisses unfinished check-ins

The activity dismisses the active dialog on pause; unfinished values and notes have no recovery path.

**Impact:** switching apps mid-entry can discard work. This follows the source lifecycle path; it was not reproduced on a device.

**Fix:** restore the draft together with its habit/date identity and a restoration-safe submission callback. Simply removing dismissal is insufficient.

**Evidence:** [ListHabitsActivity.kt:105-110](uhabits-android/src/main/java/org/isoron/uhabits/activities/habits/list/ListHabitsActivity.kt#L105-L110), [DialogUtils.kt:8-28](uhabits-android/src/main/java/org/isoron/uhabits/utils/DialogUtils.kt#L8-L28), [NumberDialog.kt:49-57](uhabits-android/src/main/java/org/isoron/uhabits/activities/common/dialogs/NumberDialog.kt#L49-L57).

**Command:** `/impeccable harden`.

### 3. [P1] Progress and empty states can tell conflicting stories

A recorded "at most" value can be described as "Target met" without counting as done. Separately, an archived-only database can reach "You're all done for today!" when no habits are visible.

**Impact:** the answer to "what remains?" becomes unreliable.

**Fix:** distinguish checked in, within limit so far, and hidden by filters. Derive empty-state explanations from the actual reason and offer the relevant recovery action. Preserve documented limit-based scoring rather than treating an unfinished day as complete.

**Evidence:** [Habit.kt:82-98](uhabits-core/src/jvmMain/java/org/isoron/uhabits/core/models/Habit.kt#L82-L98), [NumberButtonView.kt:141-154](uhabits-android/src/main/java/org/isoron/uhabits/activities/habits/list/views/NumberButtonView.kt#L141-L154), [ListHabitsRootView.kt:159-183](uhabits-android/src/main/java/org/isoron/uhabits/activities/habits/list/ListHabitsRootView.kt#L159-L183).

**Command:** `/impeccable clarify`.

### 4. [P2] Leaving a changed habit editor silently discards the draft

Toolbar Up finishes the editor without a dirty-state decision. This matches the documented cancellation behavior, but an accidental exit still loses edits.

**Fix:** offer dirty-only "Discard changes / Keep editing" protection, consistently for toolbar Up and system Back, or provide recoverable drafts. Leave unchanged exits immediate.

**Evidence:** [EditHabitActivity.kt:416-419](uhabits-android/src/main/java/org/isoron/uhabits/activities/habits/edit/EditHabitActivity.kt#L416-L419).

**Command:** `/impeccable harden`.

## Cognitive load and emotional journey

**Moderate: 3/8 checklist failures** - mixed menu categories, excessive menu choices, and working-memory demands in entry dialogs. Single focus, grouping, hierarchy, sequential decisions, and progressive disclosure are strengths. Filter and Sort each define six choices; single-habit selection can expose eight actions, subject to platform menu layout. The habit rows themselves are not an equivalent menu-choice problem.

The likely journey is calm orientation, uncertainty inside the anonymous popup, then satisfaction from immediate feedback. Lost drafts and contradictory progress undermine that ending. These are predicted experiences, not observed user-study results.

## Persona red flags

- **Jordan, first-timer:** must interpret popup symbols and find Group by section under Filter > Sort.
- **Sam, accessibility-dependent:** well-described list cells lead into less meaningfully named popup controls.
- **Casey, interrupted mobile user:** switching apps can lose an unfinished measurement or note.

## Minor observations and open design decisions

Historical-date scrolling needs a verified accessible alternative; large-font behavior and tightly adjacent targets need on-device evaluation. No clipping, contrast failure, or live TalkBack failure was established.

The key product decision is whether the daily summary emphasizes check-ins, achieved targets, or distinguishes both. Editor protection also requires choosing between dirty-only confirmation and recoverable drafts. Neither requires a visual redesign.
