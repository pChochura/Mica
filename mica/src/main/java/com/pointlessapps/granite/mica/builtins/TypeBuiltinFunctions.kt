package com.pointlessapps.granite.mica.builtins

import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Companion.of
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Resolver
import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.runtime.model.BoolVariable
import com.pointlessapps.granite.mica.runtime.model.StringVariable

internal val typeOfFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "typeOf",
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    returnType = StringType,
    execute = { args -> StringVariable(args.first().type.name) },
)

internal val isSubtypeOfFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "isSubtypeOf",
    parameters = listOf(
        Resolver.SUBTYPE_MATCH.of(AnyType),
        Resolver.SUBTYPE_MATCH.of(StringType),
    ),
    returnType = BoolType,
    execute = { args -> BoolVariable(args[0].type.superTypes.any { it.name == args[1].value }) },
)
