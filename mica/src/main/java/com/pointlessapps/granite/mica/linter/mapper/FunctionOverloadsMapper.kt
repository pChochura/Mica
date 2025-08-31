package com.pointlessapps.granite.mica.linter.mapper

import com.pointlessapps.granite.mica.linter.model.FunctionOverloads

internal fun FunctionOverloads.toFunctionSignatures(): Set<String> =
    flatMap { (k, v) ->
        v.keys.map { "${k.first}(${it.joinToString { param -> param.type.name }})" }
    }.toSet()
