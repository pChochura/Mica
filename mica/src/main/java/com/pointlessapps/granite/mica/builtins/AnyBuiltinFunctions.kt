package com.pointlessapps.granite.mica.builtins

import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Companion.of
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Resolver
import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.runtime.model.VariableType

private val copyFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "copy",
    accessType = FunctionOverload.AccessType.GLOBAL_AND_MEMBER,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    getReturnType = { it[0] },
    execute = { args ->
        val newValue = when (val value = args[0].value) {
            is Set<*> -> value.toMutableSet()
            is Map<*, *> -> value.toMutableMap()
            is List<*> -> value.toMutableList()
            else -> value
        }

        VariableType.Value(newValue)
    },
)

private val deepCopyFunction = BuiltinFunctionDeclarationBuilder.create(
    name = "deepCopy",
    accessType = FunctionOverload.AccessType.GLOBAL_AND_MEMBER,
    parameters = listOf(Resolver.SUBTYPE_MATCH.of(AnyType)),
    getReturnType = { it[0] },
    execute = { args ->
        fun copy(value: Any?): Any? = when (value) {
            is Set<*> -> value.map { copy(it as Any) }.toMutableSet()
            is Map<*, *> -> value.mapValues { copy(it as Any) }.toMutableMap()
            is List<*> -> value.map { copy(it as Any) }.toMutableList()
            else -> value
        }

        VariableType.Value(copy(args[0].value))
    },
)

internal val anyBuiltinFunctions = listOf(
    copyFunction,
    deepCopyFunction,
)
