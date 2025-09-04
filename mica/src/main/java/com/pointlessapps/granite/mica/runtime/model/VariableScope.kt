package com.pointlessapps.granite.mica.runtime.model

import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.mapper.asType
import com.pointlessapps.granite.mica.mapper.toType
import com.pointlessapps.granite.mica.model.Type

/**
 * Represents the state of the program.
 * It holds the values of the variables in a specific scope.
 *
 * A [Scope] is region of the program (like a function declaration or an if statement),
 * where the [VariableScope] is an instance of that scope. It is created for every recursive call of
 * a function or each block in the if statement.
 */
internal data class VariableScope(
    private val variables: MutableMap<String, VariableType.Value>,
    private val parent: VariableScope?,
) {
    private val propertyAliases = mutableMapOf<String, PropertyAlias>()

    fun declarePropertyAlias(
        name: String,
        onVariableCallback: () -> VariableType.Value,
        onValueChangedCallback: (VariableType.Value) -> Unit,
    ) {
        propertyAliases.put(
            key = name,
            value = PropertyAlias(
                onVariableCallback = onVariableCallback,
                onValueChangedCallback = onValueChangedCallback,
            ),
        )
    }

    fun assignValue(name: String, value: Any) {
        var currentState: VariableScope? = this
        while (currentState != null) {
            currentState.variables[name]?.let {
                currentState.variables[name] = VariableType.Value(value.asType(it.toType()))

                return
            }
            currentState = currentState.parent
        }

        if (propertyAliases.containsKey(name)) {
            requireNotNull(
                value = propertyAliases[name],
                lazyMessage = { "Property alias $name not found" },
            ).let {
                val type = it.onVariableCallback().toType()
                it.onValueChangedCallback(VariableType.Value(value.asType(type)))
            }
        }
    }

    fun declare(name: String, value: Any, variableType: Type) {
        variables[name] = VariableType.Value(value.asType(variableType))
    }

    fun get(name: String): VariableType.Value? {
        var currentState: VariableScope? = this
        while (currentState != null) {
            currentState.variables[name]?.let { return it }
            currentState = currentState.parent
        }

        if (propertyAliases.containsKey(name)) {
            return requireNotNull(
                value = propertyAliases[name],
                lazyMessage = { "Property alias $name not found" },
            ).onVariableCallback()
        }

        return null
    }

    companion object {
        fun from(state: VariableScope): VariableScope = VariableScope(
            variables = mutableMapOf(),
            parent = state,
        )
    }
}
