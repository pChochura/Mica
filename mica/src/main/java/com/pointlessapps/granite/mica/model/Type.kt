package com.pointlessapps.granite.mica.model

internal sealed class Type(open val name: String)

internal data object BoolType : Type("bool")
internal data object CharType : Type("char")
internal data object CharRangeType : Type("charRange")
internal data object StringType : Type("string")
internal data object NumberType : Type("number")
internal data object NumberRangeType : Type("numberRange")
internal data object IndefiniteNumberRangeType : Type("indefiniteNumberRange")
internal data object AnyType : Type("any")

internal open class ArrayType(val elementType: Type) : Type("[${elementType.name}]")
internal object EmptyArrayType : ArrayType(AnyType)

/**
 * A type that cannot be constructed.
 * It represents an error state or a function that has no return type.
 */
internal data object UndefinedType : Type("undefined")
