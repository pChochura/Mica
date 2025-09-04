package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.mapper.asType
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.runtime.model.VariableType

internal object CreateCustomObjectExecutor {

    fun execute(
        values: List<Any>,
        types: List<Type>,
        propertyNames: List<String>,
    ) = VariableType.Value(
        propertyNames
            .zip(types.zip(values).map { (type, value) -> value.asType(type) })
            .associate { (name, value) -> name to value }
            .toMutableMap(),
    )
}
