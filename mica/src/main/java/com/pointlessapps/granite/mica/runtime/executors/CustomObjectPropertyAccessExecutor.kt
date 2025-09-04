package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.mapper.asCustomType
import com.pointlessapps.granite.mica.runtime.model.VariableType

internal object CustomObjectPropertyAccessExecutor {

    fun execute(
        value: Any,
        propertyName: String,
    ) = VariableType.Value(
        requireNotNull(
            value = value.asCustomType()[propertyName],
            lazyMessage = { "Custom object property $propertyName not found." },
        ),
    )
}
