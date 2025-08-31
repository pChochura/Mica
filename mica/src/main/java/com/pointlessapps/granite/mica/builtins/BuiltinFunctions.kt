package com.pointlessapps.granite.mica.builtins

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
    (it.name to it.parameters.size) to mutableMapOf(it.parameters to it)
}
