package com.pointlessapps.granite.mica.model

import com.pointlessapps.granite.mica.runtime.model.Variable.Companion.toVariable

internal sealed class Type(
    val name: String,
    val parentType: Type?,
) {
    open val superTypes: Set<Type>
        get() = buildSet {
            add(this@Type)
            parentType?.superTypes?.let(::addAll)
        }

    inline fun <reified T : Type> isSubtypeOf(): Boolean = superTypes.any { it is T }

    open fun isSubtypeOf(other: Type): Boolean = superTypes.contains(other)
    open fun isSubtypeOfAny(vararg others: Type): Boolean = others.any { isSubtypeOf(it) }

    protected open fun valueAsImmediateSupertype(value: Any?): Any? = value
    inline fun <reified T : Type> valueAsSupertype(value: Any?): Any? {
        var currentType = this
        var currentValue = value
        while (currentType !is T) {
            currentValue = currentType.valueAsImmediateSupertype(currentValue)
            currentType = currentType.parentType ?: return currentValue
        }

        return currentValue
    }
}

internal data object AnyType : Type("any", null)
internal data object BoolType : Type("bool", AnyType)
internal data object CharType : Type("char", AnyType)
internal data object IntType : Type("int", AnyType)
internal data object RealType : Type("real", AnyType)
internal data object RealRangeType : Type("realRange", AnyType)

internal data object StringType : Type("string", ArrayType(CharType)) {
    override fun valueAsImmediateSupertype(value: Any?): Any? = (value as String).map {
        CharType.toVariable(it)
    }
}

internal data object CharRangeType : Type("charRange", ArrayType(CharType)) {
    override fun valueAsImmediateSupertype(value: Any?): Any? = (value as CharRange).map {
        CharType.toVariable(it)
    }
}

internal data object IntRangeType : Type("intRange", ArrayType(IntType)) {
    override fun valueAsImmediateSupertype(value: Any?): Any? = (value as LongRange).map {
        IntType.toVariable(it)
    }
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

internal object EmptyArrayType : ArrayType(AnyType) {
    override fun isSubtypeOf(other: Type): Boolean {
        if (other.isSubtypeOf(ArrayType(AnyType))) return true

        return super.isSubtypeOf(other)
    }
}

/**
 * A type that cannot be constructed.
 * It represents an error state or a function that has no return type.
 */
internal data object UndefinedType : Type("undefined", parentType = null)
