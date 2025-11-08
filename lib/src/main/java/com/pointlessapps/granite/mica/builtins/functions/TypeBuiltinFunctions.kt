package com.pointlessapps.granite.mica.builtins.functions

import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Companion.of
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Resolver
import com.pointlessapps.granite.mica.mapper.asStringType
import com.pointlessapps.granite.mica.mapper.toType
import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.StringType

private val typeOfFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "typeOf",
    accessType = FunctionOverload.AccessType.GLOBAL_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    returnType = StringType,
    execute = { _, args -> args[0].toType().name },
)

private val isSubtypeOfFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "isSubtypeOf",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(
        Resolver.SUBTYPE_MATCH.of(AnyType),
        Resolver.SUBTYPE_MATCH.of(StringType),
    ),
    returnType = BoolType,
    execute = { _, args -> args[0].toType().superTypes.any { it.name == args[1].asStringType() } },
)

private val isSubtypeOf2Function = BuiltinFunctionDeclarationBuilder.create(
    name = "isSubtypeOf",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = AnyType,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    returnType = BoolType,
    execute = { typeArg, args ->
        if (typeArg == null) return@create false

        return@create args[0].toType().isSubtypeOf(typeArg)
    },
)

internal val typeBuiltinFunctions = listOf(
    typeOfFunction,
    isSubtypeOfFunction,
    isSubtypeOf2Function,
)
