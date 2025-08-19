package com.pointlessapps.granite.mica.linter.resolver

import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.CharRangeType
import com.pointlessapps.granite.mica.model.CharType
import com.pointlessapps.granite.mica.model.IntRangeType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.model.RealRangeType
import com.pointlessapps.granite.mica.model.RealType
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.model.Type

internal object TypeOperationResolver {

    fun resolvePrefixUnaryOperator(rhs: Type, operator: Token.Operator): Type? =
        when (operator.type) {
            Token.Operator.Type.Not -> if (rhs == BoolType) BoolType else null
            Token.Operator.Type.Subtract, Token.Operator.Type.Add -> when (rhs) {
                RealType -> RealType
                IntType -> IntType
                else -> null
            }

            else -> throw IllegalStateException("Invalid prefix operator ${operator.type.literal}")
        }

    fun resolveBinaryOperator(lhs: Type, rhs: Type, operator: Token.Operator): Type? =
        when (operator.type) {
            Token.Operator.Type.Range -> resolveRangeTypes(lhs, rhs)
            Token.Operator.Type.Add -> resolveAddTypes(lhs, rhs)

            Token.Operator.Type.Equals, Token.Operator.Type.NotEquals,
            Token.Operator.Type.GraterThan, Token.Operator.Type.LessThan,
            Token.Operator.Type.GraterThanOrEquals, Token.Operator.Type.LessThanOrEquals,
                -> if (lhs == rhs) BoolType else null

            Token.Operator.Type.Multiply, Token.Operator.Type.Subtract,
            Token.Operator.Type.Divide, Token.Operator.Type.Exponent,
                -> resolveNumberTypes(lhs, rhs)

            Token.Operator.Type.Or, Token.Operator.Type.And ->
                if (lhs == BoolType && rhs == BoolType) BoolType else null

            else -> throw IllegalStateException(
                "Invalid binary operator ${operator.type.literal}",
            )
        }

    private fun resolveAddTypes(lhs: Type, rhs: Type): Type? = when {
        lhs == IntType && rhs == IntType -> IntType
        lhs == RealType && rhs == RealType -> RealType
        lhs == StringType && rhs == StringType -> StringType
        lhs == CharType && rhs == CharType -> StringType
        lhs is ArrayType && rhs is ArrayType && lhs.elementType == rhs.elementType ->
            ArrayType(lhs.elementType)

        else -> null
    }

    private fun resolveRangeTypes(lhs: Type, rhs: Type): Type? = when {
        lhs == IntType && rhs == IntType -> IntRangeType
        lhs == RealType && rhs == RealType -> RealRangeType
        lhs == CharType && rhs == CharType -> CharRangeType
        else -> null
    }

    private fun resolveNumberTypes(lhs: Type, rhs: Type): Type? = when {
        lhs == IntType && rhs == IntType -> IntType
        lhs == RealType && rhs == RealType -> RealType
        else -> null
    }
}
