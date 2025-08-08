package com.pointlessapps.granite.mica.runtime.resolver

import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.CharRangeType
import com.pointlessapps.granite.mica.model.CharType
import com.pointlessapps.granite.mica.model.ClosedDoubleRange
import com.pointlessapps.granite.mica.model.IndefiniteNumberRangeType
import com.pointlessapps.granite.mica.model.NumberRangeType
import com.pointlessapps.granite.mica.model.NumberType
import com.pointlessapps.granite.mica.model.OpenEndDoubleRange
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.model.UndefinedType
import com.pointlessapps.granite.mica.runtime.errors.RuntimeTypeException
import com.pointlessapps.granite.mica.runtime.resolver.ValueCoercionResolver.coerceToType

internal object ValueComparatorResolver {

    fun Any.compareToAs(other: Any, thisType: Type, otherType: Type): Int {
        val coercedValue = this.coerceToType(thisType, otherType)

        return when (otherType) {
            BoolType -> (coercedValue as Boolean).compareTo(other as Boolean)
            CharType -> (coercedValue as Char).compareTo(other as Char)
            CharRangeType -> (coercedValue as CharRange).compareTo(other as CharRange)
            StringType -> (coercedValue as String).compareTo(other as String)
            NumberType -> (coercedValue as Double).compareTo(other as Double)
            NumberRangeType -> (coercedValue as ClosedDoubleRange).compareTo(other as ClosedDoubleRange)
            IndefiniteNumberRangeType -> (coercedValue as OpenEndDoubleRange).compareTo(other as OpenEndDoubleRange)
            AnyType -> 0
            UndefinedType -> throw RuntimeTypeException("Undefined type cannot be used")
        }
    }
}
