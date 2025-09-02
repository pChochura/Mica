package com.pointlessapps.granite.mica.builtins

import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Companion.of
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Resolver
import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.CharRangeType
import com.pointlessapps.granite.mica.model.CharType
import com.pointlessapps.granite.mica.model.ClosedDoubleRange
import com.pointlessapps.granite.mica.model.CustomType
import com.pointlessapps.granite.mica.model.IntRangeType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.model.RealRangeType
import com.pointlessapps.granite.mica.model.RealType
import com.pointlessapps.granite.mica.model.SetType
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.model.UndefinedType
import com.pointlessapps.granite.mica.runtime.helper.CustomObject
import com.pointlessapps.granite.mica.runtime.model.Variable
import com.pointlessapps.granite.mica.runtime.model.Variable.Companion.toVariable

@Suppress("UNCHECKED_CAST")
private val copyBuiltinFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "copy",
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    getReturnType = { it[0] },
    execute = { args ->
        val value = args[0].value
        val newValue = when (args[0].type) {
            AnyType, BoolType, CharType, IntType, RealType, StringType,
            CharRangeType, IntRangeType, RealRangeType,
                -> value

            is SetType -> (value as MutableSet<*>).toMutableSet()
            is CustomType -> (value as CustomObject).toMutableMap()
            is ArrayType -> (value as MutableList<*>).toMutableList()
            UndefinedType -> null
        }

        args[0].type.toVariable(newValue)
    },
)

@Suppress("UNCHECKED_CAST")
private val deepCopyBuiltinFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "deepCopy",
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    getReturnType = { it[0] },
    execute = { args ->
        fun copy(variable: Variable<*>): Variable<*> = variable.type.toVariable(
            when (variable.value) {
                is Boolean, is Char, is Long, is Double, is String,
                is CharRange, is LongRange, is ClosedDoubleRange,
                    -> variable.value

                is Set<*> -> variable.value.map { copy(it as Variable<*>) }.toMutableSet()
                is Map<*, *> -> variable.value.mapValues { copy(it as Variable<*>) }.toMutableMap()
                is List<*> -> variable.value.map { copy(it as Variable<*>) }.toMutableList()
                else -> throw IllegalArgumentException("${variable.type.name} cannot be copied")
            },
        )

        copy(args[0])
    },
)

internal val anyBuiltinFunctions = listOf(
    copyBuiltinFunction,
    deepCopyBuiltinFunction,
)
