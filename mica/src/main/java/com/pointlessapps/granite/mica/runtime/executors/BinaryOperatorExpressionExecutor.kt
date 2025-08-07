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

internal object BinaryOperatorExpressionExecutor {

    fun execute(
        expression: BinaryExpression,
        typeResolver: TypeResolver,
        onAnyExpressionCallback: (Expression) -> Any,
    ): Any {
        val lhsValue = lazy { onAnyExpressionCallback(expression.lhs) }
        val rhsValue = lazy { onAnyExpressionCallback(expression.rhs) }

        val lhsType = typeResolver.resolveExpressionType(expression.lhs)
        val rhsType = typeResolver.resolveExpressionType(expression.rhs)
        val operatorToken = expression.operatorToken

        return when (operatorToken.type) {
            Token.Operator.Type.Equals,
            Token.Operator.Type.NotEquals,
            Token.Operator.Type.GraterThan,
            Token.Operator.Type.LessThan,
            Token.Operator.Type.GraterThanOrEquals,
            Token.Operator.Type.LessThanOrEquals,
                -> executeComparisonOperator(
                operatorToken.type,
                lhsValue,
                rhsValue,
                lhsType,
                rhsType
            )

            Token.Operator.Type.Add -> executeAddition(lhsValue, rhsValue, lhsType, rhsType)

            Token.Operator.Type.Subtract,
            Token.Operator.Type.Multiply,
            Token.Operator.Type.Divide,
            Token.Operator.Type.Exponent,
                -> executeArithmeticOperator(
                operatorToken.type,
                lhsValue,
                rhsValue,
                lhsType,
                rhsType
            )

            Token.Operator.Type.And, Token.Operator.Type.Or -> executeLogicalOperator(
                operatorToken.type,
                lhsValue,
                rhsValue,
                lhsType,
                rhsType
            )

            Token.Operator.Type.Range -> executeRangeOperator(
                lhsValue,
                rhsValue,
                lhsType,
                rhsType
            )

            else -> throwIncompatibleTypesError(operatorToken.type, lhsType, rhsType)
        }
    }

    private fun executeComparisonOperator(
        operatorType: Token.Operator.Type,
        lhsValue: Lazy<Any>,
        rhsValue: Lazy<Any>,
        lhsType: Type,
        rhsType: Type,
    ): Boolean {
        val comparisonResult = compare(lhsValue, rhsValue, lhsType, rhsType)
            ?: throwIncompatibleTypesError(operatorType, lhsType, rhsType)

        return when (operatorType) {
            Token.Operator.Type.Equals -> comparisonResult == 0
            Token.Operator.Type.NotEquals -> comparisonResult != 0
            Token.Operator.Type.GraterThan -> comparisonResult > 0
            Token.Operator.Type.LessThan -> comparisonResult < 0
            Token.Operator.Type.GraterThanOrEquals -> comparisonResult >= 0
            Token.Operator.Type.LessThanOrEquals -> comparisonResult <= 0
            else -> throwIncompatibleTypesError(operatorType, lhsType, rhsType)
        }
    }

    private fun executeAddition(
        lhsValue: Lazy<Any>,
        rhsValue: Lazy<Any>,
        lhsType: Type,
        rhsType: Type,
    ): Any = when {
        lhsType == StringType || rhsType == StringType -> {
            // String concatenation
            val lhsString = lhsValue.value.coerceToType(lhsType, StringType) as String
            val rhsString = rhsValue.value.coerceToType(rhsType, StringType) as String
            lhsString + rhsString
        }

        lhsType == CharType || rhsType == CharType -> {
            // Char concatenation into a String
            val lhsChar = lhsValue.value.coerceToType(lhsType, CharType) as Char
            val rhsChar = rhsValue.value.coerceToType(rhsType, CharType) as Char
            lhsChar.toString() + rhsChar.toString()
        }

        else -> {
            val lhsNumber = lhsValue.value.coerceToType(lhsType, NumberType) as Float
            val rhsNumber = rhsValue.value.coerceToType(rhsType, NumberType) as Float
            lhsNumber + rhsNumber
        }
    }

    private fun executeArithmeticOperator(
        operatorType: Token.Operator.Type,
        lhsValue: Lazy<Any>,
        rhsValue: Lazy<Any>,
        lhsType: Type,
        rhsType: Type,
    ): Float {
        val lhsNumber = lhsValue.value.coerceToType(lhsType, NumberType) as Float
        val rhsNumber = rhsValue.value.coerceToType(rhsType, NumberType) as Float

        return when (operatorType) {
            Token.Operator.Type.Subtract -> lhsNumber - rhsNumber
            Token.Operator.Type.Multiply -> lhsNumber * rhsNumber
            Token.Operator.Type.Divide -> lhsNumber / rhsNumber
            Token.Operator.Type.Exponent -> lhsNumber.pow(rhsNumber)
            else -> throwIncompatibleTypesError(operatorType, lhsType, rhsType)
        }
    }

    private fun executeLogicalOperator(
        operatorType: Token.Operator.Type,
        lhsValue: Lazy<Any>,
        rhsValue: Lazy<Any>,
        lhsType: Type,
        rhsType: Type,
    ): Boolean {
        val lhsBoolean = lhsValue.value.coerceToType(lhsType, BoolType) as Boolean
        val rhsBoolean by lazy { rhsValue.value.coerceToType(rhsType, BoolType) as Boolean }

        return when (operatorType) {
            Token.Operator.Type.And -> lhsBoolean && rhsBoolean
            Token.Operator.Type.Or -> lhsBoolean || rhsBoolean
            else -> throwIncompatibleTypesError(operatorType, lhsType, rhsType)
        }
    }

    private fun executeRangeOperator(
        lhsValue: Lazy<Any>,
        rhsValue: Lazy<Any>,
        lhsType: Type,
        rhsType: Type,
    ) = if (lhsType == CharType || rhsType == CharType) {
        // Char range
        val lhsCharValue = lhsValue.value.coerceToType(lhsType, CharType) as Char
        val rhsCharValue = rhsValue.value.coerceToType(rhsType, CharType) as Char
        CharRange(lhsCharValue, rhsCharValue)
    } else {
        // Number range
        val lhsNumberValue = lhsValue.value.coerceToType(lhsType, NumberType) as Float
        val rhsNumberValue = rhsValue.value.coerceToType(rhsType, NumberType) as Float
        ClosedFloatRange(lhsNumberValue, rhsNumberValue)
    }

    private fun compare(
        lhsValue: Lazy<Any>,
        rhsValue: Lazy<Any>,
        lhsType: Type,
        rhsType: Type,
    ): Int? = when {
        lhsType == rhsType || lhsType.canBeCoercedTo(rhsType) ->
            lhsValue.value.compareToAs(rhsValue.value, lhsType, rhsType)

        rhsType.canBeCoercedTo(lhsType) ->
            rhsValue.value.compareToAs(lhsValue.value, rhsType, lhsType)

        else -> null
    }

    private fun throwIncompatibleTypesError(
        operatorType: Token.Operator.Type,
        lhsType: Type,
        rhsType: Type,
    ): Nothing {
        throw RuntimeTypeException(
            "Operator ${operatorType.valueLiteral()} is not applicable to ${lhsType.name} and ${rhsType.name}",
        )
    }
}
