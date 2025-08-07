package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.ast.expressions.BinaryExpression
import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.linter.resolver.TypeCoercionResolver.canBeCoercedTo
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.CharType
import com.pointlessapps.granite.mica.model.ClosedFloatRange
import com.pointlessapps.granite.mica.model.NumberType
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.runtime.errors.RuntimeTypeException
import com.pointlessapps.granite.mica.runtime.resolver.ValueCoercionResolver.coerceToType
import com.pointlessapps.granite.mica.runtime.resolver.ValueComparatorResolver.compareToAs
import kotlin.math.pow

internal object BinaryOperatorExpressionExecutor : ExpressionExecutor<BinaryExpression> {

    override fun execute(
        expression: BinaryExpression,
        typeResolver: TypeResolver,
        onAnyExpressionCallback: (Expression) -> Any,
    ): Any {
        val lhsValue by lazy { onAnyExpressionCallback(expression.lhs) }
        val rhsValue by lazy { onAnyExpressionCallback(expression.rhs) }

        val lhsType = typeResolver.resolveExpressionType(expression.lhs)
        val rhsType = typeResolver.resolveExpressionType(expression.rhs)

        return when (expression.operatorToken.type) {
            Token.Operator.Type.Equals -> compare(lhsValue, rhsValue, lhsType, rhsType)
                ?.let { it == 0 } ?: throwError(expression.operatorToken.type, lhsType, rhsType)

            Token.Operator.Type.NotEquals -> compare(lhsValue, rhsValue, lhsType, rhsType)
                ?.let { it != 0 } ?: throwError(expression.operatorToken.type, lhsType, rhsType)

            Token.Operator.Type.GraterThan -> compare(lhsValue, rhsValue, lhsType, rhsType)
                ?.let { it > 0 } ?: throwError(expression.operatorToken.type, lhsType, rhsType)

            Token.Operator.Type.LessThan -> compare(lhsValue, rhsValue, lhsType, rhsType)
                ?.let { it < 0 } ?: throwError(expression.operatorToken.type, lhsType, rhsType)

            Token.Operator.Type.GraterThanOrEquals -> compare(lhsValue, rhsValue, lhsType, rhsType)
                ?.let { it >= 0 } ?: throwError(expression.operatorToken.type, lhsType, rhsType)

            Token.Operator.Type.LessThanOrEquals -> compare(lhsValue, rhsValue, lhsType, rhsType)
                ?.let { it <= 0 } ?: throwError(expression.operatorToken.type, lhsType, rhsType)

            Token.Operator.Type.Add ->
                if (lhsType == StringType || rhsType == StringType) {
                    // String concatenation
                    val lhsStringValue = lhsValue.coerceToType(lhsType, StringType) as String
                    val rhsStringValue = rhsValue.coerceToType(rhsType, StringType) as String
                    lhsStringValue + rhsStringValue
                } else if (lhsType == CharType || rhsType == CharType) {
                    // Char concatenation
                    val lhsCharValue = lhsValue.coerceToType(lhsType, CharType) as Char
                    val rhsCharValue = rhsValue.coerceToType(rhsType, CharType) as Char
                    lhsCharValue.toString() + rhsCharValue.toString()
                } else {
                    // Number addition
                    val lhsNumberValue = lhsValue.coerceToType(lhsType, NumberType) as Float
                    val rhsNumberValue = rhsValue.coerceToType(rhsType, NumberType) as Float
                    lhsNumberValue + rhsNumberValue
                }

            Token.Operator.Type.Subtract -> {
                val lhsNumberValue = lhsValue.coerceToType(lhsType, NumberType) as Float
                val rhsNumberValue = rhsValue.coerceToType(rhsType, NumberType) as Float
                lhsNumberValue - rhsNumberValue
            }

            Token.Operator.Type.Multiply -> {
                val lhsNumberValue = lhsValue.coerceToType(lhsType, NumberType) as Float
                val rhsNumberValue = rhsValue.coerceToType(rhsType, NumberType) as Float
                lhsNumberValue.times(rhsNumberValue)
            }

            Token.Operator.Type.Divide -> {
                val lhsNumberValue = lhsValue.coerceToType(lhsType, NumberType) as Float
                val rhsNumberValue = rhsValue.coerceToType(rhsType, NumberType) as Float
                lhsNumberValue.div(rhsNumberValue)
            }

            Token.Operator.Type.Exponent -> {
                val lhsNumberValue = lhsValue.coerceToType(lhsType, NumberType) as Float
                val rhsNumberValue = rhsValue.coerceToType(rhsType, NumberType) as Float
                lhsNumberValue.pow(rhsNumberValue)
            }

            Token.Operator.Type.And -> {
                val lhsBooleanValue = lhsValue.coerceToType(lhsType, BoolType) as Boolean
                // Wrap it in a lazy block to avoid executing the rhs if the lhs is false
                val rhsBooleanValue by lazy { rhsValue.coerceToType(rhsType, BoolType) as Boolean }
                lhsBooleanValue && rhsBooleanValue
            }

            Token.Operator.Type.Or -> {
                val lhsBooleanValue = lhsValue.coerceToType(lhsType, BoolType) as Boolean
                // Wrap it in a lazy block to avoid executing the rhs if the lhs is true
                val rhsBooleanValue by lazy { rhsValue.coerceToType(rhsType, BoolType) as Boolean }
                lhsBooleanValue || rhsBooleanValue
            }

            Token.Operator.Type.Range ->
                if (lhsType == CharType || rhsType == CharType) {
                    // Char range
                    val lhsCharValue = lhsValue.coerceToType(lhsType, CharType) as Char
                    val rhsCharValue = rhsValue.coerceToType(rhsType, CharType) as Char
                    CharRange(lhsCharValue, rhsCharValue)
                } else {
                    // Number range
                    val lhsNumberValue = lhsValue.coerceToType(lhsType, NumberType) as Float
                    val rhsNumberValue = rhsValue.coerceToType(rhsType, NumberType) as Float
                    ClosedFloatRange(lhsNumberValue, rhsNumberValue)
                }

            else -> throwError(expression.operatorToken.type, lhsType, rhsType)
        }
    }

    private fun compare(lhsValue: Any, rhsValue: Any, lhsType: Type, rhsType: Type): Int? = when {
        lhsType == rhsType -> lhsValue.compareToAs(rhsValue, lhsType, rhsType)
        lhsType.canBeCoercedTo(rhsType) -> lhsValue.compareToAs(rhsValue, lhsType, rhsType)
        rhsType.canBeCoercedTo(lhsType) -> rhsValue.compareToAs(lhsValue, rhsType, lhsType)
        else -> null
    }

    private fun throwError(operatorType: Token.Operator.Type, lhsType: Type, rhsType: Type) {
        throw RuntimeTypeException(
            "${operatorType.valueLiteral()} is not applicable to ${lhsType.name} and ${rhsType.name}",
        )
    }
}
