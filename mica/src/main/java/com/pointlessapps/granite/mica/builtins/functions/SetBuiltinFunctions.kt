package com.pointlessapps.granite.mica.builtins.functions

import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Companion.of
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Resolver
import com.pointlessapps.granite.mica.mapper.asSetType
import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.EmptyGenericType
import com.pointlessapps.granite.mica.model.EmptySetType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.model.SetType
import com.pointlessapps.granite.mica.model.UndefinedType

private val lengthFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "length",
    accessType = FunctionOverload.AccessType.GLOBAL_AND_MEMBER,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SHALLOW_MATCH.of(EmptySetType)),
    returnType = IntType,
    execute = { _, args ->
        val list = args[0].asSetType()
        return@create list.size.toLong()
    },
)

private val removeFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "remove",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = AnyType,
    parameters = listOf(
        Resolver.SHALLOW_MATCH.of(SetType(EmptyGenericType)),
        Resolver.EXACT_MATCH.of(EmptyGenericType),
    ),
    returnType = BoolType,
    execute = { _, args ->
        val set = args[0].asSetType()
        return@create set.remove(args[1])
    },
)

private val clearFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "clear",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SHALLOW_MATCH.of(EmptySetType)),
    returnType = UndefinedType,
    execute = { _, args ->
        args[0].asSetType().clear()
        return@create null
    },
)

private val insertFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "insert",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = AnyType,
    parameters = listOf(
        Resolver.SHALLOW_MATCH.of(SetType(EmptyGenericType)),
        Resolver.EXACT_MATCH.of(EmptyGenericType),
    ),
    returnType = UndefinedType,
    execute = { _, args ->
        val set = args[0].asSetType() as MutableSet<Any?>
        set.add(args[1])
        return@create null
    },
)

private val containsFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "contains",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = AnyType,
    parameters = listOf(
        Resolver.SHALLOW_MATCH.of(SetType(EmptyGenericType)),
        Resolver.EXACT_MATCH.of(EmptyGenericType),
    ),
    returnType = BoolType,
    execute = { _, args ->
        val set = args[0].asSetType()
        return@create set.contains(args[1])
    },
)

internal val setBuiltinFunctions = listOf(
    lengthFunction,
    removeFunction,
    clearFunction,
    insertFunction,
    containsFunction,
)
