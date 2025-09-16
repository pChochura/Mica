package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.mapper.asBoolType
import com.pointlessapps.granite.mica.mapper.asIntType
import com.pointlessapps.granite.mica.mapper.asRealType
import com.pointlessapps.granite.mica.mapper.toType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.model.RealType
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.runtime.errors.RuntimeTypeException
import com.pointlessapps.granite.mica.runtime.model.VariableType

internal object PrefixUnaryOperatorExpressionExecutor {

    fun execute(
        value: Any,
        operator: Token.Operator.Type,
    ): VariableType.Value {
        val type = value.toType()
        return VariableType.Value(
            when (operator) {
                Token.Operator.Type.Not -> when {
                    type.isSubtypeOf(BoolType) -> !value.asBoolType()
                    else -> null
                }

                Token.Operator.Type.Add -> when {
                    type.isSubtypeOf(IntType) -> value.asIntType()
                    type.isSubtypeOf(RealType) -> value.asRealType()
                    else -> null
                }

                Token.Operator.Type.Subtract -> when {
                    type.isSubtypeOf(IntType) -> -value.asIntType()
                    type.isSubtypeOf(RealType) -> -value.asRealType()
                    else -> null
                }

                else -> null
            } ?: throw RuntimeTypeException(
                "Operator ${operator.literal} is not applicable to $type",
            ),
        )
    }
}
