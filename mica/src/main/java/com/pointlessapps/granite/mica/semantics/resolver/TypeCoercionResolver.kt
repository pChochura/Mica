package com.pointlessapps.granite.mica.semantics.resolver

import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.semantics.model.AnyType
import com.pointlessapps.granite.mica.semantics.model.BoolType
import com.pointlessapps.granite.mica.semantics.model.CharRangeType
import com.pointlessapps.granite.mica.semantics.model.CharType
import com.pointlessapps.granite.mica.semantics.model.IndefiniteNumberRangeType
import com.pointlessapps.granite.mica.semantics.model.NumberRangeType
import com.pointlessapps.granite.mica.semantics.model.NumberType
import com.pointlessapps.granite.mica.semantics.model.StringType
import com.pointlessapps.granite.mica.semantics.model.Type
import com.pointlessapps.granite.mica.semantics.model.VoidType

internal object TypeCoercionResolver {

    fun Type.canBeCoercedTo(targetType: Type) = when (this) {
        BoolType -> targetType in listOf(BoolType, NumberType, StringType, CharType)
        CharType -> targetType in listOf(CharType, NumberType, StringType, BoolType)
        CharRangeType -> targetType in listOf(CharRangeType, NumberRangeType)
        StringType -> targetType in listOf(StringType)
        NumberType -> targetType in listOf(NumberType, StringType, CharType, BoolType)
        NumberRangeType -> targetType in listOf(NumberRangeType, CharRangeType)
        IndefiniteNumberRangeType -> targetType in listOf(IndefiniteNumberRangeType)
        is AnyType -> true
        is VoidType -> false
    }

    private fun resolveEqualityOperator(lhs: Type, rhs: Type): Type? {
        if (lhs == rhs || lhs.canBeCoercedTo(rhs) || rhs.canBeCoercedTo(lhs)) return BoolType

        return null
    }

    private fun resolveRangeOperator(lhs: Type, rhs: Type): Type? {
        val rhsCanBeCoercedToLhs = rhs.canBeCoercedTo(lhs)
        if (lhs == NumberType && rhsCanBeCoercedToLhs) return NumberRangeType
        if (lhs == CharType && rhsCanBeCoercedToLhs) return CharRangeType

        val lhsCanBeCoercedToRhs = lhs.canBeCoercedTo(rhs)
        if (rhs == NumberType && lhsCanBeCoercedToRhs) return NumberRangeType
        if (rhs == CharType && lhsCanBeCoercedToRhs) return CharRangeType

        return null
    }

    private fun resolveLogicOperator(lhs: Type, rhs: Type): Type? {
        if (lhs == BoolType && rhs.canBeCoercedTo(lhs)) return BoolType
        if (rhs == BoolType && lhs.canBeCoercedTo(rhs)) return BoolType

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

        return null
    }

    private fun resolveArithmeticOperator(lhs: Type, rhs: Type): Type? {
        if (lhs == NumberType && rhs.canBeCoercedTo(lhs)) return NumberType
        if (rhs == NumberType && lhs.canBeCoercedTo(rhs)) return NumberType

        return null
    }

    private fun resolveInequalityOperator(lhs: Type, rhs: Type): Type? {
        if (lhs == NumberType && rhs.canBeCoercedTo(lhs)) return BoolType
        if (rhs == NumberType && lhs.canBeCoercedTo(rhs)) return BoolType

        return null
    }

    fun resolveBinaryOperator(lhs: Type, rhs: Type, operator: Token.Operator): Type? =
        when (operator.type) {
            Token.Operator.Type.Equals, Token.Operator.Type.NotEquals ->
                resolveEqualityOperator(lhs, rhs)

            Token.Operator.Type.Range ->
                resolveRangeOperator(lhs, rhs)

            Token.Operator.Type.Or, Token.Operator.Type.And ->
                resolveLogicOperator(lhs, rhs)

            Token.Operator.Type.Add ->
                resolveAddOperator(lhs, rhs)

            Token.Operator.Type.Subtract, Token.Operator.Type.Multiply,
            Token.Operator.Type.Divide, Token.Operator.Type.Exponent,
                -> resolveArithmeticOperator(lhs, rhs)

            Token.Operator.Type.GraterThan, Token.Operator.Type.LessThan,
            Token.Operator.Type.GraterThanOrEquals, Token.Operator.Type.LessThanOrEquals,
                -> resolveInequalityOperator(lhs, rhs)

            Token.Operator.Type.Not -> throw IllegalStateException("Invalid binary operator ${operator.type}")
        }

    fun resolvePrefixUnaryOperator(rhs: Type, operator: Token.Operator): Type? =
        when (operator.type) {
            Token.Operator.Type.Not ->
                if (rhs.canBeCoercedTo(BoolType)) BoolType else null

            Token.Operator.Type.Subtract, Token.Operator.Type.Add ->
                if (rhs.canBeCoercedTo(NumberType)) NumberType else null

            Token.Operator.Type.Range ->
                if (rhs.canBeCoercedTo(NumberType)) IndefiniteNumberRangeType else null

            else -> null
        }

    fun resolvePostfixUnaryOperator(lhs: Type, operator: Token.Operator): Type? =
        when (operator.type) {
            Token.Operator.Type.Range ->
                if (lhs.canBeCoercedTo(NumberType)) IndefiniteNumberRangeType else null

            else -> null
        }
}
