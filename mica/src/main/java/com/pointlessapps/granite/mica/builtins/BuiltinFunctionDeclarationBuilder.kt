package com.pointlessapps.granite.mica.builtins

import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Resolver.*
import com.pointlessapps.granite.mica.mapper.toType
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.CustomType
import com.pointlessapps.granite.mica.model.SetType
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.runtime.model.VariableType

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
                val matches = when (param.resolver) {
                    EXACT_MATCH -> argType == param.type
                    SHALLOW_MATCH -> when (argType) {
                        is CustomType -> param.type is CustomType
                        is ArrayType -> param.type is ArrayType
                        is SetType -> param.type is SetType
                        else -> argType.isSubtypeOf(param.type)
                    }
                    SUBTYPE_MATCH -> argType.isSubtypeOf(param.type)
                }
                if (!matches) {
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
