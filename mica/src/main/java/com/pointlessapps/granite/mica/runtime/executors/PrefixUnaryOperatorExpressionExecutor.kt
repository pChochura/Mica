package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.UnaryExpression
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.runtime.errors.RuntimeTypeException

internal object PrefixUnaryOperatorExpressionExecutor {

    fun execute(
        expression: UnaryExpression,
        typeResolver: TypeResolver,
        onAnyExpressionCallback: (Expression) -> Any,
    ): Any {
        val value = onAnyExpressionCallback(expression.rhs)

        return when (expression.operatorToken.type) {
            Token.Operator.Type.Not -> !(value as Boolean)
            Token.Operator.Type.Subtract -> -(value as Float)
            Token.Operator.Type.Add -> value as Float
            else -> throw RuntimeTypeException(
                "Operator ${expression.operatorToken.type.valueLiteral()} is not applicable to ${
                    typeResolver.resolveExpressionType(expression).name
                }",
            )
        }
    }
}
