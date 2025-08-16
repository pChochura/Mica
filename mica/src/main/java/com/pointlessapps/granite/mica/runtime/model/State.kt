package com.pointlessapps.granite.mica.runtime.model

import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.runtime.model.Variable.Companion.toVariable
import com.pointlessapps.granite.mica.runtime.resolver.ValueCoercionResolver.coerceToType

/**
 * Represents the state of the program.
 * It holds the values of the variables in a specific scope.
 *
 * A [Scope] is region of the program (like a function declaration or an if statement),
 * where the [State] is an instance of that scope. It is created for every recursive call of
 * a function or each block in the if statement.
 */
internal data class State(
    private val variables: MutableMap<String, Variable<*>>,
    private val parent: State?,
) {
    fun assignValue(name: String, value: Any, originalType: Type) {
        var currentState: State? = this
        while (currentState != null) {
            currentState.variables[name]?.let {
                currentState.variables[name] = it.type.toVariable(
                    value.coerceToType(originalType, it.type),
                )

                return
            }
            currentState = currentState.parent
        }
    }

    fun declareVariable(name: String, value: Any, originalType: Type, variableType: Type) {
        variables[name] = variableType.toVariable(value.coerceToType(originalType, variableType))
    }

    /**
     * Returns the variable or throws an error if it is not found.
     */
    fun getVariable(name: String): Variable<*>? {
        variables[name]?.let { return requireNotNull(it) }

        // Traverse parents and return the value if found
        var currentState = parent
        while (currentState != null) {
            currentState.variables[name]?.let { return requireNotNull(it) }
            currentState = currentState.parent
        }

        return null
    }

    companion object {
        fun from(state: State): State = State(
            variables = mutableMapOf(),
            parent = state,
        )
    }
}
