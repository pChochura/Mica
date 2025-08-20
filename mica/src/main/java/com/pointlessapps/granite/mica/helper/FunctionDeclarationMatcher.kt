package com.pointlessapps.granite.mica.helper

import com.pointlessapps.granite.mica.model.Type

internal fun <T> Map<Pair<String, Int>, MutableMap<List<Type>, T>>.getMatchingFunctionDeclaration(
    name: String,
    arguments: List<Type>,
): T? {
    val functionOverloads = this[name to arguments.size] ?: return null
    val candidates: MutableList<Map.Entry<List<Type>, T>> = ArrayList(functionOverloads.size)
    functionOverloads.forEach { entry ->
        val matches = entry.key.zip(arguments).all { (parameter, argument) ->
            argument.isSupertypeOf(parameter)
        }

        if (matches) candidates.add(entry)
    }

    if (candidates.size == 1) return candidates.first().value

    var bestCandidate = candidates.first()
    var bestMatchCount = bestCandidate.key.zip(arguments)
        .count { it.first.name == it.second.name }

    candidates.subList(1, candidates.size).forEach { candidate ->
        val matchCount = candidate.key.zip(arguments).count { it.first.name == it.second.name }
        if (matchCount > bestMatchCount) {
            bestCandidate = candidate
            bestMatchCount = matchCount
        }
    }

    return bestCandidate.value
}
