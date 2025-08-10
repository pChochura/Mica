package com.pointlessapps.granite.mica.comparators

import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.CharRangeType
import com.pointlessapps.granite.mica.model.CharType
import com.pointlessapps.granite.mica.model.IndefiniteNumberRangeType
import com.pointlessapps.granite.mica.model.NumberRangeType
import com.pointlessapps.granite.mica.model.NumberType
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.model.Type

internal fun Type.compareTo(other: Type): Int = this.sortOrder().compareTo(other.sortOrder())

internal object TypeComparator : Comparator<Type> {
    override fun compare(p0: Type, p1: Type): Int {
        if (p0 is ArrayType && p1 is ArrayType) return p0.elementType.compareTo(p1.elementType)

        return p0.compareTo(p1)
    }
}

private fun Type.sortOrder(): Int = when (this) {
    IndefiniteNumberRangeType -> 0
    NumberRangeType -> 1
    CharRangeType -> 2
    is ArrayType -> 3
    NumberType -> 4
    CharType -> 5
    BoolType -> 6
    StringType -> 7
    else -> Int.MAX_VALUE
}
