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
import com.pointlessapps.granite.mica.runtime.model.Variable

internal val builtinFunctions = listOf(
    BuiltinFunctionDeclarationBuilder.create(
        name = "toString",
        parameters = listOf("value" to AnyType),
        returnType = StringType,
        execute = { args ->
            fun Any.asString(type: Type): String = when (type) {
                IntType -> (this as Long).toString()
                BoolType -> if (this as Boolean) "true" else "false"
                CharType -> (this as Char).toString()
                RealType -> (this as Double).toString()
                is ArrayType -> (this as List<*>).joinToString(prefix = "[", postfix = "]") {
                    (it as Variable<*>).let { variable ->
                        requireNotNull(variable.value?.asString(variable.type))
                    }
                }

                CharRangeType -> (this as CharRange).toString()
                IntRangeType -> (this as IntRange).toString()
                RealRangeType -> (this as ClosedDoubleRange).toString()
                StringType -> this as String
                else -> throw IllegalArgumentException(
                    "toString function cannot be applied to $type",
                )
            }

            StringType to args[0].second.asString(args[0].first)
        },
    ),
    BuiltinFunctionDeclarationBuilder.create(
        name = "toInt",
        parameters = listOf("value" to AnyType),
        returnType = IntType,
        execute = { args ->
            val value = args[0].second
            IntType to when (args[0].first) {
                IntType -> value as Long
                BoolType -> if (value as Boolean) 1L else 0L
                CharType -> (value as Char).code.toLong()
                RealType -> (value as Double).toLong()
                else -> throw IllegalArgumentException(
                    "toInt function cannot be applied to ${args[0].first.name}",
                )
            }
        },
    ),
    BuiltinFunctionDeclarationBuilder.create(
        name = "length",
        parameters = listOf("list" to ArrayType(AnyType)),
        returnType = IntType,
        execute = { args -> IntType to (args[0].second as List<*>).size.toLong() },
    ),
    BuiltinFunctionDeclarationBuilder.create(
        name = "removeAt",
        parameters = listOf(
            "list" to ArrayType(AnyType),
            "index" to IntType,
        ),
        getReturnType = { argTypes -> argTypes[0] },
        execute = { args ->
            args[0].first to (args[0].second as List<*>).toMutableList()
                .apply { removeAt((args[1].second as Long).toInt()) }
        },
    ),
    BuiltinFunctionDeclarationBuilder.create(
        name = "set",
        parameters = listOf(
            "list" to ArrayType(AnyType),
            "index" to IntType,
            "value" to AnyType,
        ),
        getReturnType = { argTypes -> argTypes[0] },
        execute = { args ->
            val elementType = args[0].first as ArrayType
            if (args[2].first != elementType) {
                throw IllegalArgumentException(
                    "set function expects an ${elementType.name} as `value` argument, got ${
                        args[2].first.name
                    }",
                )
            }

            args[0].first to (args[0].second as List<*>).toMutableList()
                .apply { set((args[1].second as Long).toInt(), args[2].second) }
        },
    ),
)

internal val builtinFunctionDeclarations = builtinFunctions.associate {
    val parameterTypes = it.parameters.map(Pair<String, Type>::second)
    (it.name to it.parameters.size) to mutableMapOf(
        parameterTypes to it,
    )
}
