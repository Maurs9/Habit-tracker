package org.isoron.uhabits.core.models

/** A computed total in thousandths; individual persisted measurements remain Ints. */
data class EntryAggregate(val timestamp: Timestamp, val value: Long)
