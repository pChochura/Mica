package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.model.RealType
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.runtime.errors.RuntimeTypeException
import com.pointlessapps.granite.mica.runtime.model.Variable
import com.pointlessapps.granite.mica.runtime.model.Variable.Companion.toVariable

internal object PrefixUnaryOperatorExpressionExecutor {

    fun execute(
        value: Variable<*>,
        operator: Token.Operator.Type,
    ): Variable<*> {

        return when (operator) {
            Token.Operator.Type.Not -> BoolType.toVariable(!(value.value as Boolean))
            Token.Operator.Type.Add -> value
            Token.Operator.Type.Subtract -> when (value.type) {
                IntType -> IntType.toVariable(-(value.value as Long))
                RealType -> RealType.toVariable(-(value.value as Double))
                else -> throw RuntimeTypeException(
                    "Operator ${operator.literal} is not applicable to ${value.type.name}",
                )
            }

            else -> throw RuntimeTypeException(
                "Operator ${operator.literal} is not applicable to ${value.type.name}",
            )
        }
    }
}
