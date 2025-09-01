package com.pointlessapps.granite.mica.builtins

import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Companion.of
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Resolver
import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.EmptyArrayType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.model.UndefinedType
import com.pointlessapps.granite.mica.runtime.model.BoolVariable
import com.pointlessapps.granite.mica.runtime.model.IntVariable
import com.pointlessapps.granite.mica.runtime.model.UndefinedVariable
import com.pointlessapps.granite.mica.runtime.model.Variable
import com.pointlessapps.granite.mica.runtime.model.Variable.Companion.toVariable

private val lengthFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "length",
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(EmptyArrayType)),
    returnType = IntType,
    execute = { args ->
        val list = args[0].type.valueAsSupertype<ArrayType>(args[0].value) as List<*>
        return@create IntVariable(list.size.toLong())
    },
)

private val removeAtFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "removeAt",
    parameters = listOf(
        Resolver.SHALLOW_MATCH.of(EmptyArrayType),
        Resolver.SUBTYPE_MATCH.of(IntType),
    ),
    getReturnType = { (it[0] as ArrayType).elementType },
    execute = { args ->
        val list = args[0].value as MutableList<*>
        val index = args[1].type.valueAsSupertype<IntType>(args[1].value) as Long
        return@create list.removeAt(index.toInt()) as Variable<*>
    },
)

@Suppress("UNCHECKED_CAST")
private val insertAtFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "insertAt",
    parameters = listOf(
        Resolver.SHALLOW_MATCH.of(EmptyArrayType),
        Resolver.SUBTYPE_MATCH.of(IntType),
        Resolver.SUBTYPE_MATCH.of(AnyType),
    ),
    returnType = UndefinedType,
    execute = { args ->
        val elementType = (args[0].type as ArrayType).elementType
        if (!args[2].type.isSubtypeOf(elementType)) {
            throw IllegalStateException(
                "Function insertAt expects ${elementType.name} as a third argument",
            )
        }

        val list = args[0].value as MutableList<Variable<*>>
        val index = args[1].type.valueAsSupertype<IntType>(args[1].value) as Long
        list.add(index.toInt(), args[2])
        return@create UndefinedVariable
    },
)

@Suppress("UNCHECKED_CAST")
private val insertFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "insert",
    parameters = listOf(
        Resolver.SHALLOW_MATCH.of(EmptyArrayType),
        Resolver.SUBTYPE_MATCH.of(AnyType),
    ),
    returnType = UndefinedType,
    execute = { args ->
        val elementType = (args[0].type as ArrayType).elementType
        if (!args[1].type.isSubtypeOf(elementType)) {
            throw IllegalStateException(
                "Function insert expects ${elementType.name} as a second argument",
            )
        }

        val list = args[0].value as MutableList<Variable<*>>
        list.add(args[1])
        return@create UndefinedVariable
    },
)

private val containsFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "contains",
    parameters = listOf(
        Resolver.SUBTYPE_MATCH.of(EmptyArrayType),
        Resolver.SUBTYPE_MATCH.of(AnyType),
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
        return@create BoolVariable(
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

@Suppress("UNCHECKED_CAST")
private val sortFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "sort",
    parameters = listOf(Resolver.SHALLOW_MATCH.of(EmptyArrayType)),
    returnType = UndefinedType,
    execute = { args ->
        (args[0].value as MutableList<Variable<*>>).sort()
        return@create UndefinedVariable
    },
)

internal val arrayBuiltinFunctions = listOf(
    lengthFunction,
    removeAtFunction,
    insertAtFunction,
    insertFunction,
    containsFunction,
    sortFunction,
)
