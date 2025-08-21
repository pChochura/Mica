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
            fun Any.asString(type: Type): String = when {
                type.isSubtypeOf(IntType) ->
                    (type.valueAsSupertype<IntType>(this) as Long).toString()

                type.isSubtypeOf(BoolType) ->
                    if (type.valueAsSupertype<BoolType>(this) as Boolean) "true" else "false"

                type.isSubtypeOf(CharType) ->
                    (type.valueAsSupertype<CharType>(this) as Char).toString()

                type.isSubtypeOf(RealType) ->
                    (type.valueAsSupertype<RealType>(this) as Double).toString()

                type.isSubtypeOf(CharRangeType) ->
                    (type.valueAsSupertype<CharRangeType>(this) as CharRange).toString()

                type.isSubtypeOf(IntRangeType) ->
                    (type.valueAsSupertype<IntRangeType>(this) as LongRange).toString()

                type.isSubtypeOf(RealRangeType) ->
                    (type.valueAsSupertype<RealRangeType>(this) as ClosedDoubleRange).toString()

                type.isSubtypeOf(StringType) -> type.valueAsSupertype<StringType>(this) as String

                type.isSubtypeOf<ArrayType>() -> (type.valueAsSupertype<ArrayType>(this) as List<*>)
                    .joinToString(prefix = "[", postfix = "]") {
                        (it as Variable<*>).let { variable ->
                            requireNotNull(variable.value?.asString(variable.type))
                        }
                    }

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
            val type = args[0].first
            IntType to when {
                type.isSubtypeOf(IntType) -> type.valueAsSupertype<IntType>(value) as Long
                type.isSubtypeOf(BoolType) ->
                    if (type.valueAsSupertype<BoolType>(value) as Boolean) 1L else 0L

                type.isSubtypeOf(CharType) ->
                    (type.valueAsSupertype<CharType>(value) as Char).code.toLong()

                type.isSubtypeOf(RealType) ->
                    (type.valueAsSupertype<RealType>(value) as Double).toLong()

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
        execute = { args ->
            val list = args[0].first.valueAsSupertype<ArrayType>(args[0].second) as List<*>
            IntType to list.size.toLong()
        },
    ),
    BuiltinFunctionDeclarationBuilder.create(
        name = "removeAt",
        parameters = listOf(
            "list" to ArrayType(AnyType),
            "index" to IntType,
        ),
        getReturnType = { argTypes -> argTypes[0] },
        execute = { args ->
            val list = args[0].first.valueAsSupertype<ArrayType>(args[0].second) as List<*>
            val index = args[1].first.valueAsSupertype<IntType>(args[1].second) as Long
            args[0].first to list.toMutableList().apply { removeAt(index.toInt()) }
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
            val list = args[0].first.valueAsSupertype<ArrayType>(args[0].second) as List<*>
            val index = args[1].first.valueAsSupertype<IntType>(args[1].second) as Long
            val value = args[2].second

            val elementType = args[0].first.superTypes
                .filterIsInstance<ArrayType>()
                .first().elementType

            if (!args[2].first.isSubtypeOf(elementType)) {
                throw IllegalArgumentException(
                    "set function expects an ${elementType.name} as `value` argument, got ${
                        args[2].first.name
                    }",
                )
            }

            args[0].first to list.toMutableList().apply { set(index.toInt(), value) }
        },
    ),
)

internal val builtinFunctionDeclarations = builtinFunctions.associate {
    val parameterTypes = it.parameters.map(Pair<String, Type>::second)
    (it.name to it.parameters.size) to mutableMapOf(
        parameterTypes to it,
    )
}
