package com.pointlessapps.granite.mica.builtins.functions

import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Companion.of
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Resolver
import com.pointlessapps.granite.mica.mapper.asRealRangeType
import com.pointlessapps.granite.mica.mapper.asRealType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.RealRangeType
import com.pointlessapps.granite.mica.model.RealType

private val containsFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "contains",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(
        Resolver.EXACT_MATCH.of(RealRangeType),
        Resolver.SUBTYPE_MATCH.of(RealType),
    ),
    returnType = BoolType,
    execute = { _, args ->
        val range = args[0].asRealRangeType()
        val value = args[1].asRealType()
        return@create range.contains(value)
    },
)

private val minFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "min",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.EXACT_MATCH.of(RealRangeType)),
    returnType = RealType,
    execute = { _, args -> args[0].asRealRangeType().min },
)

private val maxFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "max",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.EXACT_MATCH.of(RealRangeType)),
    returnType = RealType,
    execute = { _, args -> args[0].asRealRangeType().max },
)

internal val rangeBuiltinFunctions = listOf(
    containsFunction,
    minFunction,
    maxFunction,
)
