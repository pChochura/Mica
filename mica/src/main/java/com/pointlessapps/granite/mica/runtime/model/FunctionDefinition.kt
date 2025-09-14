package com.pointlessapps.granite.mica.runtime.model

import com.pointlessapps.granite.mica.builtins.functions.BuiltinFunctionDeclaration

internal sealed interface FunctionDefinition {
    data class Function(
        val isVararg: Boolean,
        val hasTypeParameterConstraint: Boolean,
        val parametersCount: Int,
        val index: Int,
    ) : FunctionDefinition

    data class BuiltinFunction(val declaration: BuiltinFunctionDeclaration) : FunctionDefinition
}
