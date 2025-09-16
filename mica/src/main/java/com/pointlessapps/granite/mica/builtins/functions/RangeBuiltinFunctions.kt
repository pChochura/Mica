package com.pointlessapps.granite.mica.builtins.functions

import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Companion.of
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Resolver
import com.pointlessapps.granite.mica.mapper.asRealRangeType
import com.pointlessapps.granite.mica.mapper.asRealType
import com.pointlessapps.granite.mica.mapper.toType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.RealRangeType
import com.pointlessapps.granite.mica.model.RealType
import com.pointlessapps.granite.mica.runtime.model.VariableType

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
        if (!args[1].value.toType().isSubtypeOf(RealType)) {
            throw IllegalArgumentException("Function contains expects $RealType as a first argument")
        }

        val range = args[0].value.asRealRangeType()
        val value = args[1].value.asRealType()
        return@create VariableType.Value(range.contains(value))
    },
)

private val minFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "min",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.EXACT_MATCH.of(RealRangeType)),
    returnType = RealType,
    execute = { _, args -> VariableType.Value(args[0].value.asRealRangeType().min) },
)

private val maxFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "max",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.EXACT_MATCH.of(RealRangeType)),
    returnType = RealType,
    execute = { _, args -> VariableType.Value(args[0].value.asRealRangeType().max) },
)

internal val rangeBuiltinFunctions = listOf(
    containsFunction,
    minFunction,
    maxFunction,
)
