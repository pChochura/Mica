package com.pointlessapps.granite.mica.runtime.model

import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.CharRangeType
import com.pointlessapps.granite.mica.model.CharType
import com.pointlessapps.granite.mica.model.ClosedFloatRange
import com.pointlessapps.granite.mica.model.IndefiniteNumberRangeType
import com.pointlessapps.granite.mica.model.NumberRangeType
import com.pointlessapps.granite.mica.model.NumberType
import com.pointlessapps.granite.mica.model.OpenEndFloatRange
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.model.UndefinedType
import com.pointlessapps.granite.mica.runtime.errors.RuntimeTypeException

internal sealed class Variable<T>(var value: T?, val type: Type) {
    companion object {
        fun Type.toVariable(value: Any): Variable<out Any> = when (this) {
            AnyType -> AnyVariable(value)
            BoolType -> BoolVariable(value as Boolean)
            CharRangeType -> CharRangeVariable(value as CharRange)
            CharType -> CharVariable(value as Char)
            IndefiniteNumberRangeType -> IndefiniteNumberRangeVariable(value as OpenEndFloatRange)
            NumberRangeType -> NumberRangeVariable(value as ClosedFloatRange)
            NumberType -> NumberVariable(value as Float)
            StringType -> StringVariable(value as String)
            UndefinedType -> throw RuntimeTypeException("Undefined type cannot be converted to a variable")
        }
    }
}

internal class BoolVariable(value: Boolean?) : Variable<Boolean>(value, BoolType)
internal class CharVariable(value: Char?) : Variable<Char>(value, CharType)
internal class CharRangeVariable(value: CharRange?) : Variable<CharRange>(value, CharRangeType)
internal class StringVariable(value: String?) : Variable<String>(value, StringType)
internal class NumberVariable(value: Float?) : Variable<Float>(value, NumberType)
internal class NumberRangeVariable(value: ClosedFloatRange?) :
    Variable<ClosedFloatRange>(value, NumberRangeType)

internal class IndefiniteNumberRangeVariable(value: OpenEndFloatRange?) :
    Variable<OpenEndFloatRange>(value, IndefiniteNumberRangeType)

internal class AnyVariable(value: Any?) : Variable<Any>(value, AnyType)
