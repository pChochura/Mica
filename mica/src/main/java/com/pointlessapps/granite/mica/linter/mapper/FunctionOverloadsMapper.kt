package com.pointlessapps.granite.mica.linter.mapper

import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.AccessType.GLOBAL_ONLY
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.AccessType.MEMBER_ONLY
import com.pointlessapps.granite.mica.linter.model.FunctionOverloads
import com.pointlessapps.granite.mica.model.Type

internal fun FunctionOverloads.toFunctionSignatures(): Set<String> =
    flatMap { (name, parametersMap) ->
        parametersMap.map { (key, value) ->
            val parameters = key.map {
                it.type to (it.resolver == FunctionOverload.Parameter.Resolver.EXACT_MATCH)
            }
            val isVararg = key.lastOrNull()?.vararg == true
            getSignature(name, parameters, value.accessType, isVararg)
        }
    }.toSet()

internal fun getSignature(
    name: String,
    parameters: List<Pair<Type, Boolean>>,
    accessType: FunctionOverload.AccessType,
    isVararg: Boolean,
) = if (accessType == MEMBER_ONLY) {
    buildString {
        parameters.firstOrNull()?.let { (type, _) -> append("$type.") }
        append(name)
        append("(")
        append(parameters.drop(1).getParametersSignature(isVararg))
        append(")")
    }
} else {
    buildString {
        append(name)
        if (accessType == GLOBAL_ONLY) append("!")
        append("(")
        append(parameters.getParametersSignature(isVararg))
        append(")")
    }
}

private fun List<Pair<Type, Boolean>>.getParametersSignature(isVararg: Boolean) =
    mapIndexed { index, (type, exactMatch) ->
        "${if (isVararg && index == lastIndex) ".." else ""}$type${if (exactMatch) "!" else ""}"
    }.joinToString(", ")
