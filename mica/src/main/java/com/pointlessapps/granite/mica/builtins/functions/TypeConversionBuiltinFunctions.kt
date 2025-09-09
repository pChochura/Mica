package com.pointlessapps.granite.mica.builtins.functions

import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Companion.of
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Resolver
import com.pointlessapps.granite.mica.mapper.asArrayType
import com.pointlessapps.granite.mica.mapper.asBoolType
import com.pointlessapps.granite.mica.mapper.asCharRangeType
import com.pointlessapps.granite.mica.mapper.asCharType
import com.pointlessapps.granite.mica.mapper.asIntRangeType
import com.pointlessapps.granite.mica.mapper.asIntType
import com.pointlessapps.granite.mica.mapper.asMapType
import com.pointlessapps.granite.mica.mapper.asRealRangeType
import com.pointlessapps.granite.mica.mapper.asRealType
import com.pointlessapps.granite.mica.mapper.toType
import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.CharRangeType
import com.pointlessapps.granite.mica.model.CharType
import com.pointlessapps.granite.mica.model.ClosedDoubleRange
import com.pointlessapps.granite.mica.model.CustomType
import com.pointlessapps.granite.mica.model.EmptyArrayType
import com.pointlessapps.granite.mica.model.EmptyMapType
import com.pointlessapps.granite.mica.model.IntRangeType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.model.MapType
import com.pointlessapps.granite.mica.model.RealRangeType
import com.pointlessapps.granite.mica.model.RealType
import com.pointlessapps.granite.mica.model.SetType
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.runtime.model.VariableType

private val toIntFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toInt",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    returnType = IntType,
    execute = { args ->
        val value = args[0].value
        val type = value.toType()
        return@create VariableType.Value(
            when {
                type.isSubtypeOf(IntType) -> value.asIntType()
                type.isSubtypeOf(BoolType) -> if (value.asBoolType()) 1L else 0L
                type.isSubtypeOf(CharType) -> value.asCharType().code.toLong()
                type.isSubtypeOf(RealType) -> value.asRealType().toLong()
                else -> throw IllegalArgumentException(
                    "toInt function cannot be applied to ${type.name}",
                )
            },
        )
    },
)

private val toRealFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toReal",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    returnType = RealType,
    execute = { args ->
        val value = args[0].value
        val type = value.toType()
        return@create VariableType.Value(
            when {
                type.isSubtypeOf(IntType) -> value.asIntType().toDouble()
                type.isSubtypeOf(RealType) -> value.asIntType()
                else -> throw IllegalArgumentException(
                    "toReal function cannot be applied to ${type.name}",
                )
            },
        )
    },
)

private val toBoolFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toBool",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    returnType = BoolType,
    execute = { args ->
        val value = args[0].value
        val type = value.toType()
        return@create VariableType.Value(
            when {
                type.isSubtypeOf(IntType) -> value.asIntType() != 0L
                type.isSubtypeOf(BoolType) -> value.asBoolType()
                else -> throw IllegalArgumentException(
                    "toBool function cannot be applied to ${type.name}",
                )
            },
        )
    },
)

private val toCharFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toChar",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    returnType = CharType,
    execute = { args ->
        val value = args[0].value
        val type = value.toType()
        return@create VariableType.Value(
            when {
                type.isSubtypeOf(IntType) -> Char(value.asIntType().toInt())
                type.isSubtypeOf(CharType) -> value.asCharType()
                else -> throw IllegalArgumentException(
                    "toChar function cannot be applied to ${type.name}",
                )
            },
        )
    },
)

private val toStringFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toString",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    returnType = StringType,
    execute = { args ->
        fun asString(value: Any?): String = when (value) {
            is Boolean, is Char, is Long, is Double, is String,
            is CharRange, is LongRange, is ClosedDoubleRange,
                -> value.toString()

            is Set<*> -> value.joinToString(prefix = "{", postfix = "}", transform = ::asString)
            is Map<*, *> -> if (value.containsKey(CustomType.NAME_PROPERTY)) {
                value.filterKeys { it != CustomType.NAME_PROPERTY }.entries.joinToString(
                    prefix = "${value[CustomType.NAME_PROPERTY]}{",
                    postfix = "}",
                ) { (key, value) -> "${asString(key)} = ${asString(value)}" }
            } else {
                value.entries.joinToString(
                    prefix = "{",
                    postfix = "}",
                ) { (key, value) -> "${asString(key)}: ${asString(value)}" }
            }

            is List<*> -> value.joinToString(prefix = "[", postfix = "]", transform = ::asString)

            else -> throw IllegalArgumentException(
                "toString function cannot be applied to ${value.toType().name}",
            )
        }

        return@create VariableType.Value(asString(args[0].value))
    },
)

private val toArrayFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toArray",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(EmptyArrayType)),
    getReturnType = { it[0].superTypes.filterIsInstance<ArrayType>().first() },
    execute = { args -> VariableType.Value(args[0].value.asArrayType()) },
)

private val toSetFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toSet",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(EmptyArrayType)),
    getReturnType = { SetType(it[0].superTypes.filterIsInstance<ArrayType>().first().elementType) },
    execute = { args -> VariableType.Value(args[0].value.asArrayType().toMutableSet()) },
)

private val toMapFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toMap",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(EmptyMapType)),
    getReturnType = { it[0].superTypes.filterIsInstance<MapType>().first() },
    execute = { args -> VariableType.Value(args[0].value.asMapType()) },
)

private val toIntRangeFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toIntRange",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    returnType = IntRangeType,
    execute = { args ->
        val value = args[0].value
        val type = value.toType()
        return@create VariableType.Value(
            when {
                type.isSubtypeOf(IntRangeType) -> value.asIntRangeType()
                type.isSubtypeOf(CharRangeType) -> value.asCharRangeType().let {
                    LongRange(it.start.code.toLong(), it.endInclusive.code.toLong())
                }

                type.isSubtypeOf(RealRangeType) -> value.asRealRangeType().let {
                    LongRange(it.start.toLong(), it.endInclusive.toLong())
                }

                else -> throw IllegalArgumentException(
                    "toIntRange function cannot be applied to ${type.name}",
                )
            },
        )
    },
)

private val toRealRangeFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toRealRange",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    returnType = RealRangeType,
    execute = { args ->
        val value = args[0].value
        val type = value.toType()
        return@create VariableType.Value(
            when {
                type.isSubtypeOf(IntRangeType) -> value.asIntRangeType().let {
                    ClosedDoubleRange(it.start.toDouble(), it.endInclusive.toDouble())
                }

                type.isSubtypeOf(CharRangeType) -> value.asCharRangeType().let {
                    ClosedDoubleRange(it.start.code.toDouble(), it.endInclusive.code.toDouble())
                }

                type.isSubtypeOf(RealRangeType) -> value.asCharRangeType()

                else -> throw IllegalArgumentException(
                    "toRealRange function cannot be applied to ${type.name}",
                )
            },
        )
    },
)

private val toCharRangeFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "toCharRange",
    accessType = FunctionOverload.AccessType.MEMBER_ONLY,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    returnType = CharRangeType,
    execute = { args ->
        val value = args[0].value
        val type = value.toType()
        return@create VariableType.Value(
            when {
                type.isSubtypeOf(IntRangeType) -> value.asIntRangeType().let {
                    CharRange(Char(it.start.toInt()), Char(it.endInclusive.toInt()))
                }

                type.isSubtypeOf(CharRangeType) -> value.asCharRangeType()
                type.isSubtypeOf(RealRangeType) -> value.asRealRangeType().let {
                    CharRange(Char(it.start.toInt()), Char(it.endInclusive.toInt()))
                }

                else -> throw IllegalArgumentException(
                    "toCharRange function cannot be applied to ${type.name}",
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
    toMapFunction,
    toIntRangeFunction,
    toRealRangeFunction,
    toCharRangeFunction,
)
