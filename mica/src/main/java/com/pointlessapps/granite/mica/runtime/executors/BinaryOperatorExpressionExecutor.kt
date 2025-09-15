package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.helper.commonSupertype
import com.pointlessapps.granite.mica.mapper.asArrayType
import com.pointlessapps.granite.mica.mapper.asBoolType
import com.pointlessapps.granite.mica.mapper.asCharRangeType
import com.pointlessapps.granite.mica.mapper.asCharType
import com.pointlessapps.granite.mica.mapper.asCustomType
import com.pointlessapps.granite.mica.mapper.asIntRangeType
import com.pointlessapps.granite.mica.mapper.asIntType
import com.pointlessapps.granite.mica.mapper.asMapType
import com.pointlessapps.granite.mica.mapper.asRealRangeType
import com.pointlessapps.granite.mica.mapper.asRealType
import com.pointlessapps.granite.mica.mapper.asSetType
import com.pointlessapps.granite.mica.mapper.asStringType
import com.pointlessapps.granite.mica.mapper.toType
import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.CharRangeType
import com.pointlessapps.granite.mica.model.CharType
import com.pointlessapps.granite.mica.model.ClosedDoubleRange
import com.pointlessapps.granite.mica.model.CustomType
import com.pointlessapps.granite.mica.model.EmptyArrayType
import com.pointlessapps.granite.mica.model.EmptyCustomType
import com.pointlessapps.granite.mica.model.EmptyMapType
import com.pointlessapps.granite.mica.model.EmptySetType
import com.pointlessapps.granite.mica.model.IntRangeType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.model.MapType
import com.pointlessapps.granite.mica.model.NumberType
import com.pointlessapps.granite.mica.model.RealRangeType
import com.pointlessapps.granite.mica.model.RealType
import com.pointlessapps.granite.mica.model.SetType
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.runtime.errors.RuntimeTypeException
import com.pointlessapps.granite.mica.runtime.model.VariableType
import com.pointlessapps.granite.mica.runtime.resolver.compareTo
import kotlin.math.pow

internal object BinaryOperatorExpressionExecutor {

    fun execute(
        lhsValue: Any?,
        rhsValue: Any?,
        operator: Token.Operator.Type,
    ): VariableType.Value = when (operator) {
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
        Token.Operator.Type.Modulo,
        Token.Operator.Type.Exponent,
            -> executeArithmeticOperator(operator, lhsValue, rhsValue)

        Token.Operator.Type.And, Token.Operator.Type.Or ->
            executeLogicalOperator(operator, lhsValue, rhsValue)

        Token.Operator.Type.Range -> executeRangeOperator(lhsValue, rhsValue)
        else -> throwIncompatibleTypesError(operator, lhsValue, rhsValue)
    }

    private fun executeComparisonOperator(
        operatorType: Token.Operator.Type,
        lhsValue: Any?,
        rhsValue: Any?,
    ): VariableType.Value {
        val comparisonResult = compare(lhsValue, rhsValue)
            ?: throwIncompatibleTypesError(operatorType, lhsValue, rhsValue)

        return VariableType.Value(
            when (operatorType) {
                Token.Operator.Type.Equals -> comparisonResult == 0
                Token.Operator.Type.NotEquals -> comparisonResult != 0
                Token.Operator.Type.GraterThan -> comparisonResult > 0
                Token.Operator.Type.LessThan -> comparisonResult < 0
                Token.Operator.Type.GraterThanOrEquals -> comparisonResult >= 0
                Token.Operator.Type.LessThanOrEquals -> comparisonResult <= 0
                else -> throwIncompatibleTypesError(operatorType, lhsValue, rhsValue)
            },
        )
    }

    private fun executeAddition(lhsValue: Any?, rhsValue: Any?): VariableType.Value {
        val commonSupertype = commonSupertype(lhsValue.toType(), rhsValue.toType())
        return VariableType.Value(
            when {
                commonSupertype.isSubtypeOf(IntType) ->
                    lhsValue.asIntType() + rhsValue.asIntType()

                commonSupertype.isSubtypeOf(NumberType) ->
                    lhsValue.asRealType(true) + rhsValue.asRealType(true)

                commonSupertype.isSubtypeOf(StringType) ->
                    lhsValue.asStringType() + rhsValue.asStringType()

                commonSupertype.isSubtypeOf(CharType) ->
                    lhsValue.asCharType().toString() + rhsValue.asCharType()

                commonSupertype.isSubtypeOf(EmptySetType) ->
                    lhsValue.asSetType() + rhsValue.asSetType()

                commonSupertype.isSubtypeOf(EmptyArrayType) ->
                    lhsValue.asArrayType() + rhsValue.asArrayType()

                commonSupertype.isSubtypeOf(EmptyMapType) ->
                    lhsValue.asMapType() + rhsValue.asMapType()

                else -> throwIncompatibleTypesError(Token.Operator.Type.Add, lhsValue, rhsValue)
            },
        )
    }

    private fun executeArithmeticOperator(
        operatorType: Token.Operator.Type,
        lhsValue: Any?,
        rhsValue: Any?,
    ): VariableType.Value {
        val commonSupertype = commonSupertype(lhsValue.toType(), rhsValue.toType())
        return VariableType.Value(
            when {
                commonSupertype.isSubtypeOf(IntType) -> {
                    val lhsLong = lhsValue.asIntType()
                    val rhsLong = rhsValue.asIntType()
                    when (operatorType) {
                        Token.Operator.Type.Subtract -> lhsLong - rhsLong
                        Token.Operator.Type.Multiply -> lhsLong * rhsLong
                        Token.Operator.Type.Divide -> lhsLong / rhsLong
                        Token.Operator.Type.Modulo -> lhsLong % rhsLong
                        Token.Operator.Type.Exponent -> lhsLong.toDouble()
                            .pow(rhsLong.toDouble())

                        else -> throwIncompatibleTypesError(operatorType, lhsValue, rhsValue)
                    }
                }

                commonSupertype.isSubtypeOf(NumberType) -> {
                    val lhsDouble = lhsValue.asRealType(true)
                    val rhsDouble = rhsValue.asRealType(true)
                    when (operatorType) {
                        Token.Operator.Type.Subtract -> lhsDouble - rhsDouble
                        Token.Operator.Type.Multiply -> lhsDouble * rhsDouble
                        Token.Operator.Type.Divide -> lhsDouble / rhsDouble
                        Token.Operator.Type.Modulo -> lhsDouble % rhsDouble
                        Token.Operator.Type.Exponent -> lhsDouble.pow(rhsDouble)
                        else -> throwIncompatibleTypesError(operatorType, lhsValue, rhsValue)
                    }
                }

                else -> throwIncompatibleTypesError(operatorType, lhsValue, rhsValue)
            },
        )
    }

    private fun executeLogicalOperator(
        operatorType: Token.Operator.Type,
        lhsValue: Any?,
        rhsValue: Any?,
    ): VariableType.Value {
        val commonSupertype = commonSupertype(lhsValue.toType(), rhsValue.toType())
        if (!commonSupertype.isSubtypeOf(BoolType)) {
            throwIncompatibleTypesError(operatorType, lhsValue, rhsValue)
        }

        val lhsBoolean = lhsValue.asBoolType()
        val rhsBoolean = rhsValue.asBoolType()
        return VariableType.Value(
            when (operatorType) {
                Token.Operator.Type.And -> lhsBoolean && rhsBoolean
                Token.Operator.Type.Or -> lhsBoolean || rhsBoolean
                else -> throwIncompatibleTypesError(operatorType, lhsValue, rhsValue)
            },
        )
    }

    private fun executeRangeOperator(lhsValue: Any?, rhsValue: Any?): VariableType.Value {
        val commonSupertype = commonSupertype(lhsValue.toType(), rhsValue.toType())

        return VariableType.Value(
            when {
                commonSupertype.isSubtypeOf(IntType) -> LongRange(
                    start = lhsValue.asIntType(),
                    endInclusive = rhsValue.asIntType(),
                )

                commonSupertype.isSubtypeOf(NumberType) -> ClosedDoubleRange(
                    start = lhsValue.asRealType(true),
                    endInclusive = rhsValue.asRealType(true),
                )

                commonSupertype.isSubtypeOf(CharType) -> CharRange(
                    start = lhsValue.asCharType(),
                    endInclusive = rhsValue.asCharType(),
                )

                else -> throwIncompatibleTypesError(Token.Operator.Type.Range, lhsValue, rhsValue)
            },
        )
    }

    private fun compare(lhsValue: Any?, rhsValue: Any?): Int? {
        val lhsType = lhsValue.toType()
        val rhsType = rhsValue.toType()
        return when {
            lhsType.isSubtypeOf(rhsType) -> when (commonSupertype(lhsType, rhsType)) {
                AnyType -> 0
                is SetType, EmptySetType ->
                    lhsValue.asSetType().compareTo(rhsValue.asSetType())

                is ArrayType, EmptyArrayType ->
                    lhsValue.asArrayType().compareTo(rhsValue.asArrayType())

                is CustomType, EmptyCustomType ->
                    lhsValue.asCustomType().compareTo(rhsValue.asCustomType())

                is MapType, EmptyMapType ->
                    lhsValue.asMapType().compareTo(rhsValue.asMapType())

                BoolType -> lhsValue.asBoolType().compareTo(rhsValue.asBoolType())
                CharType -> lhsValue.asCharType().compareTo(rhsValue.asCharType())
                CharRangeType -> lhsValue.asCharRangeType().compareTo(rhsValue.asCharRangeType())
                StringType -> lhsValue.asStringType().compareTo(rhsValue.asStringType())
                IntType -> lhsValue.asIntType().compareTo(rhsValue.asIntType())
                RealType -> lhsValue.asRealType().compareTo(rhsValue.asRealType())
                NumberType -> lhsValue.asRealType(true).compareTo(rhsValue.asRealType(true))
                IntRangeType -> lhsValue.asIntRangeType().compareTo(rhsValue.asIntRangeType())
                RealRangeType -> lhsValue.asRealRangeType().compareTo(rhsValue.asRealRangeType())

                else -> throw RuntimeTypeException(
                    "Types ${lhsType.name} and ${rhsType.name} are not compatible",
                )
            }

            else -> null
        }
    }

    private fun throwIncompatibleTypesError(
        operatorType: Token.Operator.Type,
        lhsType: Any?,
        rhsType: Any?,
    ): Nothing = throwIncompatibleTypesError(operatorType, lhsType.toType(), rhsType.toType())

    private fun throwIncompatibleTypesError(
        operatorType: Token.Operator.Type,
        lhsType: Type,
        rhsType: Type,
    ): Nothing = throw RuntimeTypeException(
        "Operator ${operatorType.literal} is not applicable to ${
            lhsType.name
        } and ${rhsType.name}",
    )
}
