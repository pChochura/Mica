package com.pointlessapps.granite.mica.builtins.functions

import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Companion.of
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Resolver
import com.pointlessapps.granite.mica.mapper.asStringType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.runtime.model.VariableType

private val containsFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "contains",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    parameters = listOf(
        Resolver.SUBTYPE_MATCH.of(StringType),
        Resolver.SUBTYPE_MATCH.of(StringType),
    ),
    returnType = BoolType,
    execute = { _, args ->
        val string = args[0].value.asStringType()
        val value = args[1].value.asStringType()
        return@create VariableType.Value(string.contains(value))
    },
)

private val startsWithFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "startsWith",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    parameters = listOf(
        Resolver.SUBTYPE_MATCH.of(StringType),
        Resolver.SUBTYPE_MATCH.of(StringType),
    ),
    returnType = BoolType,
    execute = { _, args ->
        val string = args[0].value.asStringType()
        val value = args[1].value.asStringType()
        return@create VariableType.Value(string.startsWith(value))
    },
)

private val endsWithFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "endsWith",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    parameters = listOf(
        Resolver.SUBTYPE_MATCH.of(StringType),
        Resolver.SUBTYPE_MATCH.of(StringType),
    ),
    returnType = BoolType,
    execute = { _, args ->
        val string = args[0].value.asStringType()
        val value = args[1].value.asStringType()
        return@create VariableType.Value(string.endsWith(value))
    },
)

private val indexOfFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "indexOf",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    parameters = listOf(
        Resolver.SUBTYPE_MATCH.of(StringType),
        Resolver.SUBTYPE_MATCH.of(StringType),
    ),
    returnType = IntType,
    execute = { _, args ->
        val string = args[0].value.asStringType()
        val value = args[1].value.asStringType()
        return@create VariableType.Value(string.indexOf(value).toLong())
    },
)

internal val stringBuiltinFunctions = listOf(
    containsFunction,
    startsWithFunction,
    endsWithFunction,
    indexOfFunction,
)
