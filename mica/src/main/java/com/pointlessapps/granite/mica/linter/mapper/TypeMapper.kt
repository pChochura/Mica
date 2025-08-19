package com.pointlessapps.granite.mica.linter.mapper

import com.pointlessapps.granite.mica.model.AnyType
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

internal fun Token.Symbol.toType(): Type? = value.toType()

private fun String.toType(): Type? {
    if (startsWith("[") && endsWith("]")) {
        return substring(1, length - 1).toType()?.let(::ArrayType)
    }

    return when (this) {
        AnyType.name -> AnyType
        BoolType.name -> BoolType
        CharType.name -> CharType
        CharRangeType.name -> CharRangeType
        StringType.name -> StringType
        IntType.name -> IntType
        RealType.name -> RealType
        IntRangeType.name -> IntRangeType
        RealRangeType.name -> RealRangeType
        else -> null
    }
}
