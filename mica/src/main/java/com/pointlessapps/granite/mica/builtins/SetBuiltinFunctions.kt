package com.pointlessapps.granite.mica.builtins

import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Companion.of
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Resolver
import com.pointlessapps.granite.mica.mapper.asSetType
import com.pointlessapps.granite.mica.mapper.toType
import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.EmptySetType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.model.SetType
import com.pointlessapps.granite.mica.model.UndefinedType
import com.pointlessapps.granite.mica.runtime.model.VariableType

private val lengthFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "length",
    accessType = FunctionOverload.AccessType.GLOBAL_AND_MEMBER,
    parameters = listOf(Resolver.SHALLOW_MATCH.of(EmptySetType)),
    returnType = IntType,
    execute = { args ->
        val list = args[0].value.asSetType()
        return@create VariableType.Value(list.size.toLong())
    },
)

private val removeFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "remove",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    parameters = listOf(
        Resolver.SHALLOW_MATCH.of(EmptySetType),
        Resolver.SUBTYPE_MATCH.of(AnyType),
    ),
    getReturnType = { (it[0] as SetType).elementType },
    execute = { args ->
        val set = args[0].value.asSetType()
        val elementType = (set.toType() as SetType).elementType
        if (!args[1].value.toType().isSubtypeOf(elementType)) {
            throw IllegalArgumentException(
                "Function remove expects ${elementType.name} as a first argument",
            )
        }

        return@create VariableType.Value(set.remove(args[1].value))
    },
)

private val clearFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "clear",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    parameters = listOf(Resolver.SHALLOW_MATCH.of(EmptySetType)),
    returnType = UndefinedType,
    execute = { args ->
        args[0].value.asSetType().clear()
        return@create VariableType.Undefined
    },
)

@Suppress("UNCHECKED_CAST")
private val insertFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "insert",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    parameters = listOf(
        Resolver.SHALLOW_MATCH.of(EmptySetType),
        Resolver.SUBTYPE_MATCH.of(AnyType),
    ),
    returnType = UndefinedType,
    execute = { args ->
        val set = args[0].value.asSetType() as MutableSet<Any?>
        val elementType = (set.toType() as SetType).elementType
        if (!args[1].value.toType().isSubtypeOf(elementType)) {
            throw IllegalArgumentException(
                "Function insert expects ${elementType.name} as a first argument",
            )
        }

        set.add(args[1].value)
        return@create VariableType.Undefined
    },
)

private val containsFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "contains",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    parameters = listOf(
        Resolver.SHALLOW_MATCH.of(EmptySetType),
        Resolver.SUBTYPE_MATCH.of(AnyType),
    ),
    returnType = BoolType,
    execute = { args ->
        val set = args[0].value.asSetType()
        val elementType = (set.toType() as SetType).elementType
        if (!args[1].value.toType().isSubtypeOf(elementType)) {
            throw IllegalArgumentException(
                "Function contains expects ${elementType.name} as a first argument",
            )
        }

        return@create VariableType.Value(set.contains(args[1].value))
    },
)

internal val setBuiltinFunctions = listOf(
    lengthFunction,
    removeFunction,
    clearFunction,
    insertFunction,
    containsFunction,
)
