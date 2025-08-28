package com.pointlessapps.granite.mica.model

import com.pointlessapps.granite.mica.runtime.model.CharVariable
import com.pointlessapps.granite.mica.runtime.model.IntVariable

internal sealed class Type(
    val name: String,
    val parentType: Type?,
) {
    open val superTypes: Set<Type>
        get() = buildSet {
            add(this@Type)
            parentType?.superTypes?.let(::addAll)
        }

    // TODO look for the supertype by name
    inline fun <reified T : Type> isSubtypeOf(): Boolean = superTypes.any { it is T }

    open fun isSubtypeOf(other: Type): Boolean = superTypes.contains(other)
    open fun isSubtypeOfAny(vararg others: Type): Boolean = others.any { isSubtypeOf(it) }

    inline fun <reified T : Type> valueAsSupertype(value: Any?) =
        valueAsSupertype(value) { it !is T }

    fun valueAsSupertype(value: Any?, supertype: Type) =
        valueAsSupertype(value) { !it::class.isInstance(supertype) }

    private fun valueAsSupertype(value: Any?, condition: (Type) -> Boolean): Any? {
        var currentType = this
        var currentValue = value
        while (condition(currentType)) {
            currentValue = currentType.valueAsImmediateSupertype(currentValue)
            currentType = currentType.parentType ?: return currentValue
        }

        return currentValue
    }

    // TODO consider deep transformation (elements as well for the ArrayType)
    protected open fun valueAsImmediateSupertype(value: Any?): Any? = value
}

internal data object AnyType : Type("any", null)
internal data object BoolType : Type("bool", AnyType)
internal data object CharType : Type("char", AnyType)
internal data object IntType : Type("int", AnyType)
internal data object RealType : Type("real", AnyType)
internal data object RealRangeType : Type("realRange", AnyType)

internal data object StringType : Type("string", ArrayType(CharType)) {
    override fun valueAsImmediateSupertype(value: Any?): Any? = (value as String)
        .map(::CharVariable)
        .toMutableList()
}

internal data object CharRangeType : Type("charRange", ArrayType(CharType)) {
    override fun valueAsImmediateSupertype(value: Any?): Any? = (value as CharRange)
        .map(::CharVariable)
        .toMutableList()
}

internal data object IntRangeType : Type("intRange", ArrayType(IntType)) {
    override fun valueAsImmediateSupertype(value: Any?): Any? = (value as LongRange)
        .map(::IntVariable)
        .toMutableList()
}

internal open class ArrayType(val elementType: Type) : Type("[${elementType.name}]", AnyType) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArrayType) return false

        return elementType == other.elementType
    }

    override fun hashCode() = elementType.hashCode()

    override val superTypes: Set<Type>
        get() = buildSet {
            addAll(elementType.superTypes.map(::ArrayType))
            parentType?.superTypes?.let(::addAll)
        }
}

internal data object EmptyArrayType : ArrayType(AnyType) {
    override fun isSubtypeOf(other: Type): Boolean {
        if (other.isSubtypeOf(ArrayType(AnyType))) return true

        return super.isSubtypeOf(other)
    }
}

internal class CustomType(name: String) : Type(name, AnyType) {
    override fun isSubtypeOf(other: Type): Boolean {
        if (other is CustomType && other.name == name) return true

        return super.isSubtypeOf(other)
    }
}

/**
 * A type that cannot be constructed.
 * It represents an error state or a function that has no return type.
 */
internal data object UndefinedType : Type("undefined", parentType = null)
