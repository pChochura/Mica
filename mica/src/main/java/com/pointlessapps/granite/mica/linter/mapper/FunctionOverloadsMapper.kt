package com.pointlessapps.granite.mica.linter.mapper

import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.linter.model.FunctionOverloads
import com.pointlessapps.granite.mica.model.Type

internal fun FunctionOverloads.toFunctionSignatures(): Set<String> =
    flatMap { (name, parametersMap) ->
        parametersMap.keys.map {
            getSignature(
                name = name,
                parameters = it.map(FunctionOverload.Parameter::type),
                isVararg = it.lastOrNull()?.vararg == true,
            )
        }
    }.toSet()

internal fun getSignature(name: String, parameters: List<Type>, isVararg: Boolean = false): String {
    return "$name(${
        parameters.mapIndexed { index, type ->
            "${if (isVararg && index == parameters.lastIndex) ".." else ""}${type.name}"
        }.joinToString(", ")
    })"
}
