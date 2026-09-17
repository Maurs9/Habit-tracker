# Organizing habits

## Creating and editing

The add button opens the editor directly with **Yes or No** selected. Choose
**Measurable** to set a unit, target, target type, and frequency. Type can be changed
only before the habit is created. Switching types keeps the draft fields.
Both types support frequencies such as three times per week or every two days.
For measurable habits, meeting the full target on a recorded day qualifies that
day toward the selected frequency; eligible rest days receive automatic checkmarks.
Frequency intervals accept positive whole numbers through 2,147,483,647 days,
with at least one repetition and no more repetitions than days. Invalid or
overflowing input is rejected before saving. Very long existing schedules remain
compatible; derived rest days are generated only through the current logical day
plus 30, while recorded future entries are preserved.

**Section** and **Tags** stay visible. **More options** reveals Question and
Notes; **Fewer options** hides them without clearing their text. Editing a habit
with either field filled starts expanded. The fold, type, organization choices,
and draft fields survive rotation. Back or the toolbar arrow asks before
discarding changed fields: choose **Keep editing** to retain the draft or
**Discard changes** to leave. Unchanged drafts exit immediately; **Save** applies
the habit without a discard prompt.

## Today's list

Today's date and entries share a subtle highlighted column. It follows the
reversed date order and right-to-left layout, and disappears when scrolling to
older days. The tint uses 6% opacity in light mode, 16% in dark mode, and 20% in
pure-black mode so the column stays visible against darker backgrounds.
Foreground colors are adjusted only in highlighted cells to keep
text and inactive marks readable; the saved color and other cells are unchanged.

Dark and pure-black themes use thin separators in the existing gaps between
habit rows. Pure-black card backgrounds stay black, and light mode retains its
existing card spacing and shadows.
Changing pure black refreshes the settings screen and the main list, including
when dark mode follows the system. Habit-name text is adjusted locally for
readability on normal and selected rows; saved color indexes are unchanged.

The habit list uses the compact density layout, reducing the row gap to 1dp,
retaining standard 48dp entry buttons, and using streamlined section headers:
8dp above the first header, 20dp above later headers, and 4dp below each title.
Text size, horizontal date spacing, ordering, and habit history are unchanged;
text continues to respect the device's font settings.
The former density setting is no longer shown. Stored Standard, Spacious, or
Large values are ignored, so older preferences cannot prevent startup.

The toolbar shows **X of N done** for the current filtered list, or **N to go**
when completed/entered habits are hidden. Selected tags follow the count in
alphabetical order. The filter icon turns blue when tags are selected or
completed/entered habits are hidden; hiding archived habits alone does not tint
it. An empty database has no subtitle.

Yes/no checks and skips count as done. Recorded numerical "at least" values count
at or above their full target, not a per-day fraction. Recorded numerical
"at most" values do not count as done; qualifying automatic rest days do.
A numerical skip is not completion of a target. Hiding entered habits still
uses the existing entered-day filter, not this done count.

Numerical "at most" entries use **Within limit** rather than implying the day is
complete. Empty views explain whether habits are archived or hidden by the
current filters and provide the relevant recovery action. The sort menu marks
the active sort and includes its direction.

Opening an entry shows its habit and date; measurements also show their unit
and target. Unsubmitted values and notes survive app interruptions and rotation,
and remain attached to the original habit and day. A missing habit is reported
instead of saving the draft to a different list row.

Automatic numerical checkmarks are derived from the schedule, not recorded
quantities. A value such as `0.001` stays a measurement and is compared with the
target normally. CSV checkmark exports keep raw thousandths for numerical
measurements (`0.001` exports as `1`) and label automatic rest days `YES_AUTO`.
Database version 29 separates skip markers from nonnegative measurements.
The automatic upgrade and import of older backups preserve records previously
interpreted as skips, including their dates and notes. An old `0.003` entry that
was already interpreted as a skip cannot be distinguished from an intentional
skip; re-enter it if it was meant to be a measurement. New `0.003` entries remain
measurements, even when skipping is disabled.

Numerical skips preserve streak continuity without creating a successful
skip-only streak. Missing at-most measurements retain the existing zero-amount
scoring behavior. Target progress includes all repetitions in the selected
frequency and uses the actual quarter and year lengths. Weekly/custom-frequency
targets are prorated by period length; monthly frequencies retain calendar-month
quotas. Skipped days reduce the corresponding daily share of the target.

## Colors

Choose a habit color using the wheel: its four rings, from outside to inside,
are Deep, Vibrant, Soft, and Light. Each ring has 12 hues. The four center colors
are Light Gray, Medium Gray, Slate, and Charcoal. Select a color, then press
**OK**; **Cancel** leaves the habit unchanged. All 52 choices have accessibility
labels and support keyboard selection.

The picker also offers a named color list with larger touch targets. It contains
the same 52 colors as the wheel, and preserves the selected view, color and list
position across rotation. Switching views does not save a color; **OK** confirms
the selection and **Cancel** leaves the habit unchanged. Preview labels use a
contrasting foreground without changing saved palette colors.

New habits start with Vibrant Blue, or the last color saved when creating a
habit. Editing an existing habit or canceling creation does not change that
default. Light and dark themes use different values for the same saved slot;
widgets use the dark palette. Palette updates preserve stored indexes and
habit history, but can change their appearance and the light-theme hex values
in CSV exports.

## Tags

Open **Tags** when creating or editing a habit. Check existing tags, or enter a
tag name and choose **Add**. Spaces within a tag are allowed; line breaks and
blank names are not. Leading and trailing spaces are removed. Typing an existing
name with different capitalization selects its existing spelling.

Choose **Save** to apply the selection; any non-blank name still in the field is
added too. **Cancel** discards changes. Selections, newly added tags, and an
unfinished name survive rotation. In the habit editor, tags are a draft until
you save the habit. Uncheck every tag and save to remove all assignments.

You can also select one habit in the list or open its detail-screen menu and
choose **Tags**. The detail subtitle shows its section and alphabetically sorted
tags; it hides that row when neither is set. Saving an empty
tag list removes its tags without changing its history.

Open the filter menu and choose **Filter by tags**. A habit must have all selected
tags to appear. Existing archived/completed filters still apply. **Clear tags**
removes the tag filter without changing those other settings.

Tags are included in database backups and in the `Tags` column of `Habits.csv`.
Multiple tags occupy separate lines inside a quoted CSV field. Database upgrades
leave existing habits untagged and preserve their original data.

## Sections

Each habit can have one optional section, separate from its tags. Choose
**Section** in the habit editor, or select one or more habits in the list and
choose **Section**. **None** removes the assignment. **New section…** creates
and selects a section; names are trimmed, non-blank, and unique ignoring
capitalization. Canceling the habit editor after creating a section leaves
that empty section in the manager.

In the filter menu, open **Sort → Group by section** to group the list; it is
off by default. Sections follow their saved order, while the selected primary
and secondary sorts apply within each group. Headers show today's done/total
count for visible habits only. Empty sections are hidden. Unsectioned habits,
including missing section references, appear last under **Other**. If all visible
habits are unsectioned, the list stays flat without an Other header. Section
headers are informational; sections cannot be collapsed.

Section names are bold and higher-contrast than their progress counts.
Extra space and a thin line above each subsequent section separate the groups;
the first section has less top spacing and no line. Headers remain close to
their own habits, without colored banners or sticky behavior.

With manual sorting and grouping enabled, drag habits **within their section
only**. Headers cannot be selected or dragged. Use the selection menu's
**Section** action to move habits between sections. Turning grouping off restores
the flat list and its existing manual reorder behavior.

Long-press a habit and move it to reorder. Releasing without moving selects the
habit instead. Reversing direction while dragging preserves the final drop
position when the list is reopened.

Keyboard and accessibility users can also move a habit up or down, including
through the selection menu or Ctrl+Up/Down. The same manual-order and section
restrictions apply. The date header supports keyboard and accessibility
navigation to older or newer dates.

Open **Settings → Sections** to add sections, rename them, move them up or down,
or delete them. Deletion asks for confirmation; habits and history are kept and
become unsectioned. Empty sections remain available in this manager.

Version 29 database backups retain sections, habit assignments, and unambiguous
skip markers. Older
backups remain importable. Import matches section names ignoring surrounding
whitespace and capitalization, keeps local section names and order, and appends
unknown sections in backup order. Re-importing does not create duplicate
sections. Missing section references become unsectioned.

`Habits.csv` includes a `Section` column immediately after `Tags`; unsectioned
habits have an empty value. CSV is for inspection, not database restoration.

## Widget selection

When adding a widget, check one or more habits and choose **Save**. Multiple
habits use the existing swipeable stack, in picker list order rather than tap
order. A section shortcut checks that section's listed habits without clearing
other checks; it does not link the widget to future section membership changes.
Uncheck individual habits to refine the selection.

Archived habits are excluded. Streak widgets offer only Yes or No habits;
Target widgets offer only Measurable habits. Section shortcuts follow those
same restrictions and disappear when no sections exist.

Checks survive rotation. Save is disabled with no selection; **Cancel** or
Back discards the draft. If selected habits are deleted or become ineligible,
the picker asks you to review the remaining checks and save again instead of
silently saving a partial or empty selection.

## Saved filters

Choose tags, visibility options, sorting, and **Group by section**, then select **Save current filter**.
Give the filter a unique name. **Saved filters** restores that combination.
Older saved filters restore with grouping off.

The saved-filter dialog also lets you delete a saved filter. This removes only
the saved view, not any habits or entries. Saved filters are application
preferences; a habit database export does not include them.

## Skipping a date range

Select one or more habits and choose **Skip a date range**. Choose the first and
last dates, review the number of eligible entries, then choose **Apply skips**.
Both dates are included. The same action is available for a single habit in
its detail-screen menu.

Bulk Skip changes only dates with no recorded value, no note, and no automatic
checkmark. It preserves completed entries, recorded lapses, numerical values
(including zero), existing skips, automatic checkmarks, and notes.

Entries are checked again when the operation runs. An entry recorded after the
preview is not overwritten. Skips use the app's existing scoring and streak
rules; this is not a separate vacation/freeze scoring mode.

To change a skip later, edit that date using the normal entry controls.
To unset a numerical entry, clear its value and save. Its notes are preserved.

## Reminder times

Select one habit in the list, or open its detail-screen menu, and choose
**Reminder times**. Add a time, or tap an
existing time to edit or remove it. Save the list to apply your changes.
Duplicate times are not allowed. All times share the weekdays selected in the
habit's edit form; a newly enabled reminder defaults to every day.

The habit editor still lets you change its primary reminder time. Other times
are preserved. Turning reminders off in that editor clears all of the habit's
reminder times. Removing every time in the reminder-times dialog also turns them
off. Snooze pauses that habit's regular reminders until the selected snooze time,
then normal scheduling resumes.
A pending snooze survives app restarts while its deadline is still in the
future. An expired snooze no longer blocks regular scheduling or repeatedly
registers an alarm in the past. A snoozed broadcast already waiting for app
startup can still be accepted once.

Database backups retain all reminder times. `Habits.csv` includes
`ReminderTimes` and `ReminderDays` columns for inspection in a spreadsheet.
As with other CSV exports, these are not a restorable database backup.

## Automation and data safety

Numerical increments and decrements treat missing, skipped, and automatic entries
as zero, then apply the adjustment in the serialized command queue. Consecutive
actions accumulate rather than overwriting each other, and retain the entry's
current notes. Quantities stay between zero and 2,147,483.647.

Entry replacement is atomic: a failed replacement does not delete the previously
saved value or note. CSV exports quote habit names containing commas, quotes,
or line breaks. HabitBull imports infer a habit's type before encoding its
measurements and use strict date parsing. Rewire and Tickmate imports rebuild
computed history before reporting success.

Database backups do not include application preferences. Android's settings
backup uses the default preferences file, including saved filters and interface
choices. Keep a backup from the previous app version before upgrading; version
29 databases cannot be opened by older releases that only support version 28.
