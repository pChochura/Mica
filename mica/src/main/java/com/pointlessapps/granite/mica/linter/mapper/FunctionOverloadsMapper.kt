package com.pointlessapps.granite.mica.linter.mapper

import com.pointlessapps.granite.mica.model.Type

internal fun Map<Pair<String, Int>, MutableMap<List<Type>, (List<Type>) -> Type>>.toFunctionSignatures(): Set<String> =
    flatMap { (k, v) ->
        v.keys.map { "${k.first}(${it.joinToString(transform = Type::name)})" }
    }.toSet()
