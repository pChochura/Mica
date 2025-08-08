package com.pointlessapps.granite.mica.runtime.resolver

import com.pointlessapps.granite.mica.model.AnyType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.CharRangeType
import com.pointlessapps.granite.mica.model.CharType
import com.pointlessapps.granite.mica.model.ClosedDoubleRange
import com.pointlessapps.granite.mica.model.IndefiniteNumberRangeType
import com.pointlessapps.granite.mica.model.NumberRangeType
import com.pointlessapps.granite.mica.model.NumberType
import com.pointlessapps.granite.mica.model.OpenEndDoubleRange
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.model.UndefinedType
import com.pointlessapps.granite.mica.runtime.errors.RuntimeTypeException

internal object ValueCoercionResolver {

    fun Any.coerceToType(originalType: Type, targetType: Type): Any = when (originalType) {
        BoolType -> coerceFromBool(targetType)
        CharType -> coerceFromChar(targetType)
        CharRangeType -> coerceFromCharRange(targetType)
        StringType -> coerceFromString(targetType)
        NumberType -> coerceFromNumber(targetType)
        NumberRangeType -> coerceFromNumberRange(targetType)
        IndefiniteNumberRangeType -> coerceFromIndefiniteNumberRange(targetType)
        AnyType -> coerceFromAny(targetType)
        UndefinedType -> throw RuntimeTypeException("Undefined type cannot be used")
    }

    private fun Any.coerceFromBool(targetType: Type): Any = when (targetType) {
        BoolType -> this as Boolean
        NumberType -> if (this as Boolean) 1f else 0f
        StringType -> if (this as Boolean) "true" else "false"
        CharType -> if (this as Boolean) '1' else '0'
        AnyType -> this
        else -> throw RuntimeTypeException("Bool cannot be coerced to ${targetType.name}")
    }

    private fun Any.coerceFromChar(targetType: Type): Any = when (targetType) {
        CharType -> this as Char
        NumberType -> (this as Char).code.toDouble()
        StringType -> (this as Char).toString()
        BoolType -> this as Char != '0'
        AnyType -> this
        else -> throw RuntimeTypeException("Char cannot be coerced to ${targetType.name}")
    }

    private fun Any.coerceFromCharRange(targetType: Type): Any = when (targetType) {
        CharRangeType -> this as CharRange
        NumberRangeType -> (this as CharRange).let {
            ClosedDoubleRange(it.first.code.toDouble(), it.last.code.toDouble())
        }

        AnyType -> this
        else -> throw RuntimeTypeException("CharRange cannot be coerced to ${targetType.name}")
    }

    private fun Any.coerceFromString(targetType: Type): Any = when (targetType) {
        StringType -> this as String
        AnyType -> this
        else -> throw RuntimeTypeException("String cannot be coerced to ${targetType.name}")
    }

    private fun Any.coerceFromNumber(targetType: Type): Any = when (targetType) {
        NumberType -> this as Double
        StringType -> (this as Double).toString()
        CharType -> Char((this as Double).toInt())
        BoolType -> this as Double != 0.0
        AnyType -> this
        else -> throw RuntimeTypeException("Number cannot be coerced to ${targetType.name}")
    }

    private fun Any.coerceFromNumberRange(targetType: Type): Any = when (targetType) {
        NumberRangeType -> this as ClosedDoubleRange
        CharRangeType -> (this as ClosedDoubleRange).let {
            CharRange(Char(it.start.toInt()), Char(it.endInclusive.toInt()))
        }

        AnyType -> this
        else -> throw RuntimeTypeException("NumberRange cannot be coerced to ${targetType.name}")
    }

    private fun Any.coerceFromIndefiniteNumberRange(targetType: Type): Any = when (targetType) {
        IndefiniteNumberRangeType -> this as OpenEndDoubleRange
        AnyType -> this
        else -> throw RuntimeTypeException("IndefiniteNumberRange cannot be coerced to ${targetType.name}")
    }

    private fun Any.coerceFromAny(targetType: Type): Any = when (targetType) {
        AnyType -> this
        else -> throw RuntimeTypeException("Any cannot be coerced to ${targetType.name}")
    }
}
