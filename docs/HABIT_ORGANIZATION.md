# Organizing habits

## Creating and editing

The add button opens the editor directly with **Yes or No** selected. Choose
**Measurable** to set a unit, target, target type, and period. Type can be changed
only before the habit is created. Switching types keeps the draft fields and
each type's frequency; a measurable period always has a numerator of one.

**Section** and **Tags** stay visible. **More options** reveals Question and
Notes; **Fewer options** hides them without clearing their text. Editing a habit
with either field filled starts expanded. The fold, type, organization choices,
and draft fields survive rotation. Back or the toolbar arrow cancels the editor
and returns to the previous screen; **Save** applies the habit.

## Today's list

Today's date and entries share a subtle highlighted column. It follows the
reversed date order and right-to-left layout, and disappears when scrolling to
older days. Foreground colors are adjusted only in highlighted cells to keep
text and inactive marks readable; the saved color and other cells are unchanged.

The toolbar shows **X of N done** for the current filtered list, or **N to go**
when completed/entered habits are hidden. Selected tags follow the count in
alphabetical order. The filter icon turns blue when tags are selected or
completed/entered habits are hidden; hiding archived habits alone does not tint
it. An empty database has no subtitle.

The done count keeps the existing completion rule: yes/no checks and skips
count; numerical "at least" habits count at or above their full target, not a
per-day fraction. Numerical "at most" habits never count as done. A numerical
skip is not completion of a positive target. Hiding entered habits still uses
the existing entered-day filter, not this done count.

## Colors

Choose a habit color using the wheel: its outer, middle, and inner rings are
Vibrant, Muted, and Deep. The four center colors are Gray, Dark Gray, Slate, and
Charcoal. Select a color, then press **OK**; **Cancel** leaves the habit unchanged.
All 40 choices have accessibility labels and support keyboard selection.

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
habits are unsectioned, the list stays flat without an Other header.

With manual sorting and grouping enabled, drag habits **within their section
only**. Headers cannot be selected or dragged. Use the selection menu's
**Section** action to move habits between sections. Turning grouping off restores
the flat list and its existing manual reorder behavior.

Open **Settings → Sections** to add sections, rename them, move them up or down,
or delete them. Deletion asks for confirmation; habits and history are kept and
become unsectioned. Empty sections remain available in this manager.

Version 28 database backups retain sections and habit assignments. Older
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
A pending snooze survives app restarts until it is delivered, including when its
deadline passed while the app was not running.

Database backups retain all reminder times. `Habits.csv` includes
`ReminderTimes` and `ReminderDays` columns for inspection in a spreadsheet.
As with other CSV exports, these are not a restorable database backup.
