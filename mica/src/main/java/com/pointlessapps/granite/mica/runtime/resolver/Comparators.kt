package com.pointlessapps.granite.mica.runtime.resolver

import com.pointlessapps.granite.mica.model.ClosedDoubleRange
import com.pointlessapps.granite.mica.model.OpenEndDoubleRange

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
 * Compares this [OpenEndDoubleRange] with the [other] range for order.
 *
 * Ranges are ordered by their [OpenEndDoubleRange.start] values.
 * If the start values are equal, the ranges are considered equal for ordering purposes,
 * as there is no defined upper bound to differentiate them further.
 *
 * @return a negative integer if this range's start is less than the other's,
 * zero if their starts are equal, or a positive integer if this range's start is greater than the other's.
 */
internal fun OpenEndDoubleRange.compareTo(other: OpenEndDoubleRange): Int {
    val startComparison = this.start.compareTo(other.start)
    return if (startComparison != 0) {
        startComparison
    } else 0
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
