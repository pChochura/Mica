package com.pointlessapps.granite.mica.model

internal sealed class Type(
    val name: String,
    val parentType: Type?,
) {
    override fun toString() = name
    override fun hashCode() = name.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is Type) return false
        return name == other.name
    }

    open val superTypes: Set<Type>
        get() = buildSet {
            add(this@Type)
            parentType?.superTypes?.let(::addAll)
        }

    open fun isSubtypeOf(other: Type): Boolean = superTypes.contains(other)
    open fun isSubtypeOfAny(vararg others: Type): Boolean = others.any { isSubtypeOf(it) }
}

internal object AnyType : Type("any", null)
internal object BoolType : Type("bool", AnyType)
internal object CharType : Type("char", AnyType)
internal object NumberType : Type("number", AnyType)
internal object IntType : Type("int", NumberType)
internal object RealType : Type("real", NumberType)
internal object RealRangeType : Type("realRange", AnyType)

internal object StringType : Type("string", ArrayType(CharType))
internal object CharRangeType : Type("charRange", ArrayType(CharType))
internal object IntRangeType : Type("intRange", ArrayType(IntType))

internal open class ArrayType(val elementType: Type) : Type("[$elementType]", AnyType) {
    override val superTypes: Set<Type>
        get() = buildSet {
            addAll(elementType.superTypes.map(::ArrayType))
            add(EmptyArrayType)
            parentType?.superTypes?.let(::addAll)
        }
}

internal object EmptyArrayType : ArrayType(AnyType) {
    override fun isSubtypeOf(other: Type): Boolean {
        if (other.isSubtypeOf(ArrayType(AnyType))) return true

        return super.isSubtypeOf(other)
    }
}

internal open class CustomType(name: String) : Type(name, AnyType) {
    override val superTypes: Set<Type>
        get() = buildSet {
            add(this@CustomType)
            add(EmptyCustomType)
            parentType?.superTypes?.let(::addAll)
        }

    companion object {
        const val NAME_PROPERTY = "_name"
    }
}

internal object EmptyCustomType : CustomType("type")

internal open class SetType(val elementType: Type) :
    Type("{$elementType}", ArrayType(elementType)) {
    override val superTypes: Set<Type>
        get() = buildSet {
            addAll(elementType.superTypes.map(::SetType))
            add(EmptySetType)
            parentType?.superTypes?.let(::addAll)
        }
}

internal object EmptySetType : SetType(AnyType) {
    override fun isSubtypeOf(other: Type): Boolean {
        if (other.isSubtypeOf(SetType(AnyType))) return true

        return super.isSubtypeOf(other)
    }
}

internal open class MapType(val keyType: Type, val valueType: Type) :
    Type("{$keyType:$valueType}", AnyType) {
    override val superTypes: Set<Type>
        get() = buildSet {
            keyType.superTypes.forEach { key ->
                valueType.superTypes.forEach { value ->
                    add(MapType(key, value))
                }
            }
            add(EmptyMapType)
            parentType?.superTypes?.let(::addAll)
        }
}

internal object EmptyMapType : MapType(AnyType, AnyType) {
    override fun isSubtypeOf(other: Type): Boolean {
        if (other.isSubtypeOf(MapType(AnyType, AnyType))) return true

        return super.isSubtypeOf(other)
    }
}

internal class GenericType(val boundType: Type) : Type("@$boundType", null) {
    override fun isSubtypeOf(other: Type): Boolean {
        if (other is GenericType) return boundType.isSubtypeOf(other.boundType)

        return false
    }

    companion object {
        const val NAME = "type"
    }
}

/**
 * A type that cannot be constructed.
 * It represents an error state or a function that has no return type.
 */
internal object UndefinedType : Type("undefined", parentType = null)
