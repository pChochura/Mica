package com.pointlessapps.granite.mica.builtins.functions

import com.pointlessapps.granite.mica.helper.commonSupertype
import com.pointlessapps.granite.mica.helper.matchesType
import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.mapper.toType
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.runtime.model.VariableType

// TODO add a optional parameter construct
internal object BuiltinFunctionDeclarationBuilder {

    fun create(
        name: String,
        accessType: FunctionOverload.AccessType,
        parameters: List<FunctionOverload.Parameter>,
        returnType: Type,
        execute: (VariableType.Type?, List<VariableType.Value>) -> VariableType.Value,
    ) = create(
        name = name,
        accessType = accessType,
        parameters = parameters,
        getReturnType = { _, _ -> returnType },
        execute = execute,
    )

    fun create(
        name: String,
        accessType: FunctionOverload.AccessType,
        parameters: List<FunctionOverload.Parameter>,
        getReturnType: (Type?, List<Type>) -> Type,
        execute: (VariableType.Type?, List<VariableType.Value>) -> VariableType.Value,
    ) = BuiltinFunctionDeclaration(
        name = name,
        accessType = accessType,
        parameters = parameters,
        getReturnType = getReturnType,
        execute = { typeArg, args ->
            val isVararg = parameters.lastOrNull()?.vararg == true
            if (!isVararg && args.size != parameters.size) {
                throw IllegalArgumentException(
                    "$name function expects ${parameters.size} argument, got ${args.size}",
                )
            }

            parameters.zip(args).forEachIndexed { index, (param, arg) ->
                if (param.vararg) {
                    val remainingArgumentsType = args.subList(
                        fromIndex = index,
                        toIndex = args.size,
                    ).map(VariableType.Value::toType).commonSupertype()

                    if (!param.matchesType(ArrayType(remainingArgumentsType))) {
                        throw IllegalArgumentException(
                            "$name function expects a ${param.type.name} as `${
                                index + 1
                            }` argument, got [$remainingArgumentsType]",
                        )
                    }

                    return@forEachIndexed
                }

                val argType = arg.value.toType()
                if (!param.matchesType(argType)) {
                    throw IllegalArgumentException(
                        "$name function expects a ${param.type.name} as `${
                            index + 1
                        }` argument, got [${argType.name}]",
                    )
                }
            }

            execute(typeArg, args)
        },
    )
}
