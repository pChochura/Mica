package com.pointlessapps.granite.mica.runtime.helper

import com.pointlessapps.granite.mica.runtime.errors.RuntimeTypeException

/**
 * Convert the string to a number.
 * It can be in a format of:
 *  - 12312
 *  - 100_200
 *  - 123.3121
 *  - 123e4
 *  - 123e-7
 *  - 0x312ab
 *  - 0b101
 */
internal fun String.toNumber(): Double {
    val cleanedInput = this.replace("_", "")

    if (cleanedInput.startsWith("0x", ignoreCase = true)) {
        return cleanedInput.substring(2).toLong(16).toDouble()
    }

    if (cleanedInput.startsWith("0b", ignoreCase = true)) {
        return cleanedInput.substring(2).toLong(2).toDouble()
    }

    return cleanedInput.toDoubleOrNull()
        ?: throw RuntimeTypeException("Invalid number format: $this")
}
