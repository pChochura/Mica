package com.pointlessapps.granite.mica.builtins

import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Companion.of
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Resolver
import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.CharRangeType
import com.pointlessapps.granite.mica.model.CharType
import com.pointlessapps.granite.mica.model.ClosedDoubleRange
import com.pointlessapps.granite.mica.model.EmptyArrayType
import com.pointlessapps.granite.mica.model.EmptyCustomType
import com.pointlessapps.granite.mica.model.EmptySetType
import com.pointlessapps.granite.mica.model.IntRangeType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.model.RealRangeType
import com.pointlessapps.granite.mica.model.RealType
import com.pointlessapps.granite.mica.model.SetType
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.runtime.helper.CustomObject
import com.pointlessapps.granite.mica.runtime.model.BoolVariable
import com.pointlessapps.granite.mica.runtime.model.CharRangeVariable
import com.pointlessapps.granite.mica.runtime.model.CharVariable
import com.pointlessapps.granite.mica.runtime.model.IntRangeVariable
import com.pointlessapps.granite.mica.runtime.model.IntVariable
import com.pointlessapps.granite.mica.runtime.model.RealRangeVariable
import com.pointlessapps.granite.mica.runtime.model.RealVariable
import com.pointlessapps.granite.mica.runtime.model.SetVariable
import com.pointlessapps.granite.mica.runtime.model.StringVariable
import com.pointlessapps.granite.mica.runtime.model.Variable
import com.pointlessapps.granite.mica.runtime.model.Variable.Companion.toVariable

private val toIntFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toInt",
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    returnType = IntType,
    execute = { args ->
        val value = args[0].value
        val type = args[0].type
        return@create IntVariable(
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

private val toRealFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toReal",
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    returnType = RealType,
    execute = { args ->
        val value = args[0].value
        val type = args[0].type
        return@create RealVariable(
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

private val toBoolFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toBool",
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    returnType = BoolType,
    execute = { args ->
        val value = args[0].value
        val type = args[0].type
        return@create BoolVariable(
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

private val toCharFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toChar",
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    returnType = CharType,
    execute = { args ->
        val value = args[0].value
        val type = args[0].type
        return@create CharVariable(
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

private val toStringFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toString",
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    returnType = StringType,
    execute = { args ->
        fun Variable<*>.asString(): String = when (value) {
            is Boolean, is Char, is Long, is Double, is String,
            is CharRange, is LongRange, is ClosedDoubleRange,
                -> value.toString()

            is Set<*> -> value.joinToString(
                prefix = "{",
                postfix = "}",
            ) { (it as Variable<*>).asString() }

            is Map<*, *> -> value.entries.joinToString(
                prefix = "{",
                postfix = "}",
            ) { (name, variable) -> "$name: ${(variable as Variable<*>).asString()}" }

            is List<*> -> value.joinToString(
                prefix = "[",
                postfix = "]",
            ) { (it as Variable<*>).asString() }

            else -> throw IllegalArgumentException("toString function cannot be applied to ${type.name}")
        }

        return@create StringVariable(args[0].asString())
    },
)

private val toArrayFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toArray",
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(EmptyArrayType)),
    getReturnType = { it[0].superTypes.filterIsInstance<ArrayType>().first() },
    execute = { args ->
        args[0].type.superTypes.filterIsInstance<ArrayType>().first().toVariable(
            args[0].type.valueAsSupertype<ArrayType>(args[0].value) as List<*>,
        )
    },
)

private val toSetFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toSet",
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(EmptyArrayType)),
    getReturnType = { SetType(it[0].superTypes.filterIsInstance<ArrayType>().first().elementType) },
    execute = { args ->
        SetVariable(
            (args[0].type.valueAsSupertype<ArrayType>(args[0].value) as List<*>).toMutableSet(),
            args[0].type.superTypes.filterIsInstance<ArrayType>().first().elementType,
        )
    },
)

private val toIntRangeFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toIntRange",
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    returnType = IntRangeType,
    execute = { args ->
        val value = args[0].value
        val type = args[0].type
        return@create IntRangeVariable(
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

private val toRealRangeFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toRealRange",
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    returnType = RealRangeType,
    execute = { args ->
        val value = args[0].value
        val type = args[0].type
        return@create RealRangeVariable(
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

private val toCharRangeFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toCharRange",
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    returnType = CharRangeType,
    execute = { args ->
        val value = args[0].value
        val type = args[0].type
        return@create CharRangeVariable(
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

internal val typeConversionBuiltinFunctions = listOf(
    toIntFunction,
    toRealFunction,
    toBoolFunction,
    toCharFunction,
    toStringFunction,
    toArrayFunction,
    toSetFunction,
    toIntRangeFunction,
    toRealRangeFunction,
    toCharRangeFunction,
)
