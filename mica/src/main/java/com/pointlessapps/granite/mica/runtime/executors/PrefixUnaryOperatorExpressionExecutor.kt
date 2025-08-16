package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.UnaryExpression
import com.pointlessapps.granite.mica.linter.resolver.TypeCoercionResolver.canBeCoercedTo
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.NumberType
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.runtime.errors.RuntimeTypeException
import com.pointlessapps.granite.mica.runtime.model.Variable
import com.pointlessapps.granite.mica.runtime.model.Variable.Companion.toVariable
import com.pointlessapps.granite.mica.runtime.resolver.ValueCoercionResolver.coerceToType

internal object PrefixUnaryOperatorExpressionExecutor {

    suspend fun execute(
        expression: UnaryExpression,
        typeResolver: TypeResolver,
        onAnyExpressionCallback: suspend (Expression) -> Variable<*>,
    ): Variable<*> {
        val value = onAnyExpressionCallback(expression.rhs).value
        val type = typeResolver.resolveExpressionType(expression.rhs)

        return when (expression.operatorToken.type) {
            Token.Operator.Type.Not -> BoolType.toVariable(!(value as Boolean))
            Token.Operator.Type.Subtract -> if (type.canBeCoercedTo(NumberType)) {
                NumberType.toVariable(-(value?.coerceToType(type, NumberType) as Double))
            } else {
                ArrayType(NumberType).toVariable(
                    (value?.coerceToType(type, ArrayType(NumberType)) as List<*>).map {
                        -(it as Double)
                    },
                )
            }

            Token.Operator.Type.Add -> if (type.canBeCoercedTo(NumberType)) {
                NumberType.toVariable(value?.coerceToType(type, NumberType) as Double)
            } else {
                ArrayType(NumberType).toVariable(
                    (value?.coerceToType(type, ArrayType(NumberType)) as List<*>).map {
                        it as Double
                    },
                )
            }

            else -> throw RuntimeTypeException(
                "Operator ${expression.operatorToken.type.literal} is not applicable to ${
                    typeResolver.resolveExpressionType(expression).name
                }",
            )
        }
    }
}
