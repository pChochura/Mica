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

            val list = arguments[0].second.coerceToType(
                originalType = arguments[0].first,
                targetType = ArrayType(AnyType),
            )

            return@BuiltinFunctionDeclaration NumberType to (list as List<*>).size.toDouble()
        }
    ),
    BuiltinFunctionDeclaration(
        name = "removeAt",
        parameters = listOf(
            "list" to ArrayType(AnyType),
            "index" to NumberType,
        ),
        getReturnType = { argumentTypes -> argumentTypes[0] },
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
                    "removeAt function expects a number as the second argument, got ${
                        arguments[1].first.name
                    }",
                )
            }

            val list = arguments[0].second.coerceToType(
                originalType = arguments[0].first,
                targetType = ArrayType(AnyType),
            )
            return@BuiltinFunctionDeclaration arguments[0].first to (list as List<*>).toMutableList()
                .apply {
                    val index = arguments[1].second.coerceToType(arguments[1].first, NumberType)
                    removeAt((index as Double).toInt())
                }
        },
    ),
    BuiltinFunctionDeclaration(
        name = "set",
        parameters = listOf(
            "list" to ArrayType(AnyType),
            "index" to NumberType,
            "value" to AnyType,
        ),
        getReturnType = { arguments -> arguments[0] },
        execute = { arguments ->
            if (arguments.size != 3) {
                throw IllegalArgumentException(
                    "set function expects 3 arguments, got ${arguments.size}",
                )
            }

            if (!arguments[0].first.canBeCoercedTo(ArrayType(AnyType))) {
                throw IllegalArgumentException(
                    "set function expects a [any] as the first argument, got ${
                        arguments[0].first.name
                    }",
                )
            }

            if (!arguments[1].first.canBeCoercedTo(NumberType)) {
                throw IllegalArgumentException(
                    "set function expects a number as the second argument, got ${
                        arguments[1].first.name
                    }",
                )
            }

            if (!arguments[2].first.canBeCoercedTo(AnyType)) {
                throw IllegalArgumentException(
                    "set function expects a any as the third argument, got ${
                        arguments[2].first.name
                    }",
                )
            }

            val elementType = arguments[0].first.resolveElementTypeCoercedToArray()
            val list = arguments[0].second.coerceToType(
                originalType = arguments[0].first,
                targetType = ArrayType(elementType),
            )
            return@BuiltinFunctionDeclaration arguments[0].first to (list as List<*>).toMutableList()
                .apply {
                    val index = arguments[1].second.coerceToType(arguments[1].first, NumberType)
                    val value = arguments[2].second.coerceToType(arguments[2].first, elementType)
                    set((index as Double).toInt(), value)
                }
        },
    ),
)

internal val builtinFunctionSignatures = builtinFunctions.map(BuiltinFunctionDeclaration::signature)
