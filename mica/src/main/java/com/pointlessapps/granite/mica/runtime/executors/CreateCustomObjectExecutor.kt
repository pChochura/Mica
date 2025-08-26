package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.model.CustomType
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.runtime.model.CustomVariable
import com.pointlessapps.granite.mica.runtime.model.Variable
import com.pointlessapps.granite.mica.runtime.model.Variable.Companion.toVariable

internal object CreateCustomObjectExecutor {

    fun execute(
        values: List<Any>,
        types: List<Type>,
        propertyNames: List<String>,
        typeName: String,
    ): Variable<*> {
        val variables = types.zip(values).map { (type, value) -> type.toVariable(value) }
        return CustomVariable(
            value = propertyNames.zip(variables).associate { (name, value) -> name to value },
            type = CustomType(typeName),
        )
    }
}
