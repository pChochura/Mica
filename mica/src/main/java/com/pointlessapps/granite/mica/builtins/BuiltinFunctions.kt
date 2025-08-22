package com.pointlessapps.granite.mica.builtins

import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.model.Type

internal val builtinFunctions = listOf(
    toIntFunction,
    toRealFunction,
    toCharFunction,
    toBoolFunction,
    toStringFunction,
    toArrayFunction,
    toIntRangeFunction,
    toRealRangeFunction,
    toCharRangeFunction,
    BuiltinFunctionDeclarationBuilder.create(
        name = "length",
        parameters = listOf("list" to ArrayType(AnyType)),
        returnType = IntType,
        execute = { args ->
            val list = args[0].first.valueAsSupertype<ArrayType>(args[0].second) as List<*>
            IntType to list.size.toLong()
        },
    ),
    BuiltinFunctionDeclarationBuilder.create(
        name = "removeAt",
        parameters = listOf(
            "list" to ArrayType(AnyType),
            "index" to IntType,
        ),
        getReturnType = { argTypes ->
            argTypes[0].superTypes.filterIsInstance<ArrayType>().first()
        },
        execute = { args ->
            val list = args[0].first.valueAsSupertype<ArrayType>(args[0].second) as List<*>
            val index = args[1].first.valueAsSupertype<IntType>(args[1].second) as Long
            args[0].first.superTypes.filterIsInstance<ArrayType>().first() to
                    (list as MutableList<*>).apply { removeAt(index.toInt()) }
        },
    ),
)

internal val builtinFunctionDeclarations = builtinFunctions.associate {
    val parameterTypes = it.parameters.map(Pair<String, Type>::second)
    (it.name to it.parameters.size) to mutableMapOf(
        parameterTypes to it,
    )
}
