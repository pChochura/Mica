package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.helper.commonSupertype
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.CharRangeType
import com.pointlessapps.granite.mica.model.CharType
import com.pointlessapps.granite.mica.model.ClosedDoubleRange
import com.pointlessapps.granite.mica.model.IntRangeType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.model.RealRangeType
import com.pointlessapps.granite.mica.model.RealType
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.runtime.errors.RuntimeTypeException
import com.pointlessapps.granite.mica.runtime.model.Variable
import com.pointlessapps.granite.mica.runtime.model.Variable.Companion.toVariable
import com.pointlessapps.granite.mica.runtime.resolver.ValueComparatorResolver.compareToAs
import kotlin.math.pow

internal object BinaryOperatorExpressionExecutor {

    fun execute(
        lhsValue: Variable<*>,
        rhsValue: Variable<*>,
        operator: Token.Operator.Type,
    ): Variable<*> = when (operator) {
        Token.Operator.Type.Equals,
        Token.Operator.Type.NotEquals,
        Token.Operator.Type.GraterThan,
        Token.Operator.Type.LessThan,
        Token.Operator.Type.GraterThanOrEquals,
        Token.Operator.Type.LessThanOrEquals,
            -> executeComparisonOperator(operator, lhsValue, rhsValue)

        Token.Operator.Type.Add -> executeAddition(lhsValue, rhsValue)

        Token.Operator.Type.Subtract,
        Token.Operator.Type.Divide,
        Token.Operator.Type.Multiply,
        Token.Operator.Type.Exponent,
            -> executeArithmeticOperator(operator, lhsValue, rhsValue)

        Token.Operator.Type.And, Token.Operator.Type.Or ->
            executeLogicalOperator(operator, lhsValue, rhsValue)

        Token.Operator.Type.Range -> executeRangeOperator(lhsValue, rhsValue)
        else -> throwIncompatibleTypesError(operator, lhsValue.type, rhsValue.type)
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

    private fun executeAddition(lhsValue: Variable<*>, rhsValue: Variable<*>): Variable<*> = when {
        lhsValue.type == IntType && rhsValue.type == IntType ->
            IntType.toVariable(lhsValue.value as Long + rhsValue.value as Long)

        lhsValue.type == RealType && rhsValue.type == RealType ->
            RealType.toVariable(lhsValue.value as Double + rhsValue.value as Double)

        lhsValue.type == StringType && rhsValue.type == StringType ->
            StringType.toVariable(lhsValue.value as String + rhsValue.value as String)

        lhsValue.type == CharType && rhsValue.type == CharType ->
            StringType.toVariable((lhsValue.value as Char).toString() + rhsValue.value as Char)

        lhsValue.type is ArrayType && rhsValue.type is ArrayType &&
                lhsValue.type.elementType.isSupertypeOf(rhsValue.type.elementType) ->
            ArrayType(listOf(lhsValue.type, rhsValue.type).commonSupertype())
                .toVariable((lhsValue.value as List<*>) + (rhsValue.value as List<*>))

        else -> throwIncompatibleTypesError(Token.Operator.Type.Add, lhsValue.type, rhsValue.type)
    }

    private fun executeArithmeticOperator(
        operatorType: Token.Operator.Type,
        lhsValue: Variable<*>,
        rhsValue: Variable<*>,
    ): Variable<*> = when {
        lhsValue.type == IntType && rhsValue.type == IntType -> IntType.toVariable(
            when (operatorType) {
                Token.Operator.Type.Subtract -> lhsValue.value as Long - rhsValue.value as Long
                Token.Operator.Type.Multiply -> lhsValue.value as Long * rhsValue.value as Long
                Token.Operator.Type.Divide -> lhsValue.value as Long / rhsValue.value as Long
                Token.Operator.Type.Exponent ->
                    ((lhsValue.value as Long).toDouble()).pow((rhsValue.value as Long).toDouble())

                else -> throwIncompatibleTypesError(operatorType, lhsValue.type, rhsValue.type)
            },
        )

        lhsValue.type == RealType && rhsValue.type == RealType -> RealType.toVariable(
            when (operatorType) {
                Token.Operator.Type.Subtract -> lhsValue.value as Double - rhsValue.value as Double
                Token.Operator.Type.Multiply -> lhsValue.value as Double * rhsValue.value as Double
                Token.Operator.Type.Divide -> lhsValue.value as Double / rhsValue.value as Double
                Token.Operator.Type.Exponent ->
                    (lhsValue.value as Double).pow(rhsValue.value as Double)

                else -> throwIncompatibleTypesError(operatorType, lhsValue.type, rhsValue.type)
            },
        )

        else -> throwIncompatibleTypesError(operatorType, lhsValue.type, rhsValue.type)
    }

    private fun executeLogicalOperator(
        operatorType: Token.Operator.Type,
        lhsValue: Variable<*>,
        rhsValue: Variable<*>,
    ): Variable<*> = when (operatorType) {
        Token.Operator.Type.And ->
            BoolType.toVariable(lhsValue.value as Boolean && rhsValue.value as Boolean)

        Token.Operator.Type.Or ->
            BoolType.toVariable(lhsValue.value as Boolean || rhsValue.value as Boolean)

        else -> throwIncompatibleTypesError(operatorType, lhsValue.type, rhsValue.type)
    }

    private fun executeRangeOperator(lhsValue: Variable<*>, rhsValue: Variable<*>): Variable<*> =
        when {
            lhsValue.type == IntType && rhsValue.type == IntType ->
                IntRangeType.toVariable(LongRange(lhsValue.value as Long, rhsValue.value as Long))

            lhsValue.type == RealType && rhsValue.type == RealType -> RealRangeType.toVariable(
                ClosedDoubleRange(lhsValue.value as Double, rhsValue.value as Double),
            )

            lhsValue.type == CharType && rhsValue.type == CharType ->
                CharRangeType.toVariable(CharRange(lhsValue.value as Char, rhsValue.value as Char))

            else -> throwIncompatibleTypesError(
                Token.Operator.Type.Range,
                lhsValue.type,
                rhsValue.type,
            )
        }

    private fun compare(lhsValue: Variable<*>, rhsValue: Variable<*>): Int? = when {
        lhsValue.type == rhsValue.type -> lhsValue.value.compareToAs(rhsValue.value, lhsValue.type)
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
