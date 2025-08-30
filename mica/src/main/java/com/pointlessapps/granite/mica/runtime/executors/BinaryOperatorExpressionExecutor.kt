package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.helper.commonSupertype
import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.CharRangeType
import com.pointlessapps.granite.mica.model.CharType
import com.pointlessapps.granite.mica.model.ClosedDoubleRange
import com.pointlessapps.granite.mica.model.CustomType
import com.pointlessapps.granite.mica.model.EmptyArrayType
import com.pointlessapps.granite.mica.model.EmptyCustomType
import com.pointlessapps.granite.mica.model.EmptySetType
import com.pointlessapps.granite.mica.model.IntRangeType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.model.RealRangeType
import com.pointlessapps.granite.mica.model.RealType
import com.pointlessapps.granite.mica.model.SetType
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.model.UndefinedType
import com.pointlessapps.granite.mica.runtime.errors.RuntimeTypeException
import com.pointlessapps.granite.mica.runtime.helper.CustomObject
import com.pointlessapps.granite.mica.runtime.model.BoolVariable
import com.pointlessapps.granite.mica.runtime.model.CharRangeVariable
import com.pointlessapps.granite.mica.runtime.model.IntRangeVariable
import com.pointlessapps.granite.mica.runtime.model.RealRangeVariable
import com.pointlessapps.granite.mica.runtime.model.StringVariable
import com.pointlessapps.granite.mica.runtime.model.Variable
import com.pointlessapps.granite.mica.runtime.model.Variable.Companion.toVariable
import com.pointlessapps.granite.mica.runtime.resolver.compareTo
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

        return BoolVariable(
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
        val commonSupertype = listOf(lhsValue.type, rhsValue.type).commonSupertype()
        return when {
            commonSupertype.isSubtypeOf(IntType) -> commonSupertype.toVariable(
                lhsValue.type.valueAsSupertype<IntType>(lhsValue.value) as Long +
                        rhsValue.type.valueAsSupertype<IntType>(rhsValue.value) as Long,
            )

            commonSupertype.isSubtypeOf(RealType) -> commonSupertype.toVariable(
                lhsValue.type.valueAsSupertype<RealType>(lhsValue.value) as Double +
                        rhsValue.type.valueAsSupertype<RealType>(rhsValue.value) as Double,
            )

            commonSupertype.isSubtypeOf(StringType) -> commonSupertype.toVariable(
                lhsValue.type.valueAsSupertype<StringType>(lhsValue.value) as String +
                        rhsValue.type.valueAsSupertype<StringType>(rhsValue.value) as String,
            )

            commonSupertype.isSubtypeOf(CharType) -> StringVariable(
                (lhsValue.type.valueAsSupertype<CharType>(lhsValue.value) as Char).toString() +
                        rhsValue.type.valueAsSupertype<CharType>(rhsValue.value) as Char,
            )

            commonSupertype.isSubtypeOf(EmptyArrayType) -> commonSupertype.toVariable(
                lhsValue.type.valueAsSupertype<ArrayType>(lhsValue.value) as List<*> +
                        rhsValue.type.valueAsSupertype<ArrayType>(rhsValue.value) as List<*>,
            )

            else -> throwIncompatibleTypesError(
                Token.Operator.Type.Add,
                lhsValue.type,
                rhsValue.type
            )
        }
    }

    private fun executeArithmeticOperator(
        operatorType: Token.Operator.Type,
        lhsValue: Variable<*>,
        rhsValue: Variable<*>,
    ): Variable<*> {
        val commonSupertype = listOf(lhsValue.type, rhsValue.type).commonSupertype()
        return when {
            commonSupertype.isSubtypeOf(IntType) -> {
                val lhsLong = lhsValue.type.valueAsSupertype<IntType>(lhsValue.value) as Long
                val rhsLong = rhsValue.type.valueAsSupertype<IntType>(rhsValue.value) as Long
                commonSupertype.toVariable(
                    when (operatorType) {
                        Token.Operator.Type.Subtract -> lhsLong - rhsLong
                        Token.Operator.Type.Multiply -> lhsLong * rhsLong
                        Token.Operator.Type.Divide -> lhsLong / rhsLong
                        Token.Operator.Type.Exponent -> lhsLong.toDouble().pow(rhsLong.toDouble())
                        else -> throwIncompatibleTypesError(
                            operatorType,
                            lhsValue.type,
                            rhsValue.type
                        )
                    },
                )
            }

            commonSupertype.isSubtypeOf(RealType) -> {
                val lhsDouble = lhsValue.type.valueAsSupertype<RealType>(lhsValue.value) as Double
                val rhsDouble = rhsValue.type.valueAsSupertype<RealType>(rhsValue.value) as Double
                commonSupertype.toVariable(
                    when (operatorType) {
                        Token.Operator.Type.Subtract -> lhsDouble - rhsDouble
                        Token.Operator.Type.Multiply -> lhsDouble * rhsDouble
                        Token.Operator.Type.Divide -> lhsDouble / rhsDouble
                        Token.Operator.Type.Exponent -> lhsDouble.pow(rhsDouble)
                        else -> throwIncompatibleTypesError(
                            operatorType,
                            lhsValue.type,
                            rhsValue.type
                        )
                    },
                )
            }

            else -> throwIncompatibleTypesError(operatorType, lhsValue.type, rhsValue.type)
        }
    }

    private fun executeLogicalOperator(
        operatorType: Token.Operator.Type,
        lhsValue: Variable<*>,
        rhsValue: Variable<*>,
    ): Variable<*> {
        val commonSupertype = listOf(lhsValue.type, rhsValue.type).commonSupertype()
        if (!commonSupertype.isSubtypeOf(BoolType)) {
            throwIncompatibleTypesError(operatorType, lhsValue.type, rhsValue.type)
        }

        val lhsBoolean = lhsValue.type.valueAsSupertype<BoolType>(lhsValue.value) as Boolean
        val rhsBoolean = rhsValue.type.valueAsSupertype<BoolType>(rhsValue.value) as Boolean
        return commonSupertype.toVariable(
            when (operatorType) {
                Token.Operator.Type.And -> lhsBoolean && rhsBoolean
                Token.Operator.Type.Or -> lhsBoolean || rhsBoolean
                else -> throwIncompatibleTypesError(operatorType, lhsValue.type, rhsValue.type)
            },
        )
    }

    private fun executeRangeOperator(lhsValue: Variable<*>, rhsValue: Variable<*>): Variable<*> {
        val commonSupertype = listOf(lhsValue.type, rhsValue.type).commonSupertype()

        return when {
            commonSupertype.isSubtypeOf(IntType) -> IntRangeVariable(
                LongRange(
                    start = lhsValue.type.valueAsSupertype<IntType>(lhsValue.value) as Long,
                    endInclusive = rhsValue.type.valueAsSupertype<IntType>(rhsValue.value) as Long,
                ),
            )

            commonSupertype.isSubtypeOf(RealType) -> RealRangeVariable(
                ClosedDoubleRange(
                    start = lhsValue.type.valueAsSupertype<RealType>(lhsValue.value) as Double,
                    endInclusive = rhsValue.type.valueAsSupertype<RealType>(rhsValue.value) as Double,
                ),
            )

            commonSupertype.isSubtypeOf(CharType) -> CharRangeVariable(
                CharRange(
                    start = lhsValue.type.valueAsSupertype<CharType>(lhsValue.value) as Char,
                    endInclusive = rhsValue.type.valueAsSupertype<CharType>(rhsValue.value) as Char,
                ),
            )

            else -> throwIncompatibleTypesError(
                Token.Operator.Type.Range,
                lhsValue.type,
                rhsValue.type,
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun compare(lhsValue: Variable<*>, rhsValue: Variable<*>): Int? = when {
        lhsValue.type.isSubtypeOf(rhsValue.type) -> {
            val commonSupertype = listOf(lhsValue.type, rhsValue.type).commonSupertype()
            val lhsValueAsCommonSupertype =
                lhsValue.type.valueAsSupertype(lhsValue.value, commonSupertype)
            val rhsValueAsCommonSupertype =
                rhsValue.type.valueAsSupertype(rhsValue.value, commonSupertype)

            fun compareAsType(type: Type): Int = when (type) {
                AnyType -> 0
                is SetType, EmptySetType -> (lhsValueAsCommonSupertype as Set<*>)
                    .compareTo(rhsValueAsCommonSupertype as Set<*>)

                is ArrayType, EmptyArrayType -> (lhsValueAsCommonSupertype as List<*>)
                    .compareTo(rhsValueAsCommonSupertype as List<*>)

                is CustomType, EmptyCustomType -> (lhsValueAsCommonSupertype as CustomObject)
                    .compareTo(rhsValueAsCommonSupertype as CustomObject)

                BoolType -> (lhsValueAsCommonSupertype as Boolean)
                    .compareTo(rhsValueAsCommonSupertype as Boolean)

                CharType -> (lhsValueAsCommonSupertype as Char)
                    .compareTo(rhsValueAsCommonSupertype as Char)

                CharRangeType -> (lhsValueAsCommonSupertype as CharRange)
                    .compareTo(rhsValueAsCommonSupertype as CharRange)

                StringType -> (lhsValueAsCommonSupertype as String)
                    .compareTo(rhsValueAsCommonSupertype as String)

                IntType -> (lhsValueAsCommonSupertype as Long)
                    .compareTo(rhsValueAsCommonSupertype as Long)

                RealType -> (lhsValueAsCommonSupertype as Double)
                    .compareTo(rhsValueAsCommonSupertype as Double)

                IntRangeType -> (lhsValueAsCommonSupertype as LongRange)
                    .compareTo(rhsValueAsCommonSupertype as LongRange)

                RealRangeType -> (lhsValueAsCommonSupertype as ClosedDoubleRange)
                    .compareTo(rhsValueAsCommonSupertype as ClosedDoubleRange)

                UndefinedType -> throw RuntimeTypeException(
                    "Types ${lhsValue.type.name} and ${rhsValue.type.name} are not compatible",
                )
            }

            compareAsType(commonSupertype)
        }

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
