package com.pointlessapps.granite.mica.linter.mapper

import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.CharRangeType
import com.pointlessapps.granite.mica.model.CharType
import com.pointlessapps.granite.mica.model.EmptyGenericType
import com.pointlessapps.granite.mica.model.GenericType
import com.pointlessapps.granite.mica.model.IntRangeType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.model.NumberType
import com.pointlessapps.granite.mica.model.RealRangeType
import com.pointlessapps.granite.mica.model.RealType
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.model.Type

private val types = mapOf(
    AnyType.name to AnyType,
    BoolType.name to BoolType,
    CharType.name to CharType,
    CharRangeType.name to CharRangeType,
    StringType.name to StringType,
    IntType.name to IntType,
    RealType.name to RealType,
    NumberType.name to NumberType,
    IntRangeType.name to IntRangeType,
    RealRangeType.name to RealRangeType,
    GenericType.NAME to EmptyGenericType,
)

internal fun Token.Symbol.toBuiltinType(): Type? = types[this.value]
