package com.pointlessapps.granite.mica.runtime.model

import com.pointlessapps.granite.mica.model.AnyType
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
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.model.UndefinedType
import com.pointlessapps.granite.mica.runtime.errors.RuntimeTypeException

internal sealed class Variable<T>(val value: T?, val type: Type) {
    companion object {
        fun Type.toVariable(value: Any?): Variable<out Any> = when (this) {
            AnyType -> AnyVariable(value)
            BoolType -> BoolVariable(value as? Boolean)
            CharRangeType -> CharRangeVariable(value as? CharRange)
            CharType -> CharVariable(value as? Char)
            RealRangeType -> RealRangeVariable(value as? ClosedDoubleRange)
            IntRangeType -> IntRangeVariable(value as? LongRange)
            IntType -> IntVariable(value as? Long)
            RealType -> RealVariable(value as? Double)
            StringType -> StringVariable(value as? String)
            is ArrayType -> ArrayVariable(value as? List<*>, elementType)
            UndefinedType -> throw RuntimeTypeException("Undefined type cannot be converted to a variable")
        }
    }
}

internal class BoolVariable(value: Boolean?) : Variable<Boolean>(value, BoolType)
internal class CharVariable(value: Char?) : Variable<Char>(value, CharType)
internal class CharRangeVariable(value: CharRange?) : Variable<CharRange>(value, CharRangeType)
internal class StringVariable(value: String?) : Variable<String>(value, StringType)
internal class IntVariable(value: Long?) : Variable<Long>(value, IntType)
internal class RealVariable(value: Double?) : Variable<Double>(value, RealType)
internal class IntRangeVariable(value: LongRange?) : Variable<LongRange>(value, IntRangeType)
internal class RealRangeVariable(value: ClosedDoubleRange?) :
    Variable<ClosedDoubleRange>(value, RealRangeType)

internal class ArrayVariable<T>(value: List<T>?, type: Type) :
    Variable<List<T>>(value, ArrayType(type))

internal class AnyVariable(value: Any?) : Variable<Any>(value, AnyType)
