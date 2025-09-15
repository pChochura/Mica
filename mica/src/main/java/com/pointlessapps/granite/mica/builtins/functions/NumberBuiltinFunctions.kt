package com.pointlessapps.granite.mica.builtins.functions

import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Companion.of
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Resolver
import com.pointlessapps.granite.mica.mapper.asRealType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.model.RealType
import com.pointlessapps.granite.mica.runtime.model.VariableType
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToLong

private val roundUpFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "roundUp",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(RealType)),
    returnType = IntType,
    execute = { _, args ->
        val value = args[0].value.asRealType()
        return@create VariableType.Value(ceil(value).toLong())
    },
)

private val roundDownFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "roundDown",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(RealType)),
    returnType = IntType,
    execute = { _, args ->
        val value = args[0].value.asRealType()
        return@create VariableType.Value(floor(value).toLong())
    },
)

private val roundFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "round",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    typeParameterConstraint = null,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(RealType)),
    returnType = IntType,
    execute = { _, args ->
        val value = args[0].value.asRealType()
        return@create VariableType.Value(value.roundToLong())
    },
)

internal val numberBuiltinFunctions = listOf(
    roundUpFunction,
    roundDownFunction,
    roundFunction,
)
