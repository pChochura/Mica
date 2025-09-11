package com.pointlessapps.granite.mica.linter.mapper

import com.pointlessapps.granite.mica.linter.model.FunctionOverloads

internal fun FunctionOverloads.toFunctionSignatures(): Set<String> =
    flatMap { (name, parametersMap) ->
        parametersMap.keys.map {
            "$name(${
                it.joinToString { param ->
                    "${if (param.vararg) ".." else ""}${param.type.name}"
                }
            })"
        }
    }.toSet()
