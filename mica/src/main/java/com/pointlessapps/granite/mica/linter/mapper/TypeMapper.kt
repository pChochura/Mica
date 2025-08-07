package com.pointlessapps.granite.mica.linter.mapper

import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.linter.model.AnyType
import com.pointlessapps.granite.mica.linter.model.BoolType
import com.pointlessapps.granite.mica.linter.model.CharRangeType
import com.pointlessapps.granite.mica.linter.model.CharType
import com.pointlessapps.granite.mica.linter.model.IndefiniteNumberRangeType
import com.pointlessapps.granite.mica.linter.model.NumberRangeType
import com.pointlessapps.granite.mica.linter.model.NumberType
import com.pointlessapps.granite.mica.linter.model.StringType
import com.pointlessapps.granite.mica.linter.model.Type

internal fun Token.Symbol.toType(): Type? = listOf(
    AnyType,
    BoolType,
    CharType,
    CharRangeType,
    StringType,
    NumberType,
    NumberRangeType,
    IndefiniteNumberRangeType,
).find { it.name == value }
