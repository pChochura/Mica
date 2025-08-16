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
import com.pointlessapps.granite.mica.model.CharRangeType
import com.pointlessapps.granite.mica.model.CharType
import com.pointlessapps.granite.mica.model.ClosedDoubleRange
import com.pointlessapps.granite.mica.model.NumberRangeType
import com.pointlessapps.granite.mica.model.NumberType
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.runtime.errors.RuntimeTypeException
import com.pointlessapps.granite.mica.runtime.model.Variable
import com.pointlessapps.granite.mica.runtime.model.Variable.Companion.toVariable
import com.pointlessapps.granite.mica.runtime.resolver.ValueCoercionResolver.coerceToType
import com.pointlessapps.granite.mica.runtime.resolver.ValueComparatorResolver.compareToAs
import kotlin.math.pow

internal object BinaryOperatorExpressionExecutor {

    suspend fun execute(
        expression: BinaryExpression,
        typeResolver: TypeResolver,
        onAnyExpressionCallback: suspend (Expression) -> Variable<*>,
    ): Variable<*> {
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
            )

            Token.Operator.Type.Add -> executeAddition(
                lhsValue = onAnyExpressionCallback(expression.lhs),
                rhsValue = onAnyExpressionCallback(expression.rhs),
            )

            Token.Operator.Type.Multiply -> executeMultiplication(
                lhsValue = onAnyExpressionCallback(expression.lhs),
                rhsValue = onAnyExpressionCallback(expression.rhs),
            )

            Token.Operator.Type.Subtract,
            Token.Operator.Type.Divide,
            Token.Operator.Type.Exponent,
                -> executeArithmeticOperator(
                operatorType = operatorToken.type,
                lhsValue = onAnyExpressionCallback(expression.lhs),
                rhsValue = onAnyExpressionCallback(expression.rhs),
            )

            Token.Operator.Type.And, Token.Operator.Type.Or -> executeLogicalOperator(
                operatorType = operatorToken.type,
                lhsValue = onAnyExpressionCallback(expression.lhs),
                rhsValueCallback = { onAnyExpressionCallback(expression.rhs) },
            )

            Token.Operator.Type.Range -> executeRangeOperator(
                lhsValue = onAnyExpressionCallback(expression.lhs),
                rhsValue = onAnyExpressionCallback(expression.rhs),
            )

            else -> throwIncompatibleTypesError(operatorToken.type, lhsType, rhsType)
        }
    }

    private fun executeComparisonOperator(
        operatorType: Token.Operator.Type,
        lhsValue: Variable<*>,
        rhsValue: Variable<*>,
    ): Variable<*> {
        val comparisonResult = compare(lhsValue, rhsValue)
            ?: throwIncompatibleTypesError(operatorType, lhsValue.type, rhsValue.type)

        return BoolType.toVariable(
            when (operatorType) {
                Token.Operator.Type.Equals -> comparisonResult == 0
                Token.Operator.Type.NotEquals -> comparisonResult != 0
                Token.Operator.Type.GraterThan -> comparisonResult > 0
                Token.Operator.Type.LessThan -> comparisonResult < 0
                Token.Operator.Type.GraterThanOrEquals -> comparisonResult >= 0
                Token.Operator.Type.LessThanOrEquals -> comparisonResult <= 0
                else -> throwIncompatibleTypesError(operatorType, lhsValue.type, rhsValue.type)
            },
        )
    }

    private fun executeAddition(lhsValue: Variable<*>, rhsValue: Variable<*>): Variable<*> {
        fun asString(): Variable<*> {
            // String concatenation
            val lhsString = lhsValue.value?.coerceToType(lhsValue.type, StringType) as String
            val rhsString = rhsValue.value?.coerceToType(rhsValue.type, StringType) as String
            return StringType.toVariable(lhsString + rhsString)
        }

        fun asChar(): Variable<*> {
            // Char concatenation into a String
            val lhsChar = lhsValue.value?.coerceToType(lhsValue.type, CharType) as Char
            val rhsChar = rhsValue.value?.coerceToType(rhsValue.type, CharType) as Char
            return StringType.toVariable(lhsChar.toString() + rhsChar.toString())
        }

        fun asNumber(): Variable<*> {
            val lhsNumber = lhsValue.value?.coerceToType(lhsValue.type, NumberType) as Double
            val rhsNumber = rhsValue.value?.coerceToType(rhsValue.type, NumberType) as Double
            return NumberType.toVariable(lhsNumber + rhsNumber)
        }

        fun asArray(lhsCanBeArray: Boolean, rhsCanBeArray: Boolean): Variable<*> {
            if (lhsCanBeArray && !rhsCanBeArray) {
                val elementType = lhsValue.type.resolveElementTypeCoercedToArray()
                return ArrayType(elementType).toVariable(
                    lhsValue.value?.coerceToType(
                        originalType = lhsValue.type,
                        targetType = ArrayType(elementType),
                    ) as List<*> + listOf(
                        rhsValue.value?.coerceToType(rhsValue.type, elementType),
                    ),
                )
            }

            if (!lhsCanBeArray && rhsCanBeArray) {
                val elementType = rhsValue.type.resolveElementTypeCoercedToArray()
                return ArrayType(elementType).toVariable(
                    listOf(
                        lhsValue.value?.coerceToType(lhsValue.type, elementType)
                    ) + rhsValue.value?.coerceToType(
                        originalType = rhsValue.type,
                        targetType = ArrayType(elementType),
                    ) as List<*>,
                )
            }

            val lhsElementType = lhsValue.type.resolveElementTypeCoercedToArray()
            val rhsElementType = rhsValue.type.resolveElementTypeCoercedToArray()
            val resultElementType = listOf(lhsElementType, rhsElementType).resolveCommonBaseType()

            val lhsList = lhsValue.value?.coerceToType(
                originalType = lhsValue.type,
                targetType = ArrayType(resultElementType),
            ) as List<*>
            val rhsList = rhsValue.value?.coerceToType(
                originalType = rhsValue.type,
                targetType = ArrayType(resultElementType),
            ) as List<*>

            return ArrayType(resultElementType).toVariable(lhsList + rhsList)
        }

        val rhsCanBeCoercedToLhs = rhsValue.type.canBeCoercedTo(lhsValue.type)
        if (lhsValue.type == NumberType && rhsCanBeCoercedToLhs) return asNumber()
        if (lhsValue.type == StringType && rhsCanBeCoercedToLhs && rhsValue.type !is ArrayType) {
            return asString()
        }
        if (lhsValue.type == CharType && rhsCanBeCoercedToLhs) return asChar()

        val lhsCanBeCoercedToRhs = lhsValue.type.canBeCoercedTo(rhsValue.type)
        if (rhsValue.type == NumberType && lhsCanBeCoercedToRhs) return asNumber()
        if (rhsValue.type == StringType && lhsCanBeCoercedToRhs && lhsValue.type !is ArrayType) {
            return asString()
        }
        if (rhsValue.type == CharType && lhsCanBeCoercedToRhs) return asChar()

        val lhsCanBeArray = lhsValue.type.canBeCoercedTo(ArrayType(AnyType))
        val rhsCanBeArray = rhsValue.type.canBeCoercedTo(ArrayType(AnyType))
        if (lhsCanBeArray || rhsCanBeArray) {
            return asArray(lhsCanBeArray, rhsCanBeArray)
        }

        if (lhsValue.type.canBeCoercedTo(NumberType) && rhsValue.type.canBeCoercedTo(NumberType)) {
            return asNumber()
        }

        throwIncompatibleTypesError(Token.Operator.Type.Add, lhsValue.type, rhsValue.type)
    }

    private fun executeMultiplication(
        lhsValue: Variable<*>,
        rhsValue: Variable<*>,
    ): Variable<*> {
        if (
            lhsValue.type.canBeCoercedTo(ArrayType(AnyType)) &&
            rhsValue.type.canBeCoercedTo(NumberType)
        ) {
            val elementType = lhsValue.type.resolveElementTypeCoercedToArray()
            val lhsList = lhsValue.value?.coerceToType(lhsValue.type, ArrayType(elementType)) as List<*>
            val rhsNumber = (rhsValue.value?.coerceToType(rhsValue.type, NumberType) as Double).toInt()
            return ArrayType(AnyType).toVariable((1..rhsNumber).flatMap { lhsList })
        }

        return executeArithmeticOperator(
            operatorType = Token.Operator.Type.Multiply,
            lhsValue = lhsValue,
            rhsValue = rhsValue,
        )
    }

    private fun executeArithmeticOperator(
        operatorType: Token.Operator.Type,
        lhsValue: Variable<*>,
        rhsValue: Variable<*>,
    ): Variable<*> {
        val lhsNumber = lhsValue.value?.coerceToType(lhsValue.type, NumberType) as Double
        val rhsNumber = rhsValue.value?.coerceToType(rhsValue.type, NumberType) as Double

        return NumberType.toVariable(
            when (operatorType) {
                Token.Operator.Type.Subtract -> lhsNumber - rhsNumber
                Token.Operator.Type.Multiply -> lhsNumber * rhsNumber
                Token.Operator.Type.Divide -> lhsNumber / rhsNumber
                Token.Operator.Type.Exponent -> lhsNumber.pow(rhsNumber)
                else -> throwIncompatibleTypesError(operatorType, lhsValue.type, rhsValue.type)
            },
        )
    }

    private suspend fun executeLogicalOperator(
        operatorType: Token.Operator.Type,
        lhsValue: Variable<*>,
        rhsValueCallback: suspend () -> Variable<*>,
    ): Variable<*> {
        val lhsBoolean = lhsValue.value?.coerceToType(lhsValue.type, BoolType) as Boolean

        return when (operatorType) {
            Token.Operator.Type.And -> BoolType.toVariable(
                lhsBoolean && rhsValueCallback().let {
                    it.value?.coerceToType(it.type, BoolType) as Boolean
                },
            )

            Token.Operator.Type.Or -> BoolType.toVariable(
                lhsBoolean || rhsValueCallback().let {
                    it.value?.coerceToType(it.type, BoolType) as Boolean
                },
            )

            else -> throwIncompatibleTypesError(
                operatorType,
                lhsValue.type,
                rhsValueCallback().type,
            )
        }
    }

    private fun executeRangeOperator(lhsValue: Variable<*>, rhsValue: Variable<*>): Variable<*> {
        fun asCharRange(): Variable<*> {
            val lhsCharValue = lhsValue.value?.coerceToType(lhsValue.type, CharType) as Char
            val rhsCharValue = rhsValue.value?.coerceToType(rhsValue.type, CharType) as Char
            return CharRangeType.toVariable(CharRange(lhsCharValue, rhsCharValue))
        }

        fun asNumberRange(): Variable<*> {
            val lhsNumberValue = lhsValue.value?.coerceToType(lhsValue.type, NumberType) as Double
            val rhsNumberValue = rhsValue.value?.coerceToType(rhsValue.type, NumberType) as Double
            return NumberRangeType.toVariable(ClosedDoubleRange(lhsNumberValue, rhsNumberValue))
        }

        val rhsCanBeCoercedToLhs = rhsValue.type.canBeCoercedTo(lhsValue.type)
        if (lhsValue.type == NumberType && rhsCanBeCoercedToLhs) return asNumberRange()
        if (lhsValue.type == CharType && rhsCanBeCoercedToLhs) return asCharRange()

        val lhsCanBeCoercedToRhs = lhsValue.type.canBeCoercedTo(rhsValue.type)
        if (rhsValue.type == NumberType && lhsCanBeCoercedToRhs) return asNumberRange()
        if (rhsValue.type == CharType && lhsCanBeCoercedToRhs) return asCharRange()

        if (lhsValue.type.canBeCoercedTo(NumberType) && rhsValue.type.canBeCoercedTo(NumberType)) {
            return asNumberRange()
        }

        throwIncompatibleTypesError(Token.Operator.Type.Range, lhsValue.type, rhsValue.type)
    }

    private fun compare(lhsValue: Variable<*>, rhsValue: Variable<*>): Int? = when {
        lhsValue.type == rhsValue.type || lhsValue.type.canBeCoercedTo(rhsValue.type) ->
            lhsValue.value.compareToAs(rhsValue.value, lhsValue.type, rhsValue.type)

        rhsValue.type.canBeCoercedTo(lhsValue.type) ->
            rhsValue.value.compareToAs(lhsValue.value, rhsValue.type, lhsValue.type)

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
