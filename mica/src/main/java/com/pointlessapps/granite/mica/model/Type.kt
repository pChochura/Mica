package com.pointlessapps.granite.mica.model

import com.pointlessapps.granite.mica.runtime.model.CharVariable
import com.pointlessapps.granite.mica.runtime.model.IntVariable
import com.pointlessapps.granite.mica.runtime.model.RealVariable

internal sealed class Type(
    val name: String,
    val parentType: Type?,
) {
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
    override val superTypes: Set<Type>
        get() = buildSet {
            addAll(elementType.superTypes.map(::ArrayType))
            add(EmptyArrayType)
            parentType?.superTypes?.let(::addAll)
        }
}

internal data object EmptyArrayType : ArrayType(AnyType) {
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
}

internal data object EmptyCustomType : CustomType("type")

internal open class SetType(val elementType: Type) :
    Type("{${elementType.name}}", ArrayType(elementType)) {
    override fun valueAsImmediateSupertype(value: Any?): Any? = (value as Set<*>).toMutableList()

    override val superTypes: Set<Type>
        get() = buildSet {
            addAll(elementType.superTypes.map(::SetType))
            add(EmptySetType)
            parentType?.superTypes?.let(::addAll)
        }
}

internal data object EmptySetType : SetType(AnyType) {
    override fun isSubtypeOf(other: Type): Boolean {
        if (other.isSubtypeOf(SetType(AnyType))) return true

        return super.isSubtypeOf(other)
    }
}

/**
 * A type that cannot be constructed.
 * It represents an error state or a function that has no return type.
 */
internal data object UndefinedType : Type("undefined", parentType = null)
