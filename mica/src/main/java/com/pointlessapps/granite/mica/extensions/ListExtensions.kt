package com.pointlessapps.granite.mica.extensions

internal fun <T> List<T>.plusIf(condition: Boolean, element: T): List<T> = if (condition) {
    this.plus(element)
} else {
    this
}
