package org.isoron.uhabits.activities.common.dialogs

internal fun parsePositiveFrequencyInteger(text: String): Int? =
    text.trim().toIntOrNull()?.takeIf { it > 0 }
