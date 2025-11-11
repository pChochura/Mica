package com.pointlessapps.granite.mica.linter.mapper

import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.AccessType.GLOBAL_AND_MEMBER
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.AccessType.GLOBAL_ONLY
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.AccessType.MEMBER_ONLY
import com.pointlessapps.granite.mica.linter.model.FunctionOverloads

internal fun FunctionOverloads.toFunctionSignatures(): Set<String> =
    flatMap { (name, parametersMap) ->
        parametersMap.map { (key, value) -> getSignature(name, key, value.accessType) }
    }.toSet()

internal fun getSignature(
    name: String,
    parameters: List<FunctionOverload.Parameter>,
    accessType: FunctionOverload.AccessType = GLOBAL_AND_MEMBER,
) = if (accessType == MEMBER_ONLY) {
    buildString {
        parameters.firstOrNull()?.let { (type, _) -> append("$type.") }
        append(name)
        append("(")
        append(parameters.drop(1).getParametersSignature())
        append(")")
    }
} else {
    buildString {
        append(name)
        if (accessType == GLOBAL_ONLY) append("!")
        append("(")
        append(parameters.getParametersSignature())
        append(")")
    }
}

private fun List<FunctionOverload.Parameter>.getParametersSignature() =
    joinToString(", ") { (type, resolver, vararg) ->
        "${if (vararg) ".." else ""}$type${
            if (resolver == FunctionOverload.Parameter.Resolver.EXACT_MATCH) "!" else ""
        }"
    }
