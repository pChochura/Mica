package com.pointlessapps.granite.mica.model

internal sealed class Type(
    val name: String,
    val parentType: Type?,
) {
    open val superTypes: List<Type> = buildList {
        add(this@Type)
        parentType?.superTypes?.let(::addAll)
    }

    open fun isSupertypeOf(other: Type): Boolean = superTypes.any { it == other }
}

internal data object BoolType : Type("bool", AnyType)
internal data object CharType : Type("char", AnyType)
internal data object CharRangeType : Type("charRange", ArrayType(CharType))
internal data object StringType : Type("string", ArrayType(CharType))
internal data object IntType : Type("int", AnyType)
internal data object RealType : Type("real", AnyType)
internal data object IntRangeType : Type("intRange", ArrayType(IntType))
internal data object RealRangeType : Type("realRange", ArrayType(RealType))
internal data object AnyType : Type("any", null)

internal open class ArrayType(val elementType: Type) : Type("[${elementType.name}]", AnyType) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArrayType) return false

        return elementType == other.elementType
    }

    override fun hashCode() = elementType.hashCode()

    override val superTypes: List<Type>
        get() = buildList {
            addAll(elementType.superTypes.map(::ArrayType))
            parentType?.superTypes?.let(::addAll)
        }
}

internal object EmptyArrayType : ArrayType(AnyType) {
    override fun isSupertypeOf(other: Type): Boolean {
        if (other is ArrayType) return true

        return super.isSupertypeOf(other)
    }
}

/**
 * A type that cannot be constructed.
 * It represents an error state or a function that has no return type.
 */
internal data object UndefinedType : Type("undefined", parentType = null)
