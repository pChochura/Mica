package com.pointlessapps.granite.mica.builtins

import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.model.UndefinedType
import com.pointlessapps.granite.mica.runtime.model.BoolVariable
import com.pointlessapps.granite.mica.runtime.model.IntVariable
import com.pointlessapps.granite.mica.runtime.model.UndefinedVariable
import com.pointlessapps.granite.mica.runtime.model.Variable
import com.pointlessapps.granite.mica.runtime.model.Variable.Companion.toVariable

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

internal val insertAtFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "insertAt",
    parameters = listOf(
        "list" to ArrayType(AnyType),
        "index" to IntType,
        "value" to AnyType,
    ),
    returnType = UndefinedType,
    execute = { args ->
        val elementType = args[0].type.superTypes.filterIsInstance<ArrayType>().first().elementType
        if (!args[2].type.isSubtypeOf(elementType)) {
            throw IllegalStateException(
                "Function insertAt expects ${elementType.name} as a third argument",
            )
        }

        val list = args[0].type.valueAsSupertype<ArrayType>(
            args[0].value,
        ) as MutableList<Variable<*>>
        val index = args[1].type.valueAsSupertype<IntType>(args[1].value) as Long
        val value = elementType.toVariable(
            args[2].type.valueAsSupertype(args[2].value, elementType),
        )
        list.add(index.toInt(), value)
        return@create UndefinedVariable
    },
)

internal val insertFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "insert",
    parameters = listOf(
        "list" to ArrayType(AnyType),
        "value" to AnyType,
    ),
    returnType = UndefinedType,
    execute = { args ->
        val elementType = args[0].type.superTypes.filterIsInstance<ArrayType>().first().elementType
        if (!args[1].type.isSubtypeOf(elementType)) {
            throw IllegalStateException(
                "Function insert expects ${elementType.name} as a second argument",
            )
        }

        val list = args[0].type.valueAsSupertype<ArrayType>(
            args[0].value,
        ) as MutableList<Variable<*>>
        val value = elementType.toVariable(
            args[1].type.valueAsSupertype(args[1].value, elementType),
        )
        list.add(value)
        return@create UndefinedVariable
    },
)

internal val containsFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "contains",
    parameters = listOf(
        "list" to ArrayType(AnyType),
        "value" to AnyType,
    ),
    returnType = BoolType,
    execute = { args ->
        val elementType = args[0].type.superTypes.filterIsInstance<ArrayType>().first().elementType
        if (!args[1].type.isSubtypeOf(elementType)) {
            throw IllegalStateException(
                "Function contains expects ${elementType.name} as a second argument",
            )
        }

        val list = args[0].type.valueAsSupertype<ArrayType>(args[0].value) as MutableList<*>
        BoolVariable(
            list.firstOrNull {
                val element = it as Variable<*>
                val value = element.type.toVariable(
                    args[1].type.valueAsSupertype(args[1].value, element.type),
                )
                return@firstOrNull element.compareTo(value) == 0
            } != null,
        )
    },
)
