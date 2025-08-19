package com.pointlessapps.granite.mica.runtime.helper

import com.pointlessapps.granite.mica.model.ClosedDoubleRange
import com.pointlessapps.granite.mica.runtime.errors.RuntimeTypeException

/**
 * Convert the string to a number.
 * It can be in a format of:
 *  - 12312
 *  - 100_200
 *  - 0x312ab
 *  - 0b101
 */
internal fun String.toIntNumber(): Long {
    val cleanedInput = this.replace("_", "")

    if (cleanedInput.startsWith("0x", ignoreCase = true)) {
        return cleanedInput.substring(2).toLong(16)
    }

    if (cleanedInput.startsWith("0b", ignoreCase = true)) {
        return cleanedInput.substring(2).toLong(2)
    }

    return cleanedInput.toLongOrNull()
        ?: throw RuntimeTypeException("Invalid number format: $this")
}

/**
 * Convert the string to a number.
 * It can be in a format of:
 *  - 123.3121
 *  - 123e4
 *  - 123e-7
 *  - 1.23e-7
 */
internal fun String.toRealNumber(): Double {
    val cleanedInput = this.replace("_", "")

    return cleanedInput.toDoubleOrNull()
        ?: throw RuntimeTypeException("Invalid number format: $this")
}

/**
 * Treats the range as a list of integers and maps it as a list of doubles.
 */
internal fun ClosedDoubleRange.toIntList(): List<Double> {
    return (start.toInt()..endInclusive.toInt()).map { it.toDouble() }
}
