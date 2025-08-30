package com.pointlessapps.granite.mica.builtins

import com.pointlessapps.granite.mica.model.Type

internal val builtinFunctions = listOf(
    toIntFunction,
    toRealFunction,
    toCharFunction,
    toBoolFunction,
    toStringFunction,
    toArrayFunction,
    toSetFunction,
    toIntRangeFunction,
    toRealRangeFunction,
    toCharRangeFunction,
    lengthFunction,
    removeAtFunction,
    insertAtFunction,
    insertFunction,
    containsFunction,
    setPropertyFunction,
    typeOfFunction,
    isSubtypeOfFunction,
)

internal val builtinFunctionDeclarations = builtinFunctions.associate {
    val parameterTypes = it.parameters.map(Pair<String, Type>::second)
    (it.name to it.parameters.size) to mutableMapOf(
        parameterTypes to it,
    )
}
