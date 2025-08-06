package com.pointlessapps.granite.mica.semantics.model

internal sealed class Type(open val name: String)

internal open class AnyType(override val name: String = "any") : Type(name)

internal data object BoolType : AnyType("bool")
internal data object CharType : AnyType("char")
internal data object CharRangeType : AnyType("charRange")
internal data object StringType : AnyType("string")
internal data object NumberType : AnyType("number")
internal data object NumberRangeType : AnyType("numberRange")
internal data object IndefiniteNumberRangeType : AnyType("indefiniteNumberRange")

internal data object VoidType : Type("")
