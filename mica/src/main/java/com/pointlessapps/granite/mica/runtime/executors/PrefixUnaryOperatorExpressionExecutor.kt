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
    ): Variable<*> = when (operator) {
        Token.Operator.Type.Not -> when {
            value.type.isSubtypeOf(BoolType) -> value.type.toVariable(
                !(value.type.valueAsSupertype<BoolType>(value.value) as Boolean),
            )

            else -> null
        }

        Token.Operator.Type.Add -> when {
            value.type.isSubtypeOf(IntType) -> value.type.toVariable(
                value.type.valueAsSupertype<IntType>(value.value) as Long,
            )

            value.type.isSubtypeOf(RealType) -> value.type.toVariable(
                value.type.valueAsSupertype<RealType>(value.value) as Double,
            )

            else -> null
        }

        Token.Operator.Type.Subtract -> when {
            value.type.isSubtypeOf(IntType) -> value.type.toVariable(
                -(value.type.valueAsSupertype<IntType>(value.value) as Long),
            )

            value.type.isSubtypeOf(RealType) -> value.type.toVariable(
                -(value.type.valueAsSupertype<RealType>(value.value) as Double),
            )

            else -> null
        }

        else -> null
    } ?: throw RuntimeTypeException(
        "Operator ${operator.literal} is not applicable to ${value.type.name}",
    )
}
