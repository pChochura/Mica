package com.pointlessapps.granite.mica.compiler.helper

/**
 * Calculate the power of an integer.
 *
 * Accepts [exponent] as a not negative integer.
 */
internal fun Long.pow(exponent: Long): Long {
    var result = 1L
    var currentExponent = exponent
    while (currentExponent-- > 0) {
        result *= this
    }

    return result
}
