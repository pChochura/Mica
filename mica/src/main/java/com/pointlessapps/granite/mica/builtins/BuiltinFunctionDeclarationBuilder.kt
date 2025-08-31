package com.pointlessapps.granite.mica.builtins

import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Resolver.*
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.CustomType
import com.pointlessapps.granite.mica.model.SetType
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.runtime.model.Variable

internal object BuiltinFunctionDeclarationBuilder {

    fun create(
        name: String,
        parameters: List<FunctionOverload.Parameter>,
        returnType: Type,
        execute: (List<Variable<*>>) -> Variable<*>,
    ) = create(
        name = name,
        parameters = parameters,
        getReturnType = { returnType },
        execute = execute,
    )

    fun create(
        name: String,
        parameters: List<FunctionOverload.Parameter>,
        getReturnType: (List<Type>) -> Type,
        execute: (List<Variable<*>>) -> Variable<*>,
    ) = BuiltinFunctionDeclaration(
        name = name,
        parameters = parameters,
        getReturnType = getReturnType,
        execute = { args ->
            if (args.size != parameters.size) {
                throw IllegalArgumentException(
                    "$name function expects ${parameters.size} argument, got ${args.size}",
                )
            }

            args.zip(parameters).forEachIndexed { index, (arg, param) ->
                val matches = when (param.resolver) {
                    EXACT_MATCH -> arg.type == param.type
                    SHALLOW_MATCH -> when (arg.type) {
                        is CustomType -> param.type is CustomType
                        is ArrayType -> param.type is ArrayType
                        is SetType -> param.type is SetType
                        else -> arg.type.isSubtypeOf(param.type)
                    }
                    SUBTYPE_MATCH -> arg.type.isSubtypeOf(param.type)
                }
                if (!matches) {
                    throw IllegalArgumentException(
                        "$name function expects a ${param.type.name} as `${
                            index
                        }` argument, got ${arg.type.name}",
                    )
                }
            }

            execute(args)
        },
    )
}
