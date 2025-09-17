package com.pointlessapps.granite.mica.helper

import com.pointlessapps.granite.mica.model.Type

internal fun <T> Map<Type, Map<String, T>>.getMatchingTypeDeclaration(
    receiverType: Type,
    propertyName: String,
): T? {
    val candidates = mutableMapOf<Type, Map<String, T>>()
    this.forEach { (type, properties) ->
        if (receiverType.isSubtypeOf(type) && properties.containsKey(propertyName)) {
            candidates.put(type, properties)
        }
    }

    if (candidates.isEmpty()) return null
    if (candidates.size == 1) return candidates.entries.first().value[propertyName]

    return candidates.entries.minBy { (type, _) ->
        type.superTypes.indexOf(receiverType)
    }.value[propertyName]
}
