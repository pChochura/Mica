package com.pointlessapps.granite.mica.runtime

import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.runtime.model.Variable
import com.pointlessapps.granite.mica.runtime.model.Variable.Companion.toVariable
import com.pointlessapps.granite.mica.runtime.resolver.ValueCoercionResolver.coerceToType

internal class State(
    val variables: MutableMap<String, Variable<*>>,
) {
    fun assignValue(name: String, value: Any, originalType: Type) {
        val type = requireNotNull(variables[name]).type
        variables[name] = type.toVariable(value.coerceToType(originalType, type))
    }

    fun declareVariable(name: String, value: Any, originalType: Type, variableType: Type) {
        variables[name] = variableType.toVariable(value.coerceToType(originalType, variableType))
    }
}
