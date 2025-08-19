package com.pointlessapps.granite.mica.model

internal sealed class Type(open val name: String)

internal data object BoolType : Type("bool")
internal data object CharType : Type("char")
internal data object CharRangeType : Type("charRange")
internal data object StringType : Type("string")
internal data object IntType : Type("int")
internal data object RealType : Type("real")
internal data object IntRangeType : Type("intRange")
internal data object RealRangeType : Type("realRange")
internal data object AnyType : Type("any")

internal open class ArrayType(val elementType: Type) : Type("[${elementType.name}]") {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArrayType) return false

        return elementType == other.elementType
    }

    override fun hashCode() = elementType.hashCode()
}

internal object EmptyArrayType : ArrayType(AnyType)

/**
 * A type that cannot be constructed.
 * It represents an error state or a function that has no return type.
 */
internal data object UndefinedType : Type("undefined")
