package com.pointlessapps.granite.mica.runtime

import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.runtime.model.Variable
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
    val variables: MutableMap<String, Variable<*>>,
) {
    fun assignValue(name: String, value: Any, originalType: Type) {
        val type = requireNotNull(variables[name]).type
        variables[name] = type.toVariable(value.coerceToType(originalType, type))
    }

    fun declareVariable(name: String, value: Any, originalType: Type, variableType: Type) {
        variables[name] = variableType.toVariable(value.coerceToType(originalType, variableType))
    }

    companion object {
        // Copy the variables as new values
        fun from(state: State): State = State(state.variables.toMutableMap())
    }
}
