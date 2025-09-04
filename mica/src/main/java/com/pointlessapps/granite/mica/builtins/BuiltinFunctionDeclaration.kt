package com.pointlessapps.granite.mica.builtins

import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.runtime.model.VariableType

/**
 * Represents a builtin function.
 */
internal class BuiltinFunctionDeclaration(
    val name: String,
    val parameters: List<FunctionOverload.Parameter>,
    val getReturnType: (argumentTypes: List<Type>) -> Type,
    val execute: (arguments: List<VariableType.Value>) -> VariableType.Value,
)
