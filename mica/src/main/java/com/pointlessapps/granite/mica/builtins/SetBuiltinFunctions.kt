package com.pointlessapps.granite.mica.builtins

import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Companion.of
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Resolver
import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.EmptySetType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.model.SetType
import com.pointlessapps.granite.mica.model.UndefinedType
import com.pointlessapps.granite.mica.runtime.model.BoolVariable
import com.pointlessapps.granite.mica.runtime.model.IntVariable
import com.pointlessapps.granite.mica.runtime.model.UndefinedVariable
import com.pointlessapps.granite.mica.runtime.model.Variable

private val lengthFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "length",
    parameters = listOf(Resolver.SHALLOW_MATCH.of(EmptySetType)),
    returnType = IntType,
    execute = { args ->
        val list = args[0].value as Set<*>
        return@create IntVariable(list.size.toLong())
    },
)

private val removeFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "remove",
    parameters = listOf(
        Resolver.SHALLOW_MATCH.of(EmptySetType),
        Resolver.SUBTYPE_MATCH.of(AnyType),
    ),
    getReturnType = { (it[0] as SetType).elementType },
    execute = { args ->
        val elementType = (args[0].type as SetType).elementType
        if (!args[1].type.isSubtypeOf(elementType)) {
            throw IllegalStateException(
                "Function remove expects ${elementType.name} as a second argument",
            )
        }

        val set = args[0].value as MutableSet<*>
        return@create BoolVariable(set.remove(args[1]))
    },
)

private val clearFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "clear",
    parameters = listOf(Resolver.SHALLOW_MATCH.of(EmptySetType)),
    returnType = UndefinedType,
    execute = { args ->
        (args[0].value as MutableSet<*>).clear()
        return@create UndefinedVariable
    },
)

@Suppress("UNCHECKED_CAST")
private val insertFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "insert",
    parameters = listOf(
        Resolver.SHALLOW_MATCH.of(EmptySetType),
        Resolver.SUBTYPE_MATCH.of(AnyType),
    ),
    returnType = UndefinedType,
    execute = { args ->
        val elementType = (args[0].type as SetType).elementType
        if (!args[1].type.isSubtypeOf(elementType)) {
            throw IllegalStateException(
                "Function insert expects ${elementType.name} as a second argument",
            )
        }

        val set = args[0].value as MutableSet<Variable<*>>
        set.add(args[1])
        return@create UndefinedVariable
    },
)

private val containsFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "contains",
    parameters = listOf(
        Resolver.SHALLOW_MATCH.of(EmptySetType),
        Resolver.SUBTYPE_MATCH.of(AnyType),
    ),
    returnType = BoolType,
    execute = { args ->
        val elementType = (args[0].type as SetType).elementType
        if (!args[1].type.isSubtypeOf(elementType)) {
            throw IllegalStateException(
                "Function contains expects ${elementType.name} as a second argument",
            )
        }

        val set = args[0].value as MutableSet<*>
        return@create BoolVariable(set.contains(args[1]))
    },
)

internal val setBuiltinFunctions = listOf(
    lengthFunction,
    removeFunction,
    clearFunction,
    insertFunction,
    containsFunction,
)
