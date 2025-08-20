package com.pointlessapps.granite.mica.builtins

import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.CharRangeType
import com.pointlessapps.granite.mica.model.CharType
import com.pointlessapps.granite.mica.model.ClosedDoubleRange
import com.pointlessapps.granite.mica.model.IntRangeType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.model.RealRangeType
import com.pointlessapps.granite.mica.model.RealType
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.model.Type

internal val builtinFunctions = listOf(
    BuiltinFunctionDeclaration(
        name = "toInt",
        parameters = listOf<Pair<String, Type>>(
            "value" to AnyType,
        ),
        getReturnType = { IntType },
        execute = { arguments ->
            if (arguments.size != 1) {
                throw IllegalArgumentException(
                    "toInt function expects 1 argument, got ${arguments.size}",
                )
            }

            if (!arguments[0].first.isSupertypeOf(AnyType)) {
                throw IllegalArgumentException(
                    "toInt function expects an any as the first argument, got ${
                        arguments[0].first.name
                    }",
                )
            }

            val value = arguments[0].second
            val intValue = when (arguments[0].first) {
                IntType -> value as Long
                BoolType -> if (value as Boolean) 1L else 0L
                CharType -> (value as Char).code.toLong()
                RealType -> (value as Double).toLong()
                else -> throw IllegalArgumentException(
                    "toInt function cannot be applied to ${arguments[0].first.name}",
                )
            }

            return@BuiltinFunctionDeclaration IntType to intValue
        }
    ),
    BuiltinFunctionDeclaration(
        name = "toString",
        parameters = listOf<Pair<String, Type>>(
            "value" to AnyType,
        ),
        getReturnType = { StringType },
        execute = { arguments ->
            if (arguments.size != 1) {
                throw IllegalArgumentException(
                    "toString function expects 1 argument, got ${arguments.size}",
                )
            }

            if (!arguments[0].first.isSupertypeOf(AnyType)) {
                throw IllegalArgumentException(
                    "toString function expects an any as the first argument, got ${
                        arguments[0].first.name
                    }",
                )
            }

            fun Any.asString(type: Type): String = when (type) {
                IntType -> (this as Long).toString()
                BoolType -> if (this as Boolean) "true" else "false"
                CharType -> (this as Char).toString()
                RealType -> (this as Double).toString()
                is ArrayType -> (this as List<*>).joinToString(prefix = "[", postfix = "]") {
                    requireNotNull(it?.asString(type.elementType))
                }
                CharRangeType -> (this as CharRange).toString()
                IntRangeType -> (this as IntRange).toString()
                RealRangeType -> (this as ClosedDoubleRange).toString()
                StringType -> this as String
                else -> throw IllegalArgumentException(
                    "toString function cannot be applied to ${arguments[0].first.name}",
                )
            }

            val stringValue = arguments[0].second.asString(arguments[0].first)
            return@BuiltinFunctionDeclaration StringType to stringValue
        }
    ),
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
                    "length function expects a [any] as the first argument, got ${
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

internal val builtinFunctionDeclarations = builtinFunctions.associate {
    val parameterTypes = it.parameters.map(Pair<String, Type>::second)
    (it.name to it.parameters.size) to mutableMapOf(
        parameterTypes to it,
    )
}
