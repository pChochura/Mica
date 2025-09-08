package com.pointlessapps.granite.mica.runtime.model

import com.pointlessapps.granite.mica.builtins.functions.BuiltinFunctionDeclaration

internal sealed interface FunctionDefinition {
    data class Function(val index: Int) : FunctionDefinition
    data class BuiltinFunction(val declaration: BuiltinFunctionDeclaration) : FunctionDefinition
}
