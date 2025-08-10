package com.pointlessapps.granite.mica.linter.resolver

import com.pointlessapps.granite.mica.comparators.TypeComparator
import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.ArrayType
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
    fun Type.canBeCoercedTo(targetType: Type): Boolean = when (this) {
        is ArrayType -> when {
            targetType is ArrayType && elementType.canBeCoercedTo(targetType.elementType) -> true
            targetType in listOf(StringType, AnyType) -> true
            else -> false
        }

        BoolType -> targetType in listOf(BoolType, NumberType, StringType, CharType, AnyType)
        CharType -> targetType in listOf(CharType, NumberType, StringType, BoolType, AnyType)
        CharRangeType -> when (targetType) {
            in listOf(CharRangeType, NumberRangeType, StringType, AnyType) -> true
            is ArrayType -> true
            else -> false
        }

        // TODO add Array coercion
        StringType -> targetType in listOf(StringType, AnyType)
        NumberType -> targetType in listOf(NumberType, StringType, CharType, BoolType, AnyType)

        NumberRangeType -> when (targetType) {
            in listOf(NumberRangeType, CharRangeType, StringType, AnyType) -> true
            is ArrayType -> true
            else -> false
        }

        IndefiniteNumberRangeType -> targetType in listOf(IndefiniteNumberRangeType, AnyType)
        AnyType -> targetType == AnyType
        UndefinedType -> false
    }

    /**
     * Coerces [this] type to the [ArrayType] and then returns the type of the elements.
     */
    fun Type.resolveElementTypeCoercedToArray(): Type = when (this) {
        is ArrayType -> elementType
        CharRangeType -> CharType
        NumberRangeType -> NumberType
        // TODO add String -> Array coercion
        BoolType, CharType, StringType, NumberType,
        IndefiniteNumberRangeType, UndefinedType, AnyType,
            -> throw IllegalStateException("$this cannot be coerced to an array type")
    }

    /**
     * Resolves the common type of the [this] list of types.
     * The result represents a type that all of the types in the list can be coerced to.
     */
    fun List<Type>.resolveCommonBaseType(): Type {
        val typesToCheck = listOf(
            IndefiniteNumberRangeType,
            NumberRangeType, CharRangeType,
            NumberType, CharType, BoolType,
            StringType,
        ) + filterIsInstance<ArrayType>()
        typesToCheck.sortedWith(TypeComparator).forEach { typeToCheck ->
            // If we don't find an exact match in that list, we are sure we won't be able to
            // coerce those types into anything in that types list
            val matchedTypes = filter { it == typeToCheck }
                .takeIf(List<Type>::isNotEmpty) ?: return@forEach

            matchedTypes.sortedWith(TypeComparator).forEach { matchedType ->
                if (all { it.canBeCoercedTo(matchedType) }) {
                    return matchedType
                }
            }
        }

        return AnyType
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

        val lhsCanBeArray = lhs.canBeCoercedTo(ArrayType(AnyType))
        val rhsCanBeArray = rhs.canBeCoercedTo(ArrayType(AnyType))
        if (lhsCanBeArray && rhsCanBeArray) {
            val commonElementType = listOf(
                lhs.resolveElementTypeCoercedToArray(),
                rhs.resolveElementTypeCoercedToArray(),
            ).resolveCommonBaseType()

            return ArrayType(commonElementType)
        }

        // If only the lhs can be coerced into an array, use its element type
        if (lhsCanBeArray) {
            return ArrayType(lhs.resolveElementTypeCoercedToArray())
        }

        // If only the rhs can be coerced into an array, use its element type
        if (rhsCanBeArray) {
            return ArrayType(rhs.resolveElementTypeCoercedToArray())
        }

        return resolveArithmeticOperator(lhs, rhs)
    }

    private fun resolveMultiplyOperator(lhs: Type, rhs: Type): Type? {
        // Resolve array being multiplied by a number
        // [1, 2] * 2 -> [1, 2, 1, 2]
        // 1..4 * 2 -> [1, 2, 3, 4, 1, 2, 3, 4]
        if (lhs.canBeCoercedTo(ArrayType(AnyType)) && rhs.canBeCoercedTo(NumberType)) {
            return ArrayType(lhs.resolveElementTypeCoercedToArray())
        }

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

            Token.Operator.Type.Multiply ->
                resolveMultiplyOperator(lhs, rhs)

            Token.Operator.Type.Subtract, Token.Operator.Type.Divide, Token.Operator.Type.Exponent ->
                resolveArithmeticOperator(lhs, rhs)

            else -> throw IllegalStateException(
                "Invalid binary operator ${operator.type.literal}",
            )
        }

    fun resolvePrefixUnaryOperator(rhs: Type, operator: Token.Operator): Type? =
        when (operator.type) {
            Token.Operator.Type.Not ->
                if (rhs.canBeCoercedTo(BoolType)) BoolType else null

            Token.Operator.Type.Subtract, Token.Operator.Type.Add -> when {
                rhs.canBeCoercedTo(ArrayType(AnyType)) && rhs.resolveElementTypeCoercedToArray()
                    .canBeCoercedTo(NumberType) -> ArrayType(NumberType)

                rhs.canBeCoercedTo(NumberType) -> NumberType
                else -> null
            }

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
