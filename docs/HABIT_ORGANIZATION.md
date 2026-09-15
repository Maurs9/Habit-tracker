# Organizing habits

## Tags

Add tags when creating or editing a habit. Enter one tag per line; spaces within
a tag are allowed. Leading and trailing spaces are removed, and repeated names
that differ only in capitalization are treated as the same tag.

You can also select one habit in the list and choose **Tags**. Saving an empty
tag list removes its tags without changing its history.

Open the filter menu and choose **Filter by tags**. A habit must have all selected
tags to appear. Existing archived/completed filters still apply. **Clear tags**
removes the tag filter without changing those other settings.

Tags are included in database backups and in the `Tags` column of `Habits.csv`.
Multiple tags occupy separate lines inside a quoted CSV field. Database upgrades
leave existing habits untagged and preserve their original data.

## Saved filters

Choose tags, visibility options, and sorting, then select **Save current filter**.
Give the filter a unique name. **Saved filters** restores that combination.

The saved-filter dialog also lets you delete a saved filter. This removes only
the saved view, not any habits or entries. Saved filters are application
preferences; a habit database export does not include them.

## Skipping a date range

Select one or more habits and choose **Skip a date range**. Choose the first and
last dates, review the number of eligible entries, then choose **Apply skips**.
Both dates are included.

Bulk Skip changes only dates with no recorded value, no note, and no automatic
checkmark. It preserves completed entries, recorded lapses, numerical values
(including zero), existing skips, automatic checkmarks, and notes.

Entries are checked again when the operation runs. An entry recorded after the
preview is not overwritten. Skips use the app's existing scoring and streak
rules; this is not a separate vacation/freeze scoring mode.

To change a skip later, edit that date using the normal entry controls.
To unset a numerical entry, clear its value and save. Its notes are preserved.

## Reminder times

Select one habit in the list and choose **Reminder times**. Add a time, or tap an
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
