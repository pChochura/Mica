package com.pointlessapps.granite.mica.builtins.functions

import com.pointlessapps.granite.mica.helper.matchesType
import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.mapper.toType
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.runtime.model.VariableType

// TODO add a optional parameter construct
internal object BuiltinFunctionDeclarationBuilder {

    fun create(
        name: String,
        accessType: FunctionOverload.AccessType,
        parameters: List<FunctionOverload.Parameter>,
        returnType: Type,
        execute: (List<VariableType.Value>) -> VariableType.Value,
    ) = create(
        name = name,
        accessType = accessType,
        parameters = parameters,
        getReturnType = { returnType },
        execute = execute,
    )

    fun create(
        name: String,
        accessType: FunctionOverload.AccessType,
        parameters: List<FunctionOverload.Parameter>,
        getReturnType: (List<Type>) -> Type,
        execute: (List<VariableType.Value>) -> VariableType.Value,
    ) = BuiltinFunctionDeclaration(
        name = name,
        accessType = accessType,
        parameters = parameters,
        getReturnType = getReturnType,
        execute = { args ->
            if (args.size != parameters.size) {
                throw IllegalArgumentException(
                    "$name function expects ${parameters.size} argument, got ${args.size}",
                )
            }

            args.zip(parameters).forEachIndexed { index, (arg, param) ->
                val argType = arg.value.toType()
                if (!param.matchesType(argType)) {
                    throw IllegalArgumentException(
                        "$name function expects a ${param.type.name} as `${
                            index
                        }` argument, got ${argType.name}",
                    )
                }
            }

            execute(args)
        },
    )
}
