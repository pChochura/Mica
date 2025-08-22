package com.pointlessapps.granite.mica.builtins

import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.runtime.model.Variable
import com.pointlessapps.granite.mica.runtime.model.Variable.Companion.toVariable

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
            val list = args[0].type.valueAsSupertype<ArrayType>(args[0].value) as List<*>
            IntType.toVariable(list.size.toLong())
        },
    ),
    BuiltinFunctionDeclarationBuilder.create(
        name = "removeAt",
        parameters = listOf(
            "list" to ArrayType(AnyType),
            "index" to IntType,
        ),
        getReturnType = { argTypes ->
            argTypes[0].superTypes.filterIsInstance<ArrayType>().first().elementType
        },
        execute = { args ->
            val list = args[0].type.valueAsSupertype<ArrayType>(args[0].value) as List<*>
            val index = args[1].type.valueAsSupertype<IntType>(args[1].value) as Long
            (list as MutableList<*>).removeAt(index.toInt()) as Variable<*>
        },
    ),
)

internal val builtinFunctionDeclarations = builtinFunctions.associate {
    val parameterTypes = it.parameters.map(Pair<String, Type>::second)
    (it.name to it.parameters.size) to mutableMapOf(
        parameterTypes to it,
    )
}
