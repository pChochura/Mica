package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.linter.resolver.TypeCoercionResolver.canBeCoercedTo
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.NumberType
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.runtime.errors.RuntimeTypeException
import com.pointlessapps.granite.mica.runtime.model.Variable
import com.pointlessapps.granite.mica.runtime.model.Variable.Companion.toVariable
import com.pointlessapps.granite.mica.runtime.resolver.ValueCoercionResolver.coerceToType

internal object PrefixUnaryOperatorExpressionExecutor {

    fun execute(
        value: Variable<*>,
        operator: Token.Operator.Type,
    ): Variable<*> {

        return when (operator) {
            Token.Operator.Type.Not -> BoolType.toVariable(!(value.value as Boolean))
            Token.Operator.Type.Subtract -> if (value.type.canBeCoercedTo(NumberType)) {
                NumberType.toVariable(
                    -(value.value?.coerceToType(value.type, NumberType) as Double),
                )
            } else {
                ArrayType(NumberType).toVariable(
                    (value.value?.coerceToType(value.type, ArrayType(NumberType)) as List<*>)
                        .map { -(it as Double) },
                )
            }

            Token.Operator.Type.Add -> if (value.type.canBeCoercedTo(NumberType)) {
                NumberType.toVariable(value.value?.coerceToType(value.type, NumberType) as Double)
            } else {
                ArrayType(NumberType).toVariable(
                    (value.value?.coerceToType(value.type, ArrayType(NumberType)) as List<*>)
                        .map { it as Double },
                )
            }

            else -> throw RuntimeTypeException(
                "Operator ${operator.literal} is not applicable to ${value.type.name}",
            )
        }
    }
}
