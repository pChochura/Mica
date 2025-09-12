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

internal fun Any?.asType(type: Type, looseConversion: Boolean = false): Any? = when (type) {
    AnyType -> this
    is SetType -> asSetType(looseConversion).map {
        it.asType(type.elementType, looseConversion)
    }.toMutableSet()

    is ArrayType -> asArrayType().map {
        it.asType(type.elementType, looseConversion)
    }.toMutableList()

    is MapType -> asMapType().map { (key, value) ->
        key.asType(type.keyType, looseConversion) to value.asType(type.valueType, looseConversion)
    }.toMap().toMutableMap()

    EmptySetType -> asSetType(looseConversion)
    EmptyArrayType -> asArrayType()
    is CustomType, EmptyCustomType -> asCustomType()
    EmptyMapType -> asMapType()
    BoolType -> asBoolType(looseConversion)
    CharType -> asCharType(looseConversion)
    IntType -> asIntType(looseConversion)
    RealType -> asRealType(looseConversion)
    StringType -> asStringType(looseConversion)
    CharRangeType -> asCharRangeType(looseConversion)
    IntRangeType -> asIntRangeType(looseConversion)
    RealRangeType -> asRealRangeType(looseConversion)
    UndefinedType -> throw RuntimeTypeException("Cannot convert Kt${this?.javaClass?.simpleName} to ${type.name}")
}

internal fun Any?.asBoolType(looseConversion: Boolean = false) = when {
    this is Boolean -> this
    this is Long && looseConversion -> this == 1L
    else -> throw RuntimeTypeException("Cannot convert Kt${this?.javaClass?.simpleName} to bool")
}

internal fun Any?.asCharType(looseConversion: Boolean = false) = when {
    this is Char -> this
    this is Long && looseConversion -> Char(this.toInt())
    else -> throw RuntimeTypeException("Cannot convert Kt${this?.javaClass?.simpleName} to char")
}

internal fun Any?.asIntType(looseConversion: Boolean = false) = when {
    this is Long -> this
    this is Boolean && looseConversion -> if (this) 1L else 0L
    this is Char && looseConversion -> this.code.toLong()
    this is Double && looseConversion -> this.toLong()
    else -> throw RuntimeTypeException("Cannot convert Kt${this?.javaClass?.simpleName} to int")
}

internal fun Any?.asRealType(looseConversion: Boolean = false) = when {
    this is Double -> this
    this is Long && looseConversion -> this.toDouble()
    else -> throw RuntimeTypeException("Cannot convert Kt${this?.javaClass?.simpleName} to real")
}

internal fun Any?.asStringType(looseConversion: Boolean = false) = when {
    this is String -> this
    else -> if (looseConversion) {
        this.asString()
    } else {
        throw RuntimeTypeException("Cannot convert Kt${this?.javaClass?.simpleName} to string")
    }
}

internal fun Any?.asCharRangeType(looseConversion: Boolean = false) = when {
    this is CharRange -> this
    this is LongRange && looseConversion -> CharRange(
        start = Char(this.start.toInt()),
        endInclusive = Char(this.endInclusive.toInt()),
    )

    this is ClosedDoubleRange && looseConversion -> CharRange(
        start = Char(this.start.toInt()),
        endInclusive = Char(this.endInclusive.toInt()),
    )

    else -> throw RuntimeTypeException("Cannot convert Kt${this?.javaClass?.simpleName} to charRange")
}

internal fun Any?.asIntRangeType(looseConversion: Boolean = false) = when {
    this is LongRange -> this
    this is CharRange && looseConversion -> LongRange(
        start = this.start.code.toLong(),
        endInclusive = this.endInclusive.code.toLong(),
    )

    this is ClosedDoubleRange && looseConversion -> LongRange(
        start = this.start.toLong(),
        endInclusive = this.endInclusive.toLong(),
    )

    else -> throw RuntimeTypeException("Cannot convert Kt${this?.javaClass?.simpleName} to intRange")
}

internal fun Any?.asRealRangeType(looseConversion: Boolean = false) = when {
    this is ClosedDoubleRange -> this
    this is CharRange && looseConversion -> ClosedDoubleRange(
        start = this.start.code.toDouble(),
        endInclusive = this.endInclusive.code.toDouble(),
    )

    this is LongRange && looseConversion -> ClosedDoubleRange(
        start = this.start.toDouble(),
        endInclusive = this.endInclusive.toDouble(),
    )

    else -> throw RuntimeTypeException("Cannot convert Kt${this?.javaClass?.simpleName} to realRange")
}

internal fun Any?.asSetType(looseConversion: Boolean = false) = when {
    this is Set<*> -> this as MutableSet<*>
    this is List<*> && looseConversion -> this.toMutableSet()
    this is String && looseConversion -> this.toSet().toMutableSet()
    this is CharRange && looseConversion -> this.toMutableSet()
    this is LongRange && looseConversion -> if (first < last) {
        this.toMutableSet()
    } else {
        MutableList((first - last + 1).toInt()) { first - it }.toMutableSet()
    }

    else -> throw RuntimeTypeException("Cannot convert Kt${this?.javaClass?.simpleName} to set")
}

internal fun Any?.asArrayType() = when (this) {
    is List<*> -> this as MutableList<*>
    is Set<*> -> this.toMutableList()
    is String -> this.toMutableList()
    is CharRange -> this.toMutableList()
    is LongRange -> if (first < last) {
        this.toMutableList()
    } else {
        MutableList((first - last + 1).toInt()) { first - it }
    }

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
