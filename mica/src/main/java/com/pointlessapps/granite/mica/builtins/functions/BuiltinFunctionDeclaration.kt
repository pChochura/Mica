package com.pointlessapps.granite.mica.builtins.functions

import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.runtime.model.VariableType

/**
 * Represents a builtin function.
 */
internal class BuiltinFunctionDeclaration(
    val name: String,
    val accessType: FunctionOverload.AccessType,
    val parameters: List<FunctionOverload.Parameter>,
    val getReturnType: (Type?, List<Type>) -> Type,
    val execute: (VariableType.Type?, List<VariableType.Value>) -> VariableType.Value,
)
