package com.pointlessapps.granite.mica.builtins

import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.model.Type

internal val builtinFunctions = listOf(
    BuiltinFunctionDeclaration(
        name = "length",
        parameters = listOf<Pair<String, Type>>(
            "list" to ArrayType(AnyType),
        ),
        getReturnType = { IntType },
        execute = { arguments ->
            if (arguments.size != 1) {
                throw IllegalArgumentException(
                    "length function expects 1 argument, got ${arguments.size}",
                )
            }

            if (arguments[0].first !is ArrayType) {
                throw IllegalArgumentException(
                    "removeAt function expects a [any] as the first argument, got ${
                        arguments[0].first.name
                    }",
                )
            }

            val list = arguments[0].second as List<*>
            return@BuiltinFunctionDeclaration IntType to list.size.toLong()
        }
    ),
    BuiltinFunctionDeclaration(
        name = "removeAt",
        parameters = listOf(
            "list" to ArrayType(AnyType),
            "index" to IntType,
        ),
        getReturnType = { argumentTypes -> argumentTypes[0] },
        execute = { arguments ->
            if (arguments.size != 2) {
                throw IllegalArgumentException(
                    "removeAt function expects 2 arguments, got ${arguments.size}",
                )
            }

            if (arguments[0].first !is ArrayType) {
                throw IllegalArgumentException(
                    "removeAt function expects a [any] as the first argument, got ${
                        arguments[0].first.name
                    }",
                )
            }

            if (arguments[1].first != IntType) {
                throw IllegalArgumentException(
                    "removeAt function expects an int as the second argument, got ${
                        arguments[1].first.name
                    }",
                )
            }

            val list = arguments[0].second as List<*>
            return@BuiltinFunctionDeclaration arguments[0].first to list.toMutableList()
                .apply { removeAt((arguments[1].second as Long).toInt()) }
        },
    ),
    BuiltinFunctionDeclaration(
        name = "set",
        parameters = listOf(
            "list" to ArrayType(AnyType),
            "index" to IntType,
            "value" to AnyType,
        ),
        getReturnType = { arguments -> arguments[0] },
        execute = { arguments ->
            if (arguments.size != 3) {
                throw IllegalArgumentException(
                    "set function expects 3 arguments, got ${arguments.size}",
                )
            }

            if (arguments[0].first !is ArrayType) {
                throw IllegalArgumentException(
                    "set function expects a [any] as the first argument, got ${
                        arguments[0].first.name
                    }",
                )
            }

            if (arguments[1].first != IntType) {
                throw IllegalArgumentException(
                    "set function expects an int as the second argument, got ${
                        arguments[1].first.name
                    }",
                )
            }

            val elementType = arguments[0].first as ArrayType
            if (arguments[2].first != elementType) {
                throw IllegalArgumentException(
                    "set function expects an ${elementType.name} as the third argument, got ${
                        arguments[2].first.name
                    }",
                )
            }

            val list = arguments[0].second as List<*>
            return@BuiltinFunctionDeclaration arguments[0].first to list.toMutableList()
                .apply { set((arguments[1].second as Long).toInt(), arguments[2].second) }
        },
    ),
)

internal val builtinFunctionSignatures =
    builtinFunctions.associateBy(BuiltinFunctionDeclaration::signature)
