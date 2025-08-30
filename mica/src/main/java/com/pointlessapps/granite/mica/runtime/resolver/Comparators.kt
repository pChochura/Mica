package com.pointlessapps.granite.mica.runtime.resolver

import com.pointlessapps.granite.mica.model.ClosedDoubleRange
import com.pointlessapps.granite.mica.model.Type

/**
 * Compares this [LongRange] with the [other] range for order.
 *
 * Ranges are ordered primarily by their [LongRange.start] values.
 * If the start values are equal, the ranges are then ordered by their
 * [LongRange.endInclusive] values.
 *
 * @return a negative integer if this range is less than the other,
 * zero if they are equal, or a positive integer if this range is greater than the other.
 */
internal fun LongRange.compareTo(other: LongRange): Int {
    val startComparison = this.start.compareTo(other.start)
    return if (startComparison != 0) {
        startComparison
    } else {
        this.endInclusive.compareTo(other.endInclusive)
    }
}

/**
 * Compares this [ClosedDoubleRange] with the [other] range for order.
 *
 * Ranges are ordered primarily by their [ClosedDoubleRange.start] values.
 * If the start values are equal, the ranges are then ordered by their
 * [ClosedDoubleRange.endInclusive] values.
 *
 * @return a negative integer if this range is less than the other,
 * zero if they are equal, or a positive integer if this range is greater than the other.
 */
internal fun ClosedDoubleRange.compareTo(other: ClosedDoubleRange): Int {
    val startComparison = this.start.compareTo(other.start)
    return if (startComparison != 0) {
        startComparison
    } else {
        this.endInclusive.compareTo(other.endInclusive)
    }
}

/**
 * Compares this [CharRange] with the [other] range for order.
 *
 * Ranges are ordered primarily by their [CharRange.first] characters.
 * If the first characters are equal, the ranges are then ordered by their
 * [CharRange.last] characters.
 *
 * @return a negative integer if this range is less than the other,
 * zero if they are equal, or a positive integer if this range is greater than the other.
 */
internal fun CharRange.compareTo(other: CharRange): Int {
    val firstComparison = this.first.compareTo(other.first)
    return if (firstComparison != 0) {
        firstComparison
    } else {
        this.last.compareTo(other.last)
    }
}

/**
 * Comparator for set elements.
 * Uses natural order if elements are [Comparable] and of the same type,
 * otherwise falls back to string comparison.
 */
private val elementComparator = Comparator<Any?> { a, b ->
    when {
        a == null && b == null -> 0
        a == null -> -1 // nulls first
        b == null -> 1
        a is Comparable<*> && b is Comparable<*> && a::class == b::class -> {
            // Unsafe cast, but types are checked
            @Suppress("UNCHECKED_CAST")
            (a as Comparable<Any?>).compareTo(b)
        }
        else -> a.toString().compareTo(b.toString())
    }
}

/**
 * Compares this list with the [other] list for order, assuming elements are of a specified [Type].
 *
 * Lists are compared element by element. If elements are [Comparable], their natural order is used.
 * Otherwise, they are converted to strings and compared lexicographically.
 * If one list is a prefix of another, the shorter list is considered smaller.
 *
 * @return A negative integer if this list is less than the other,
 * zero if they are equal, or a positive integer if this list is greater than the other.
 */
@Suppress("UNCHECKED_CAST")
internal fun List<*>.compareTo(other: List<*>): Int {
    val thisIterator = this.iterator()
    val otherIterator = other.iterator()

    while (thisIterator.hasNext() && otherIterator.hasNext()) {
        val thisElement = thisIterator.next()
        val otherElement = otherIterator.next()

        val comparison = elementComparator.compare(thisElement, otherElement)
        if (comparison != 0) {
            return comparison
        }
    }

    return when {
        thisIterator.hasNext() -> 1
        otherIterator.hasNext() -> -1
        else -> 0
    }
}

/**
 * Compares this set with the [other] set for order.
 *
 * Sets are compared by first comparing their sizes. If sizes are equal,
 * they are compared by converting their elements to sorted lists and then
 * comparing these lists element by element.
 *
 * Element comparison uses natural order if elements are [Comparable]. Otherwise,
 * they are converted to strings and compared lexicographically.
 *
 * @return A negative integer if this set is less than the other,
 * zero if they are equal, or a positive integer if this set is greater than the other.
 */
@Suppress("UNCHECKED_CAST")
internal fun Set<*>.compareTo(other: Set<*>): Int {
    // First, compare by size
    val sizeComparison = this.size.compareTo(other.size)
    if (sizeComparison != 0) {
        return sizeComparison
    }

    // If sizes are equal, convert to sorted lists and compare
    // Note: Element order in a set is not guaranteed, so we sort them
    // to ensure consistent comparison. We rely on the elements'
    // natural order or their string representation for sorting.

    val thisSortedList = this.toList().sortedWith(elementComparator)
    val otherSortedList = other.toList().sortedWith(elementComparator)

    return thisSortedList.compareTo(otherSortedList)
}

/**
 * Compares this map with the [other] map for order.
 *
 * Maps are compared by first comparing their sizes. If sizes are equal,
 * they are compared by their sorted key-value pairs. Keys are compared first,
 * and if keys are equal, their corresponding values are compared.
 *
 * Value comparison uses natural order if values are [Comparable]. Otherwise,
 * they are converted to strings and compared lexicographically.
 *
 * @return A negative integer if this map is less than the other,
 * zero if they are equal, or a positive integer if this map is greater than the other.
 */
@Suppress("UNCHECKED_CAST")
internal fun Map<String, *>.compareTo(other: Map<String, *>): Int {
    // First, compare by size
    val sizeComparison = this.size.compareTo(other.size)
    if (sizeComparison != 0) {
        return sizeComparison
    }

    // If sizes are equal, compare by sorted key-value pairs
    val thisSortedEntries = this.entries.sortedBy { it.key }
    val otherSortedEntries = other.entries.sortedBy { it.key }

    val thisIterator = thisSortedEntries.iterator()
    val otherIterator = otherSortedEntries.iterator()

    while (thisIterator.hasNext() && otherIterator.hasNext()) {
        val thisEntry = thisIterator.next()
        val otherEntry = otherIterator.next()

        // Compare keys
        val keyComparison = thisEntry.key.compareTo(otherEntry.key)
        if (keyComparison != 0) {
            return keyComparison
        }

        // If keys are equal, compare values
        val thisValue = thisEntry.value
        val otherValue = otherEntry.value

        val valueComparison = when {
            thisValue == null && otherValue == null -> 0
            thisValue == null -> -1 // nulls first
            otherValue == null -> 1
            thisValue is Comparable<*> && otherValue is Comparable<*> &&
                    thisValue::class == otherValue::class -> {
                // This is an unsafe cast, but we've checked that the types are the same
                // and that they are comparable.
                (thisValue as Comparable<Any?>).compareTo(otherValue as Any?)
            }
            // Fallback to string comparison if not directly comparable or types differ
            else -> thisValue.toString().compareTo(otherValue.toString())
        }
        if (valueComparison != 0) {
            return valueComparison
        }
    }

    // This part should ideally not be reached if sizes are equal and all entries match,
    // but it's a safeguard.
    return when {
        thisIterator.hasNext() -> 1
        otherIterator.hasNext() -> -1
        else -> 0
    }
}
