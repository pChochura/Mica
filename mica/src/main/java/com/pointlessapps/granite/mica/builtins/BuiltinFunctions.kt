package com.pointlessapps.granite.mica.builtins

internal val builtinFunctions = typeConversionBuiltinFunctions +
        arrayBuiltinFunctions +
        customObjectBuiltinFunctions +
        setBuiltinFunctions +
        typeBuiltinFunctions +
        stringBuiltinFunctions +
        rangeBuiltinFunctions

internal val builtinFunctionDeclarations = builtinFunctions.associate {
    (it.name to it.parameters.size) to mutableMapOf(it.parameters to it)
}
