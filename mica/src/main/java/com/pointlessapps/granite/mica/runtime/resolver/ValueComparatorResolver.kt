package com.pointlessapps.granite.mica.runtime.resolver

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

internal object ValueComparatorResolver {

    fun Any?.compareToAs(other: Any?, thisType: Type): Int = when (thisType) {
        is ArrayType -> (this as List<*>).compareTo(other as List<*>)
        BoolType -> (this as Boolean).compareTo(other as Boolean)
        CharType -> (this as Char).compareTo(other as Char)
        CharRangeType -> (this as CharRange).compareTo(other as CharRange)
        StringType -> (this as String).compareTo(other as String)
        IntType -> (this as Long).compareTo(other as Long)
        RealType -> (this as Double).compareTo(other as Double)
        IntRangeType -> (this as LongRange).compareTo(other as LongRange)
        RealRangeType -> (this as ClosedDoubleRange).compareTo(other as ClosedDoubleRange)
        AnyType -> 0
        UndefinedType -> throw RuntimeTypeException("Undefined type cannot be used")
    }
}
