package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.model.CustomType
import com.pointlessapps.granite.mica.runtime.helper.CustomObject
import com.pointlessapps.granite.mica.runtime.model.Variable

internal object CustomObjectPropertyAccessExecutor {

    @Suppress("UNCHECKED_CAST")
    fun execute(
        variable: Variable<*>,
        propertyName: String,
    ): Variable<*> {
        val customObject = variable.type.valueAsSupertype<CustomType>(
            variable.value,
        ) as CustomObject

        return requireNotNull(customObject[propertyName])
    }
}
