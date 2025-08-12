package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.ast.expressions.BinaryExpression
import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.linter.resolver.TypeCoercionResolver.canBeCoercedTo
import com.pointlessapps.granite.mica.linter.resolver.TypeCoercionResolver.resolveCommonBaseType
import com.pointlessapps.granite.mica.linter.resolver.TypeCoercionResolver.resolveElementTypeCoercedToArray
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.CharType
import com.pointlessapps.granite.mica.model.ClosedDoubleRange
import com.pointlessapps.granite.mica.model.NumberType
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.runtime.errors.RuntimeTypeException
import com.pointlessapps.granite.mica.runtime.resolver.ValueCoercionResolver.coerceToType
import com.pointlessapps.granite.mica.runtime.resolver.ValueComparatorResolver.compareToAs
import kotlin.math.pow

internal object BinaryOperatorExpressionExecutor {

    suspend fun execute(
        expression: BinaryExpression,
        typeResolver: TypeResolver,
        onAnyExpressionCallback: suspend (Expression) -> Any,
    ): Any {
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
                operatorType = operatorToken.type,
                lhsValue = onAnyExpressionCallback(expression.lhs),
                rhsValue = onAnyExpressionCallback(expression.rhs),
                lhsType = lhsType,
                rhsType = rhsType,
            )

            Token.Operator.Type.Add -> executeAddition(
                lhsValue = onAnyExpressionCallback(expression.lhs),
                rhsValue = onAnyExpressionCallback(expression.rhs),
                lhsType = lhsType,
                rhsType = rhsType,
            )

            Token.Operator.Type.Multiply -> executeMultiplication(
                lhsValue = onAnyExpressionCallback(expression.lhs),
                rhsValue = onAnyExpressionCallback(expression.rhs),
                lhsType = lhsType,
                rhsType = rhsType,
            )

            Token.Operator.Type.Subtract,
            Token.Operator.Type.Divide,
            Token.Operator.Type.Exponent,
                -> executeArithmeticOperator(
                operatorType = operatorToken.type,
                lhsValue = onAnyExpressionCallback(expression.lhs),
                rhsValue = onAnyExpressionCallback(expression.rhs),
                lhsType = lhsType,
                rhsType = rhsType,
            )

            Token.Operator.Type.And, Token.Operator.Type.Or -> executeLogicalOperator(
                operatorType = operatorToken.type,
                lhsValue = onAnyExpressionCallback(expression.lhs),
                rhsValueCallback = { onAnyExpressionCallback(expression.rhs) },
                lhsType = lhsType,
                rhsType = rhsType,
            )

            Token.Operator.Type.Range -> executeRangeOperator(
                lhsValue = onAnyExpressionCallback(expression.lhs),
                rhsValue = onAnyExpressionCallback(expression.rhs),
                lhsType = lhsType,
                rhsType = rhsType,
            )

            else -> throwIncompatibleTypesError(operatorToken.type, lhsType, rhsType)
        }
    }

    private fun executeComparisonOperator(
        operatorType: Token.Operator.Type,
        lhsValue: Any,
        rhsValue: Any,
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
        lhsValue: Any,
        rhsValue: Any,
        lhsType: Type,
        rhsType: Type,
    ): Any {
        fun asString(): String {
            // String concatenation
            val lhsString = lhsValue.coerceToType(lhsType, StringType) as String
            val rhsString = rhsValue.coerceToType(rhsType, StringType) as String
            return lhsString + rhsString
        }

        fun asChar(): String {
            // Char concatenation into a String
            val lhsChar = lhsValue.coerceToType(lhsType, CharType) as Char
            val rhsChar = rhsValue.coerceToType(rhsType, CharType) as Char
            return lhsChar.toString() + rhsChar.toString()
        }

        fun asNumber(): Double {
            val lhsNumber = lhsValue.coerceToType(lhsType, NumberType) as Double
            val rhsNumber = rhsValue.coerceToType(rhsType, NumberType) as Double
            return lhsNumber + rhsNumber
        }

        fun asArray(lhsCanBeArray: Boolean, rhsCanBeArray: Boolean): List<*> {
            if (lhsCanBeArray && !rhsCanBeArray) {
                val elementType = lhsType.resolveElementTypeCoercedToArray()
                return lhsValue.coerceToType(
                    originalType = lhsType,
                    targetType = ArrayType(elementType),
                ) as List<*> + listOf(
                    rhsValue.coerceToType(rhsType, elementType),
                )
            }

            if (!lhsCanBeArray && rhsCanBeArray) {
                val elementType = rhsType.resolveElementTypeCoercedToArray()
                return listOf(
                    lhsValue.coerceToType(lhsType, elementType)
                ) + rhsValue.coerceToType(
                    originalType = rhsType,
                    targetType = ArrayType(elementType),
                ) as List<*>
            }

            val lhsElementType = lhsType.resolveElementTypeCoercedToArray()
            val rhsElementType = rhsType.resolveElementTypeCoercedToArray()
            val resultElementType = listOf(lhsElementType, rhsElementType).resolveCommonBaseType()

            val lhsList = lhsValue.coerceToType(
                originalType = lhsType,
                targetType = ArrayType(resultElementType),
            ) as List<*>
            val rhsList = rhsValue.coerceToType(
                originalType = rhsType,
                targetType = ArrayType(resultElementType),
            ) as List<*>

            return lhsList + rhsList
        }

        val rhsCanBeCoercedToLhs = rhsType.canBeCoercedTo(lhsType)
        if (lhsType == NumberType && rhsCanBeCoercedToLhs) return asNumber()
        if (lhsType == StringType && rhsCanBeCoercedToLhs && rhsType !is ArrayType) return asString()
        if (lhsType == CharType && rhsCanBeCoercedToLhs) return asChar()

        val lhsCanBeCoercedToRhs = lhsType.canBeCoercedTo(rhsType)
        if (rhsType == NumberType && lhsCanBeCoercedToRhs) return asNumber()
        if (rhsType == StringType && lhsCanBeCoercedToRhs && lhsType !is ArrayType) return asString()
        if (rhsType == CharType && lhsCanBeCoercedToRhs) return asChar()

        val lhsCanBeArray = lhsType.canBeCoercedTo(ArrayType(AnyType))
        val rhsCanBeArray = rhsType.canBeCoercedTo(ArrayType(AnyType))
        if (lhsCanBeArray || rhsCanBeArray) {
            return asArray(lhsCanBeArray, rhsCanBeArray)
        }

        if (lhsType.canBeCoercedTo(NumberType) && rhsType.canBeCoercedTo(NumberType)) return asNumber()

        throwIncompatibleTypesError(Token.Operator.Type.Add, lhsType, rhsType)
    }

    private fun executeMultiplication(
        lhsValue: Any,
        rhsValue: Any,
        lhsType: Type,
        rhsType: Type,
    ): Any {
        if (lhsType.canBeCoercedTo(ArrayType(AnyType)) && rhsType.canBeCoercedTo(NumberType)) {
            val elementType = lhsType.resolveElementTypeCoercedToArray()
            val lhsList = lhsValue.coerceToType(lhsType, ArrayType(elementType)) as List<*>
            val rhsNumber = (rhsValue.coerceToType(rhsType, NumberType) as Double).toInt()
            return (1..rhsNumber).flatMap { lhsList }
        }

        return executeArithmeticOperator(
            operatorType = Token.Operator.Type.Multiply,
            lhsValue = lhsValue,
            rhsValue = rhsValue,
            lhsType = lhsType,
            rhsType = rhsType,
        )
    }

    private fun executeArithmeticOperator(
        operatorType: Token.Operator.Type,
        lhsValue: Any,
        rhsValue: Any,
        lhsType: Type,
        rhsType: Type,
    ): Double {
        val lhsNumber = lhsValue.coerceToType(lhsType, NumberType) as Double
        val rhsNumber = rhsValue.coerceToType(rhsType, NumberType) as Double

        return when (operatorType) {
            Token.Operator.Type.Subtract -> lhsNumber - rhsNumber
            Token.Operator.Type.Multiply -> lhsNumber * rhsNumber
            Token.Operator.Type.Divide -> lhsNumber / rhsNumber
            Token.Operator.Type.Exponent -> lhsNumber.pow(rhsNumber)
            else -> throwIncompatibleTypesError(operatorType, lhsType, rhsType)
        }
    }

    private suspend fun executeLogicalOperator(
        operatorType: Token.Operator.Type,
        lhsValue: Any,
        rhsValueCallback: suspend () -> Any,
        lhsType: Type,
        rhsType: Type,
    ): Boolean {
        val lhsBoolean = lhsValue.coerceToType(lhsType, BoolType) as Boolean

        return when (operatorType) {
            Token.Operator.Type.And ->
                lhsBoolean && rhsValueCallback().coerceToType(rhsType, BoolType) as Boolean

            Token.Operator.Type.Or ->
                lhsBoolean || rhsValueCallback().coerceToType(rhsType, BoolType) as Boolean

            else -> throwIncompatibleTypesError(operatorType, lhsType, rhsType)
        }
    }

    private fun executeRangeOperator(
        lhsValue: Any,
        rhsValue: Any,
        lhsType: Type,
        rhsType: Type,
    ): Any {
        fun asCharRange(): CharRange {
            val lhsCharValue = lhsValue.coerceToType(lhsType, CharType) as Char
            val rhsCharValue = rhsValue.coerceToType(rhsType, CharType) as Char
            return CharRange(lhsCharValue, rhsCharValue)
        }

        fun asNumberRange(): ClosedDoubleRange {
            val lhsNumberValue = lhsValue.coerceToType(lhsType, NumberType) as Double
            val rhsNumberValue = rhsValue.coerceToType(rhsType, NumberType) as Double
            return ClosedDoubleRange(lhsNumberValue, rhsNumberValue)
        }

        val rhsCanBeCoercedToLhs = rhsType.canBeCoercedTo(lhsType)
        if (lhsType == NumberType && rhsCanBeCoercedToLhs) return asNumberRange()
        if (lhsType == CharType && rhsCanBeCoercedToLhs) return asCharRange()

        val lhsCanBeCoercedToRhs = lhsType.canBeCoercedTo(rhsType)
        if (rhsType == NumberType && lhsCanBeCoercedToRhs) return asNumberRange()
        if (rhsType == CharType && lhsCanBeCoercedToRhs) return asCharRange()

        if (lhsType.canBeCoercedTo(NumberType) && rhsType.canBeCoercedTo(NumberType)) return asNumberRange()

        throwIncompatibleTypesError(Token.Operator.Type.Range, lhsType, rhsType)
    }

    private fun compare(
        lhsValue: Any,
        rhsValue: Any,
        lhsType: Type,
        rhsType: Type,
    ): Int? = when {
        lhsType == rhsType || lhsType.canBeCoercedTo(rhsType) ->
            lhsValue.compareToAs(rhsValue, lhsType, rhsType)

        rhsType.canBeCoercedTo(lhsType) -> rhsValue.compareToAs(lhsValue, rhsType, lhsType)
        else -> null
    }

    private fun throwIncompatibleTypesError(
        operatorType: Token.Operator.Type,
        lhsType: Type,
        rhsType: Type,
    ): Nothing {
        throw RuntimeTypeException(
            "Operator ${operatorType.literal} is not applicable to ${lhsType.name} and ${rhsType.name}",
        )
    }
}
