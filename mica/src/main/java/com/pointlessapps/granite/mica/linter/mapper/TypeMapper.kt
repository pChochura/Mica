package com.pointlessapps.granite.mica.linter.mapper

import com.pointlessapps.granite.mica.ast.expressions.ArrayTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.SetTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.TypeExpression
import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.CharRangeType
import com.pointlessapps.granite.mica.model.CharType
import com.pointlessapps.granite.mica.model.IntRangeType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.model.RealRangeType
import com.pointlessapps.granite.mica.model.RealType
import com.pointlessapps.granite.mica.model.SetType
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.model.UndefinedType

internal fun TypeExpression.toType(): Type = when (this) {
    is ArrayTypeExpression -> typeExpression.toType().let(::ArrayType)
    is SetTypeExpression -> typeExpression.toType().let(::SetType)
    is SymbolTypeExpression -> symbolToken.toType()
}

internal fun Token.Symbol.toType(): Type = when (value) {
    AnyType.name -> AnyType
    BoolType.name -> BoolType
    CharType.name -> CharType
    CharRangeType.name -> CharRangeType
    StringType.name -> StringType
    IntType.name -> IntType
    RealType.name -> RealType
    IntRangeType.name -> IntRangeType
    RealRangeType.name -> RealRangeType
    else -> UndefinedType
}
