package com.pointlessapps.granite.mica.builtins

import com.pointlessapps.granite.mica.linter.resolver.TypeCoercionResolver.canBeCoercedTo
import com.pointlessapps.granite.mica.linter.resolver.TypeCoercionResolver.resolveElementTypeCoercedToArray
import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.NumberType
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.runtime.resolver.ValueCoercionResolver.coerceToType

internal val builtinFunctions = listOf(
    BuiltinFunctionDeclaration(
        name = "length",
        parameters = listOf<Pair<String, Type>>(
            "list" to ArrayType(AnyType),
        ),
        getReturnType = { NumberType },
        execute = { arguments ->
            if (arguments.size != 1) {
                throw IllegalArgumentException(
                    "length function expects 1 argument, got ${arguments.size}",
                )
            }

            if (!arguments[0].first.canBeCoercedTo(ArrayType(AnyType))) {
                throw IllegalArgumentException(
                    "removeAt function expects a [any] as the first argument, got ${
                        arguments[0].first.name
                    }",
                )
            }

            return@BuiltinFunctionDeclaration (arguments[0].second as List<*>).size.toDouble()
        }
    ),
    BuiltinFunctionDeclaration(
        name = "removeAt",
        parameters = listOf(
            "list" to ArrayType(AnyType),
            "index" to NumberType,
        ),
        getReturnType = { argumentTypes ->
            ArrayType(argumentTypes[0].resolveElementTypeCoercedToArray())
        },
        execute = { arguments ->
            if (arguments.size != 2) {
                throw IllegalArgumentException(
                    "removeAt function expects 2 arguments, got ${arguments.size}",
                )
            }

            if (!arguments[0].first.canBeCoercedTo(ArrayType(AnyType))) {
                throw IllegalArgumentException(
                    "removeAt function expects a [any] as the first argument, got ${
                        arguments[0].first.name
                    }",
                )
            }

            if (!arguments[1].first.canBeCoercedTo(NumberType)) {
                throw IllegalArgumentException(
                    "removeAt function expects a [number] as the second argument, got ${
                        arguments[1].first.name
                    }",
                )
            }

            val list = arguments[0].second.coerceToType(
                originalType = arguments[0].first,
                targetType = ArrayType(AnyType),
            )
            return@BuiltinFunctionDeclaration (list as List<*>).toMutableList().apply {
                val index = arguments[1].second.coerceToType(arguments[1].first, NumberType)
                removeAt((index as Double).toInt())
            }
        },
    )
)

internal val builtinFunctionSignatures = builtinFunctions.map(BuiltinFunctionDeclaration::signature)
