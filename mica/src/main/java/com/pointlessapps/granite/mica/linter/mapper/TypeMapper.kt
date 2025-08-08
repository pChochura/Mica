package com.pointlessapps.granite.mica.linter.mapper

import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.CharRangeType
import com.pointlessapps.granite.mica.model.CharType
import com.pointlessapps.granite.mica.model.IndefiniteNumberRangeType
import com.pointlessapps.granite.mica.model.NumberRangeType
import com.pointlessapps.granite.mica.model.NumberType
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.model.Type

internal fun Token.Symbol.toType(): Type? = when (value) {
    AnyType.name -> AnyType
    BoolType.name -> BoolType
    CharType.name -> CharType
    CharRangeType.name -> CharRangeType
    StringType.name -> StringType
    NumberType.name -> NumberType
    NumberRangeType.name -> NumberRangeType
    IndefiniteNumberRangeType.name -> IndefiniteNumberRangeType
    else -> null
}
