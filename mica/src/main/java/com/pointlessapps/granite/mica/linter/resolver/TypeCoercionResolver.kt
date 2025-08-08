package com.pointlessapps.granite.mica.linter.resolver

import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.CharRangeType
import com.pointlessapps.granite.mica.model.CharType
import com.pointlessapps.granite.mica.model.IndefiniteNumberRangeType
import com.pointlessapps.granite.mica.model.NumberRangeType
import com.pointlessapps.granite.mica.model.NumberType
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.model.UndefinedType

internal object TypeCoercionResolver {

    /**
     * Checks whether the [this] type makes sense as a [targetType] type.
     */
    fun Type.canBeCoercedTo(targetType: Type) = when (this) {
        BoolType -> targetType in listOf(BoolType, NumberType, StringType, CharType, AnyType)
        CharType -> targetType in listOf(CharType, NumberType, StringType, BoolType, AnyType)
        CharRangeType -> targetType in listOf(CharRangeType, NumberRangeType, AnyType)
        StringType -> targetType in listOf(StringType, AnyType)
        NumberType -> targetType in listOf(NumberType, StringType, CharType, BoolType, AnyType)
        NumberRangeType -> targetType in listOf(NumberRangeType, CharRangeType, AnyType)
        IndefiniteNumberRangeType -> targetType in listOf(IndefiniteNumberRangeType, AnyType)
        AnyType -> targetType == AnyType
        UndefinedType -> false
    }

    private fun resolveEqualityOperator(lhs: Type, rhs: Type): BoolType? {
        if (lhs == rhs || lhs.canBeCoercedTo(rhs) || rhs.canBeCoercedTo(lhs)) return BoolType

        return null
    }

    private fun resolveComparisonOperator(lhs: Type, rhs: Type): BoolType? {
        if (lhs.canBeCoercedTo(NumberType) && rhs.canBeCoercedTo(NumberType)) return BoolType

        return null
    }

    private fun resolveRangeOperator(lhs: Type, rhs: Type): Type? {
        val rhsCanBeCoercedToLhs = rhs.canBeCoercedTo(lhs)
        if (lhs == NumberType && rhsCanBeCoercedToLhs) return NumberRangeType
        if (lhs == CharType && rhsCanBeCoercedToLhs) return CharRangeType

        val lhsCanBeCoercedToRhs = lhs.canBeCoercedTo(rhs)
        if (rhs == NumberType && lhsCanBeCoercedToRhs) return NumberRangeType
        if (rhs == CharType && lhsCanBeCoercedToRhs) return CharRangeType

        if (lhs.canBeCoercedTo(NumberType) && rhs.canBeCoercedTo(NumberType)) return NumberRangeType

        return null
    }

    private fun resolveLogicOperator(lhs: Type, rhs: Type): BoolType? {
        if (lhs.canBeCoercedTo(BoolType) && rhs.canBeCoercedTo(BoolType)) return BoolType

        return null
    }

    private fun resolveAddOperator(lhs: Type, rhs: Type): Type? {
        val rhsCanBeCoercedToLhs = rhs.canBeCoercedTo(lhs)
        if (lhs == NumberType && rhsCanBeCoercedToLhs) return NumberType
        if (lhs == StringType && rhsCanBeCoercedToLhs) return StringType
        if (lhs == CharType && rhsCanBeCoercedToLhs) return StringType

        val lhsCanBeCoercedToRhs = lhs.canBeCoercedTo(rhs)
        if (rhs == NumberType && lhsCanBeCoercedToRhs) return NumberType
        if (rhs == StringType && lhsCanBeCoercedToRhs) return StringType
        if (rhs == CharType && lhsCanBeCoercedToRhs) return StringType

        return resolveArithmeticOperator(lhs, rhs)
    }

    private fun resolveArithmeticOperator(lhs: Type, rhs: Type): NumberType? {
        if (lhs.canBeCoercedTo(NumberType) && rhs.canBeCoercedTo(NumberType)) return NumberType

        return null
    }

    fun resolveBinaryOperator(lhs: Type, rhs: Type, operator: Token.Operator): Type? =
        when (operator.type) {
            Token.Operator.Type.Equals, Token.Operator.Type.NotEquals ->
                resolveEqualityOperator(lhs, rhs)

            Token.Operator.Type.GraterThan, Token.Operator.Type.LessThan,
            Token.Operator.Type.GraterThanOrEquals, Token.Operator.Type.LessThanOrEquals,
                -> resolveComparisonOperator(lhs, rhs)

            Token.Operator.Type.Range ->
                resolveRangeOperator(lhs, rhs)

            Token.Operator.Type.Or, Token.Operator.Type.And ->
                resolveLogicOperator(lhs, rhs)

            Token.Operator.Type.Add ->
                resolveAddOperator(lhs, rhs)

            Token.Operator.Type.Subtract, Token.Operator.Type.Multiply,
            Token.Operator.Type.Divide, Token.Operator.Type.Exponent,
                -> resolveArithmeticOperator(lhs, rhs)

            else -> throw IllegalStateException(
                "Invalid binary operator ${operator.type.literal}",
            )
        }

    fun resolvePrefixUnaryOperator(rhs: Type, operator: Token.Operator): Type? =
        when (operator.type) {
            Token.Operator.Type.Not ->
                if (rhs.canBeCoercedTo(BoolType)) BoolType else null

            Token.Operator.Type.Subtract, Token.Operator.Type.Add ->
                if (rhs.canBeCoercedTo(NumberType)) NumberType else null

            else -> throw IllegalStateException(
                "Invalid prefix operator ${operator.type.literal}",
            )
        }

    fun resolvePostfixUnaryOperator(lhs: Type, operator: Token.Operator): Type? =
        when (operator.type) {
            Token.Operator.Type.Range ->
                if (lhs.canBeCoercedTo(NumberType)) IndefiniteNumberRangeType else null

            else -> throw IllegalStateException(
                "Invalid postfix operator ${operator.type.literal}",
            )
        }
}
