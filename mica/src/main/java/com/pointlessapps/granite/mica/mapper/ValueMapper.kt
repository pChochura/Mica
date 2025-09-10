package com.pointlessapps.granite.mica.mapper

import com.pointlessapps.granite.mica.model.ClosedDoubleRange
import com.pointlessapps.granite.mica.model.CustomType

internal fun Any?.asString(): String = when (this) {
    is Boolean, is Char, is Long, is Double, is String,
    is CharRange, is LongRange, is ClosedDoubleRange,
        -> toString()

    is Set<*> -> joinToString(prefix = "{", postfix = "}", transform = Any?::asString)
    is Map<*, *> -> if (containsKey(CustomType.NAME_PROPERTY)) {
        filterKeys { it != CustomType.NAME_PROPERTY }.entries.joinToString(
            prefix = "${get(CustomType.NAME_PROPERTY)}{",
            postfix = "}",
        ) { (key, value) -> "${key.asString()} = ${value.asString()}" }
    } else {
        entries.joinToString(
            prefix = "{",
            postfix = "}",
        ) { (key, value) -> "${key.asString()}: ${value.asString()}" }
    }

    is List<*> -> joinToString(prefix = "[", postfix = "]", transform = Any?::asString)

    else -> throw IllegalArgumentException(
        "toString function cannot be applied to ${toType().name}",
    )
}
