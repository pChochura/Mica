package com.pointlessapps.granite.mica.semantics.mapper

import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.semantics.model.AnyType
import com.pointlessapps.granite.mica.semantics.model.BoolType
import com.pointlessapps.granite.mica.semantics.model.CharRangeType
import com.pointlessapps.granite.mica.semantics.model.CharType
import com.pointlessapps.granite.mica.semantics.model.IndefiniteNumberRangeType
import com.pointlessapps.granite.mica.semantics.model.NumberRangeType
import com.pointlessapps.granite.mica.semantics.model.NumberType
import com.pointlessapps.granite.mica.semantics.model.StringType
import com.pointlessapps.granite.mica.semantics.model.Type

internal fun Token.Symbol.toType(): Type? = listOf(
    AnyType(),
    BoolType,
    CharType,
    CharRangeType,
    StringType,
    NumberType,
    NumberRangeType,
    IndefiniteNumberRangeType,
).find { it.name == value }
