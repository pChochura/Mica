package com.pointlessapps.granite.mica.runtime.resolver

import com.pointlessapps.granite.mica.model.ClosedDoubleRange

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

        val comparison = when {
            thisElement == null && otherElement == null -> 0
            thisElement == null -> -1 // nulls first
            otherElement == null -> 1
            thisElement is Comparable<*> && otherElement is Comparable<*> &&
                    thisElement::class == otherElement::class -> {
                // This is an unsafe cast, but we've checked that the types are the same
                // and that they are comparable.
                (thisElement as Comparable<Any?>).compareTo(otherElement as Any?)
            }
            // Fallback to string comparison if not directly comparable or types differ
            else -> thisElement.toString().compareTo(otherElement.toString())
        }
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
