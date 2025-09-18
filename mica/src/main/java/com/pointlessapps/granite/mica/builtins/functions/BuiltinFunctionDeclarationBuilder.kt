package com.pointlessapps.granite.mica.builtins.functions

import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.runtime.model.VariableType

// TODO add a optional parameter construct
internal object BuiltinFunctionDeclarationBuilder {

    fun create(
        name: String,
        accessType: FunctionOverload.AccessType,
        typeParameterConstraint: Type?,
        parameters: List<FunctionOverload.Parameter>,
        returnType: Type,
        execute: (VariableType.Type?, List<VariableType.Value>) -> VariableType.Value,
    ) = create(
        name = name,
        accessType = accessType,
        typeParameterConstraint = typeParameterConstraint,
        parameters = parameters,
        getReturnType = { _, _ -> returnType },
        execute = execute,
    )

    fun create(
        name: String,
        accessType: FunctionOverload.AccessType,
        typeParameterConstraint: Type?,
        parameters: List<FunctionOverload.Parameter>,
        getReturnType: (Type?, List<Type>) -> Type,
        execute: (VariableType.Type?, List<VariableType.Value>) -> VariableType.Value,
    ) = BuiltinFunctionDeclaration(
        name = name,
        accessType = accessType,
        typeParameterConstraint = typeParameterConstraint,
        parameters = parameters,
        getReturnType = getReturnType,
        execute = execute,
    )
}
