package com.pointlessapps.granite.mica.mapper

import com.pointlessapps.granite.mica.helper.commonSupertype
import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.CharRangeType
import com.pointlessapps.granite.mica.model.CharType
import com.pointlessapps.granite.mica.model.ClosedDoubleRange
import com.pointlessapps.granite.mica.model.CustomType
import com.pointlessapps.granite.mica.model.EmptyArrayType
import com.pointlessapps.granite.mica.model.EmptyCustomType
import com.pointlessapps.granite.mica.model.EmptyMapType
import com.pointlessapps.granite.mica.model.EmptySetType
import com.pointlessapps.granite.mica.model.IntRangeType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.model.MapType
import com.pointlessapps.granite.mica.model.RealRangeType
import com.pointlessapps.granite.mica.model.RealType
import com.pointlessapps.granite.mica.model.SetType
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.model.UndefinedType
import com.pointlessapps.granite.mica.runtime.errors.RuntimeTypeException

internal fun Any?.toType(): Type = when (this) {
    is Boolean -> BoolType
    is Char -> CharType
    is Long -> IntType
    is Double -> RealType
    is String -> StringType
    is CharRange -> CharRangeType
    is LongRange -> IntRangeType
    is ClosedDoubleRange -> RealRangeType

    is Set<*> -> if (isEmpty()) {
        EmptySetType
    } else {
        SetType(map(Any?::toType).commonSupertype())
    }

    is List<*> -> if (isEmpty()) {
        EmptyArrayType
    } else {
        ArrayType(map(Any?::toType).commonSupertype())
    }

    is Map<*, *> -> if (this.isEmpty()) {
        EmptyMapType
    } else if (this.contains(CustomType.NAME_PROPERTY)) {
        CustomType(this[CustomType.NAME_PROPERTY].asStringType())
    } else {
        val keyTypes = keys.map(Any?::toType)
        val valueTypes = values.map(Any?::toType)
        MapType(keyTypes.commonSupertype(), valueTypes.commonSupertype())
    }

    is Any -> AnyType
    else -> UndefinedType
}

internal fun Any?.asType(type: Type) = when (type) {
    AnyType -> this
    is SetType, EmptySetType -> asSetType()
    is ArrayType, EmptyArrayType -> asArrayType()
    is CustomType, EmptyCustomType -> asCustomType()
    is MapType, EmptyMapType -> asMapType()
    BoolType -> asBoolType()
    CharType -> asCharType()
    IntType -> asIntType()
    RealType -> asRealType()
    StringType -> asStringType()
    CharRangeType -> asCharRangeType()
    IntRangeType -> asIntRangeType()
    RealRangeType -> asRealRangeType()
    UndefinedType -> throw RuntimeTypeException("Cannot convert Kt${this?.javaClass?.simpleName} to ${type.name}")
}

internal fun Any?.asBoolType() = when (this) {
    is Boolean -> this
    else -> throw RuntimeTypeException("Cannot convert Kt${this?.javaClass?.simpleName} to bool")
}

internal fun Any?.asCharType() = when (this) {
    is Char -> this
    else -> throw RuntimeTypeException("Cannot convert Kt${this?.javaClass?.simpleName} to char")
}

internal fun Any?.asIntType() = when (this) {
    is Long -> this
    else -> throw RuntimeTypeException("Cannot convert Kt${this?.javaClass?.simpleName} to int")
}

internal fun Any?.asRealType() = when (this) {
    is Double -> this
    else -> throw RuntimeTypeException("Cannot convert Kt${this?.javaClass?.simpleName} to real")
}

internal fun Any?.asStringType() = when (this) {
    is String -> this
    else -> throw RuntimeTypeException("Cannot convert Kt${this?.javaClass?.simpleName} to string")
}

internal fun Any?.asCharRangeType() = when (this) {
    is CharRange -> this
    else -> throw RuntimeTypeException("Cannot convert Kt${this?.javaClass?.simpleName} to charRange")
}

internal fun Any?.asIntRangeType() = when (this) {
    is LongRange -> this
    else -> throw RuntimeTypeException("Cannot convert Kt${this?.javaClass?.simpleName} to intRange")
}

internal fun Any?.asRealRangeType() = when (this) {
    is ClosedDoubleRange -> this
    else -> throw RuntimeTypeException("Cannot convert Kt${this?.javaClass?.simpleName} to realRange")
}

internal fun Any?.asSetType() = when (this) {
    is Set<*> -> this as MutableSet<*>
    else -> throw RuntimeTypeException("Cannot convert Kt${this?.javaClass?.simpleName} to set")
}

internal fun Any?.asArrayType() = when (this) {
    is List<*> -> this as MutableList<*>
    is Set<*> -> this.toMutableList()
    is String -> this.toMutableList()
    is CharRange -> this.toMutableList()
    is LongRange -> if (first < last) this.toMutableList() else MutableList((first - last + 1).toInt()) { first - it }
    else -> throw RuntimeTypeException("Cannot convert Kt${this?.javaClass?.simpleName} to array")
}

@Suppress("UNCHECKED_CAST")
internal fun Any?.asCustomType() = when (this) {
    is Map<*, *> -> this as MutableMap<String, *>
    else -> throw RuntimeTypeException("Cannot convert Kt${this?.javaClass?.simpleName} to custom type")
}

internal fun Any?.asMapType() = when (this) {
    is Map<*, *> -> this as MutableMap<*, *>
    else -> throw RuntimeTypeException("Cannot convert Kt${this?.javaClass?.simpleName} to map")
}
