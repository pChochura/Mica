package com.pointlessapps.granite.mica.linter.resolver

import com.pointlessapps.granite.mica.helper.commonSupertype
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
            Token.Operator.Type.Not -> if (rhs.isSubtypeOf(BoolType)) rhs else null
            Token.Operator.Type.Subtract, Token.Operator.Type.Add ->
                if (rhs.isSubtypeOfAny(RealType, IntType)) rhs else null

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

            Token.Operator.Type.Or, Token.Operator.Type.And -> {
                val commonSupertype = listOf(lhs, rhs).commonSupertype()
                if (commonSupertype.isSubtypeOf(BoolType)) {
                    commonSupertype
                } else {
                    null
                }
            }

            else -> throw IllegalStateException(
                "Invalid binary operator ${operator.type.literal}",
            )
        }

    private fun resolveAddTypes(lhs: Type, rhs: Type): Type? {
        val commonSupertype = listOf(lhs, rhs).commonSupertype()
        if (
            commonSupertype.isSubtypeOfAny(IntType, RealType, StringType) ||
            commonSupertype.isSubtypeOf<ArrayType>()
        ) {
            return commonSupertype
        }

        if (commonSupertype.isSubtypeOf(CharType)) {
            return StringType
        }

        return null
    }

    private fun resolveRangeTypes(lhs: Type, rhs: Type): Type? {
        val commonSupertype = listOf(lhs, rhs).commonSupertype()
        return when {
            commonSupertype.isSubtypeOf(IntType) -> IntRangeType
            commonSupertype.isSubtypeOf(RealType) -> RealRangeType
            commonSupertype.isSubtypeOf(CharType) -> CharRangeType
            else -> null
        }
    }

    private fun resolveNumberTypes(lhs: Type, rhs: Type): Type? {
        val commonSupertype = listOf(lhs, rhs).commonSupertype()
        if (commonSupertype.isSubtypeOfAny(IntType, RealType)) {
            return commonSupertype
        }

        return null
    }
}
