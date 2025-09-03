package com.pointlessapps.granite.mica.runtime.model

import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.runtime.model.Variable.Companion.toVariable

/**
 * Represents the state of the program.
 * It holds the values of the variables in a specific scope.
 *
 * A [Scope] is region of the program (like a function declaration or an if statement),
 * where the [VariableScope] is an instance of that scope. It is created for every recursive call of
 * a function or each block in the if statement.
 */
internal data class VariableScope(
    private val variables: MutableMap<String, Variable<*>>,
    private val parent: VariableScope?,
) {
    private val propertyAliases = mutableMapOf<String, PropertyAlias>()

    fun declarePropertyAlias(
        name: String,
        variable: Variable<*>,
        onValueChangedCallback: (Variable<*>) -> Unit,
    ) {
        propertyAliases.put(
            key = name,
            value = PropertyAlias(
                value = variable,
                onValueChangedCallback = onValueChangedCallback,
            ),
        )
    }

    fun assignValue(name: String, value: Any, valueType: Type) {
        if (propertyAliases.containsKey(name)) {
            requireNotNull(
                value = propertyAliases[name],
                lazyMessage = { "Property alias $name not found" },
            ).let {
                it.onValueChangedCallback(
                    it.value.type.toVariable(
                        valueType.valueAsSupertype(value, it.value.type),
                    ),
                )
            }

            return
        }

        var currentState: VariableScope? = this
        while (currentState != null) {
            currentState.variables[name]?.let {
                currentState.variables[name] = it.type.toVariable(
                    valueType.valueAsSupertype(value, it.type),
                )

                return
            }
            currentState = currentState.parent
        }
    }

    fun declare(name: String, value: Any, valueType: Type, variableType: Type) {
        variables[name] = variableType.toVariable(
            valueType.valueAsSupertype(value, variableType),
        )
    }

    fun get(name: String): Variable<*>? {
        if (propertyAliases.containsKey(name)) {
            return requireNotNull(
                value = propertyAliases[name],
                lazyMessage = { "Property alias $name not found" },
            ).value
        }

        var currentState: VariableScope? = this
        while (currentState != null) {
            currentState.variables[name]?.let {
                return requireNotNull(
                    value = it,
                    lazyMessage = { "Variable $name not found" },
                )
            }
            currentState = currentState.parent
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
