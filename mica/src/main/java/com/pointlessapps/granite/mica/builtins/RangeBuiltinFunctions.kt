package com.pointlessapps.granite.mica.builtins

import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Companion.of
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Resolver
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.ClosedDoubleRange
import com.pointlessapps.granite.mica.model.RealRangeType
import com.pointlessapps.granite.mica.model.RealType
import com.pointlessapps.granite.mica.runtime.model.BoolVariable

private val containsFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "contains",
    parameters = listOf(
        Resolver.SHALLOW_MATCH.of(RealRangeType),
        Resolver.SUBTYPE_MATCH.of(RealType),
    ),
    returnType = BoolType,
    execute = { args ->
        if (!args[1].type.isSubtypeOf(RealType)) {
            throw IllegalStateException(
                "Function contains expects ${RealType.name} as a second argument",
            )
        }

        val range = args[0].value as ClosedDoubleRange
        val value = args[1].type.valueAsSupertype(args[1].value, RealType) as Double
        return@create BoolVariable(range.contains(value))
    },
)

internal val rangeBuiltinFunctions = listOf(
    containsFunction,
)
