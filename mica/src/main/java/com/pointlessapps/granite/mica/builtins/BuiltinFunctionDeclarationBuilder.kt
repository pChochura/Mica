package com.pointlessapps.granite.mica.builtins

import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.runtime.model.Variable

internal object BuiltinFunctionDeclarationBuilder {

    fun create(
        name: String,
        parameters: List<Pair<String, Type>>,
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
        parameters: List<Pair<String, Type>>,
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

            args.zip(parameters).forEach { (arg, param) ->
                if (!arg.type.isSubtypeOf(param.second)) {
                    throw IllegalArgumentException(
                        "$name function expects a ${param.second} as `${
                            param.first
                        }` argument, got ${arg.type.name}",
                    )
                }
            }

            execute(args)
        },
    )
}
