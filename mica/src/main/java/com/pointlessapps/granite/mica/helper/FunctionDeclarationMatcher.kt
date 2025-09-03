package com.pointlessapps.granite.mica.helper

import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Resolver.EXACT_MATCH
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Resolver.SHALLOW_MATCH
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Resolver.SUBTYPE_MATCH
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.CustomType
import com.pointlessapps.granite.mica.model.SetType
import com.pointlessapps.granite.mica.model.Type

internal fun <T> Map<Pair<String, Int>, MutableMap<List<FunctionOverload.Parameter>, T>>.getMatchingFunctionDeclaration(
    name: String,
    arguments: List<Type>,
): T? {
    val functionOverloads = this[name to arguments.size] ?: return null
    val candidates: MutableList<Map.Entry<List<FunctionOverload.Parameter>, T>> =
        ArrayList(functionOverloads.size)
    functionOverloads.forEach { entry ->
        val matches = entry.key.zip(arguments).all { (parameter, argument) ->
            when (parameter.resolver) {
                EXACT_MATCH -> argument == parameter.type
                SHALLOW_MATCH -> when (argument) {
                    is CustomType -> parameter.type is CustomType
                    is ArrayType -> parameter.type is ArrayType
                    is SetType -> parameter.type is SetType
                    else -> argument.isSubtypeOf(parameter.type)
                }

                SUBTYPE_MATCH -> argument.isSubtypeOf(parameter.type)
            }
        }

        if (matches) candidates.add(entry)
    }

    if (candidates.isEmpty()) return null
    if (candidates.size == 1) return candidates.first().value

    var bestCandidate = candidates.first()
    var bestMatchCount = bestCandidate.key.zip(arguments)
        .count { it.first.type.name == it.second.name }

    candidates.subList(1, candidates.size).forEach { candidate ->
        val matchCount = candidate.key.zip(arguments).count { it.first.type.name == it.second.name }
        if (matchCount > bestMatchCount) {
            bestCandidate = candidate
            bestMatchCount = matchCount
        }
    }

    return bestCandidate.value
}
