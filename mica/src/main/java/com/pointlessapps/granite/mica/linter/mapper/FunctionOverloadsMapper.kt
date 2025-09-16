package com.pointlessapps.granite.mica.linter.mapper

import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.AccessType.GLOBAL_AND_MEMBER
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.AccessType.GLOBAL_ONLY
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.AccessType.MEMBER_ONLY
import com.pointlessapps.granite.mica.linter.model.FunctionOverloads
import com.pointlessapps.granite.mica.model.Type

internal fun FunctionOverloads.toFunctionSignatures(): Set<String> =
    flatMap { (name, parametersMap) ->
        parametersMap.flatMap { (key, value) ->
            val parameters = key.map(FunctionOverload.Parameter::type)
            val isVararg = key.lastOrNull()?.vararg == true
            when (value.accessType) {
                MEMBER_ONLY -> listOf(getSignature(name, parameters, true, isVararg))
                GLOBAL_ONLY -> listOf(getSignature(name, parameters, false, isVararg))
                GLOBAL_AND_MEMBER -> listOf(
                    getSignature(name, parameters, false, isVararg),
                    getSignature(name, parameters, true, isVararg),
                )
            }
        }
    }.toSet()

internal fun getSignature(
    name: String,
    parameters: List<Type>,
    isMember: Boolean,
    isVararg: Boolean,
) = if (isMember) {
    buildString {
        parameters.firstOrNull()?.let {
            append("$it.")
        }
        append(name)
        append("(")
        append(
            parameters.drop(1).mapIndexed { index, type ->
                "${if (isVararg && index == parameters.lastIndex) ".." else ""}$type"
            }.joinToString(", "),
        )
        append(")")
    }
} else {
    buildString {
        append(name)
        append("(")
        append(
            parameters.mapIndexed { index, type ->
                "${if (isVararg && index == parameters.lastIndex) ".." else ""}$type"
            }.joinToString(", "),
        )
        append(")")
    }
}
