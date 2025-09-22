package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.mapper.asType
import com.pointlessapps.granite.mica.model.CustomType
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.runtime.model.VariableType

internal object CreateCustomObjectExecutor {

    fun execute(
        values: List<Any>,
        types: List<Type>,
        typeName: String,
        parentType: Type?,
        propertyNames: List<String>,
    ) = VariableType.Value(
        propertyNames
            .zip(types.zip(values).map { (type, value) -> value.asType(type) })
            .associate { (name, value) -> name to value }
            .toMutableMap()
            .apply {
                this[CustomType.PROPERTY.NAME.value] = typeName
                this[CustomType.PROPERTY.PARENT_TYPE.value] = parentType
            },
    )
}
