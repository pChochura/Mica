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
import com.pointlessapps.granite.mica.runtime.model.Variable.Companion.toVariable

internal val toIntFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toInt",
    parameters = listOf("value" to AnyType),
    returnType = IntType,
    execute = { args ->
        val value = args[0].value
        val type = args[0].type
        IntType.toVariable(
            when {
                type.isSubtypeOf(IntType) -> type.valueAsSupertype<IntType>(value) as Long
                type.isSubtypeOf(BoolType) ->
                    if (type.valueAsSupertype<BoolType>(value) as Boolean) 1L else 0L

                type.isSubtypeOf(CharType) ->
                    (type.valueAsSupertype<CharType>(value) as Char).code.toLong()

                type.isSubtypeOf(RealType) ->
                    (type.valueAsSupertype<RealType>(value) as Double).toLong()

                else -> throw IllegalArgumentException(
                    "toInt function cannot be applied to ${args[0].type.name}",
                )
            },
        )
    },
)

internal val toRealFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toReal",
    parameters = listOf("value" to AnyType),
    returnType = RealType,
    execute = { args ->
        val value = args[0].value
        val type = args[0].type
        RealType.toVariable(
            when {
                type.isSubtypeOf(IntType) -> (type.valueAsSupertype<IntType>(value) as Long).toDouble()
                type.isSubtypeOf(RealType) -> type.valueAsSupertype<RealType>(value) as Double
                else -> throw IllegalArgumentException(
                    "toReal function cannot be applied to ${args[0].type.name}",
                )
            },
        )
    },
)

internal val toBoolFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toBool",
    parameters = listOf("value" to AnyType),
    returnType = BoolType,
    execute = { args ->
        val value = args[0].value
        val type = args[0].type
        BoolType.toVariable(
            when {
                type.isSubtypeOf(IntType) -> (type.valueAsSupertype<IntType>(value) as Long) != 0L
                type.isSubtypeOf(BoolType) -> type.valueAsSupertype<BoolType>(value) as Boolean
                else -> throw IllegalArgumentException(
                    "toBool function cannot be applied to ${args[0].type.name}",
                )
            },
        )
    },
)

internal val toCharFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toChar",
    parameters = listOf("value" to AnyType),
    returnType = CharType,
    execute = { args ->
        val value = args[0].value
        val type = args[0].type
        CharType.toVariable(
            when {
                type.isSubtypeOf(IntType) -> Char((type.valueAsSupertype<IntType>(value) as Long).toInt())
                type.isSubtypeOf(CharType) -> type.valueAsSupertype<CharType>(value) as Char
                else -> throw IllegalArgumentException(
                    "toChar function cannot be applied to ${args[0].type.name}",
                )
            },
        )
    },
)

internal val toStringFunction = BuiltinFunctionDeclarationBuilder.create(
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

        StringType.toVariable(args[0].value?.asString(args[0].type))
    },
)

internal val toArrayFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toArray",
    parameters = listOf("value" to ArrayType(AnyType)),
    getReturnType = { argTypes -> argTypes[0].superTypes.filterIsInstance<ArrayType>().first() },
    execute = { args ->
        args[0].type.superTypes.filterIsInstance<ArrayType>().first().toVariable(
            args[0].type.valueAsSupertype<ArrayType>(args[0].value) as List<*>,
        )
    },
)

internal val toIntRangeFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toIntRange",
    parameters = listOf("value" to AnyType),
    returnType = IntRangeType,
    execute = { args ->
        val value = args[0].value
        val type = args[0].type
        IntRangeType.toVariable(
            when {
                type.isSubtypeOf(IntRangeType) -> type.valueAsSupertype<IntRangeType>(value) as LongRange
                type.isSubtypeOf(CharRangeType) ->
                    (type.valueAsSupertype<CharRangeType>(value) as CharRange).let {
                        LongRange(it.start.code.toLong(), it.endInclusive.code.toLong())
                    }

                type.isSubtypeOf(RealRangeType) ->
                    (type.valueAsSupertype<RealRangeType>(value) as ClosedDoubleRange).let {
                        LongRange(it.start.toLong(), it.endInclusive.toLong())
                    }

                else -> throw IllegalArgumentException(
                    "toIntRange function cannot be applied to ${args[0].type.name}",
                )
            },
        )
    },
)

internal val toRealRangeFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toRealRange",
    parameters = listOf("value" to AnyType),
    returnType = RealRangeType,
    execute = { args ->
        val value = args[0].value
        val type = args[0].type
        RealRangeType.toVariable(
            when {
                type.isSubtypeOf(IntRangeType) ->
                    (type.valueAsSupertype<IntRangeType>(value) as LongRange).let {
                        ClosedDoubleRange(it.start.toDouble(), it.endInclusive.toDouble())
                    }

                type.isSubtypeOf(CharRangeType) ->
                    (type.valueAsSupertype<CharRangeType>(value) as CharRange).let {
                        ClosedDoubleRange(it.start.code.toDouble(), it.endInclusive.code.toDouble())
                    }

                type.isSubtypeOf(RealRangeType) ->
                    type.valueAsSupertype<RealRangeType>(value) as ClosedDoubleRange

                else -> throw IllegalArgumentException(
                    "toRealRange function cannot be applied to ${args[0].type.name}",
                )
            },
        )
    },
)

internal val toCharRangeFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toCharRange",
    parameters = listOf("value" to AnyType),
    returnType = CharRangeType,
    execute = { args ->
        val value = args[0].value
        val type = args[0].type
        CharRangeType.toVariable(
            when {
                type.isSubtypeOf(IntRangeType) ->
                    (type.valueAsSupertype<IntRangeType>(value) as LongRange).let {
                        CharRange(Char(it.start.toInt()), Char(it.endInclusive.toInt()))
                    }

                type.isSubtypeOf(CharRangeType) -> type.valueAsSupertype<CharRangeType>(value) as CharRange
                type.isSubtypeOf(RealRangeType) ->
                    (type.valueAsSupertype<RealRangeType>(value) as ClosedDoubleRange).let {
                        CharRange(Char(it.start.toInt()), Char(it.endInclusive.toInt()))
                    }

                else -> throw IllegalArgumentException(
                    "toCharRange function cannot be applied to ${args[0].type.name}",
                )
            },
        )
    },
)
