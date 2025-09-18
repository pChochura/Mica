package com.pointlessapps.granite.mica.helper

import com.pointlessapps.granite.mica.linter.mapper.getSignature
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

internal class Cache(
    var functionsCount: Int = 0,
    val map: MutableMap<String, Any?> = mutableMapOf(),
)

private val cache = Cache()

internal inline fun <reified T> Map<String, MutableMap<List<FunctionOverload.Parameter>, T>>.getMatchingFunctionDeclaration(
    name: String,
    arguments: List<Type>,
): T? {
    val signature = getSignature(
        name = name,
        parameters = arguments.map { it to false },
        accessType = FunctionOverload.AccessType.GLOBAL_AND_MEMBER,
        isVararg = false,
    )
    val functionsCount = map { it.value.size }.count()
    if (cache.functionsCount == functionsCount) {
        cache.map[signature]?.let {
            if (it is T) return it
        }
    }

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

    if (candidates.size <= 1) {
        cache.functionsCount = functionsCount
        val value = candidates.singleOrNull()?.value
        cache.map[signature] = value as Any?

        return value
    }

    var bestCandidate = candidates.first().value
    var bestMatchScore = Int.MAX_VALUE

    candidates.forEach { candidate ->
        var matchScore = 0
        candidate.key.zip(arguments).forEachIndexed { index, (parameter, argument) ->
            var argumentToMatch = argument
            if (parameter.vararg) {
                argumentToMatch = ArrayType(
                    arguments.subList(
                        fromIndex = index,
                        toIndex = arguments.size,
                    ).commonSupertype(),
                )
            }

            val genericType = parameter.type.replaceTypeParameter(AnyType)
            matchScore += argumentToMatch.superTypes.indexOf(genericType)
        }

        if (matchScore < bestMatchScore) {
            bestCandidate = candidate.value
            bestMatchScore = matchScore
        }
    }

    cache.functionsCount = functionsCount
    cache.map[signature] = bestCandidate as Any
    return bestCandidate
}

internal fun FunctionOverload.Parameter.matchesType(argument: Type): Boolean {
    val isGeneric = type.isTypeParameter()
    val genericType = type.replaceTypeParameter(AnyType)

    return when (resolver) {
        EXACT_MATCH -> if (isGeneric) argument.isSubtypeOf(genericType) else argument == type
        SHALLOW_MATCH -> when (argument) {
            is CustomType -> type is CustomType
            is ArrayType -> type is ArrayType
            is SetType -> type is SetType
            is MapType -> type is MapType
            else -> argument.isSubtypeOf(genericType)
        }

        SUBTYPE_MATCH -> argument.isSubtypeOf(genericType)
    }
}
