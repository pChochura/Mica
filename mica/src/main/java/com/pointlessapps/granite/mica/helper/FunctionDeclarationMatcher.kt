package com.pointlessapps.granite.mica.helper

import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Resolver.EXACT_MATCH
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Resolver.SHALLOW_MATCH
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Resolver.SUBTYPE_MATCH
import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.CustomType
import com.pointlessapps.granite.mica.model.MapType
import com.pointlessapps.granite.mica.model.SetType
import com.pointlessapps.granite.mica.model.Type

internal fun <T> Map<String, MutableMap<List<FunctionOverload.Parameter>, T>>.getMatchingFunctionDeclaration(
    name: String,
    arguments: List<Type>,
): T? {
    val functionOverloads = this[name] ?: return null
    val candidates: MutableList<Map.Entry<List<FunctionOverload.Parameter>, T>> =
        ArrayList(functionOverloads.size)
    functionOverloads.forEach { entry ->
        var matches = if (entry.key.isEmpty()) {
            arguments.isEmpty()
        } else {
            arguments.size == entry.key.size || (entry.key.last().vararg && arguments.size >= entry.key.size - 1)
        }
        entry.key.zip(arguments).forEachIndexed { index, (parameter, argument) ->
            if (parameter.vararg) {
                val remainingArgumentsType = arguments.subList(
                    fromIndex = index,
                    toIndex = arguments.size,
                ).commonSupertype()
                if (!parameter.matchesType(ArrayType(remainingArgumentsType))) {
                    matches = false
                }

                return@forEachIndexed
            }

            if (!parameter.matchesType(argument)) {
                matches = false

                return@forEachIndexed
            }
        }

        if (matches) candidates.add(entry)
    }

    if (candidates.isEmpty()) return null
    if (candidates.size == 1) return candidates.first().value

    var bestCandidate = candidates.first().value
    var bestMatchPercentage = 0f

    candidates.forEach { candidate ->
        var exactMatches = 0
        candidate.key.zip(arguments).forEachIndexed { index, (parameter, argument) ->
            if (parameter.vararg) {
                val remainingArgumentsType = arguments.subList(
                    fromIndex = index,
                    toIndex = arguments.size,
                ).commonSupertype()
                if ((parameter.type as ArrayType).elementType == remainingArgumentsType) {
                    exactMatches++
                }

                return@forEachIndexed
            }

            if (parameter.type == argument) exactMatches++
        }

        val matchPercentage = if (candidate.key.isEmpty()) {
            1f
        } else {
            exactMatches.toFloat() / candidate.key.size
        }
        if (matchPercentage > bestMatchPercentage) {
            bestCandidate = candidate.value
            bestMatchPercentage = matchPercentage
        }
    }

    return bestCandidate
}

internal fun FunctionOverload.Parameter.matchesType(argument: Type): Boolean = when (resolver) {
    EXACT_MATCH -> argument == type.replaceTypeParameter(AnyType)
    SHALLOW_MATCH -> when (argument) {
        is CustomType -> type is CustomType
        is ArrayType -> type is ArrayType
        is SetType -> type is SetType
        is MapType -> type is MapType
        else -> argument.isSubtypeOf(type.replaceTypeParameter(AnyType))
    }

    SUBTYPE_MATCH -> argument.isSubtypeOf(type.replaceTypeParameter(AnyType))
}
