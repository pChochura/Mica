package com.pointlessapps.granite.mica.builtins

import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.runtime.model.IntVariable
import com.pointlessapps.granite.mica.runtime.model.Variable

internal val lengthFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "length",
    parameters = listOf("list" to ArrayType(AnyType)),
    returnType = IntType,
    execute = { args ->
        val list = args[0].type.valueAsSupertype<ArrayType>(args[0].value) as List<*>
        return@create IntVariable(list.size.toLong())
    },
)

internal val removeAtFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "removeAt",
    parameters = listOf(
        "list" to ArrayType(AnyType),
        "index" to IntType,
    ),
    getReturnType = { argTypes ->
        argTypes[0].superTypes.filterIsInstance<ArrayType>().first().elementType
    },
    execute = { args ->
        val list = args[0].type.valueAsSupertype<ArrayType>(args[0].value) as MutableList<*>
        val index = args[1].type.valueAsSupertype<IntType>(args[1].value) as Long
        return@create list.removeAt(index.toInt()) as Variable<*>
    },
)
