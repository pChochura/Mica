package com.pointlessapps.granite.mica.builtins

import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Companion.of
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Resolver
import com.pointlessapps.granite.mica.mapper.asStringType
import com.pointlessapps.granite.mica.mapper.toType
import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.runtime.model.VariableType

private val typeOfFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "typeOf",
    accessType = FunctionOverload.AccessType.GLOBAL_ONLY,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    returnType = StringType,
    execute = { args -> VariableType.Value(args[0].value.toType().name) },
)

private val isSubtypeOfFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "isSubtypeOf",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    parameters = listOf(
        Resolver.SUBTYPE_MATCH.of(AnyType),
        Resolver.SUBTYPE_MATCH.of(StringType),
    ),
    returnType = BoolType,
    execute = { args ->
        VariableType.Value(
            args[0].value.toType().superTypes.any { it.name == args[1].value.asStringType() },
        )
    },
)

internal val typeBuiltinFunctions = listOf(
    typeOfFunction,
    isSubtypeOfFunction,
)
