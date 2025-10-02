package com.pointlessapps.granite.mica.builtins.functions

import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.model.Type

// TODO add a optional parameter construct
internal object BuiltinFunctionDeclarationBuilder {

    fun create(
        name: String,
        accessType: FunctionOverload.AccessType,
        typeParameterConstraint: Type?,
        parameters: List<FunctionOverload.Parameter>,
        returnType: Type,
        execute: (Type?, List<Any>) -> Any?,
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
        execute: (Type?, List<Any>) -> Any?,
    ) = BuiltinFunctionDeclaration(
        name = name,
        accessType = accessType,
        typeParameterConstraint = typeParameterConstraint,
        parameters = parameters,
        getReturnType = getReturnType,
        execute = execute,
    )
}
