package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.mapper.asArrayType
import com.pointlessapps.granite.mica.mapper.asCustomType
import com.pointlessapps.granite.mica.mapper.asIntType
import com.pointlessapps.granite.mica.mapper.asMapType
import com.pointlessapps.granite.mica.mapper.asStringType
import com.pointlessapps.granite.mica.mapper.asType
import com.pointlessapps.granite.mica.mapper.toType
import com.pointlessapps.granite.mica.model.EmptyArrayType
import com.pointlessapps.granite.mica.model.EmptyCustomType
import com.pointlessapps.granite.mica.model.EmptyMapType
import com.pointlessapps.granite.mica.model.MapType
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.runtime.errors.RuntimeTypeException
import com.pointlessapps.granite.mica.runtime.model.VariableType

internal object AccessorExpressionExecutor {

    private sealed interface Variable {
        @JvmInline
        value class Array(val value: MutableList<*>) : Variable

        @JvmInline
        value class CustomType(val value: MutableMap<*, *>) : Variable

        data class Map(val keyType: Type, val value: MutableMap<*, *>) : Variable
    }

    fun executeGet(variable: Any, accessors: List<Any>): VariableType.Value {
        var currentVariable: Variable = getVariable(variable)
        accessors.subList(0, accessors.size - 1).forEach {
            currentVariable = getVariable(currentVariable.getValue(it))
        }

        return VariableType.Value(currentVariable.getValue(accessors.last()))
    }

    fun executeSet(variable: Any, accessors: List<Any>, value: Any): VariableType.Value {
        var currentVariable: Variable = getVariable(variable)
        accessors.subList(0, accessors.size - 1).forEach {
            currentVariable = getVariable(currentVariable.getValue(it))
        }
        currentVariable.setValue(accessors.last(), value)

        return VariableType.Value(variable)
    }

    private fun getVariable(variable: Any?): Variable {
        val type = variable.toType()
        return when {
            type.isSubtypeOf(EmptyArrayType) -> Variable.Array(variable.asArrayType())
            type.isSubtypeOf(EmptyCustomType) -> Variable.CustomType(variable.asCustomType())
            type.isSubtypeOf(EmptyMapType) -> Variable.Map(
                keyType = type.superTypes.filterIsInstance<MapType>().first().keyType,
                value = variable.asMapType(),
            )
            else -> throw RuntimeTypeException("Cannot access variable of type ${type.name}")
        }
    }

    private fun Variable.getValue(accessor: Any) = when (this) {
        is Variable.Array -> value[accessor.asIntType().toInt()]
        is Variable.CustomType -> value[accessor.asStringType()]
        is Variable.Map -> value[accessor.asType(keyType)]
    }

    @Suppress("UNCHECKED_CAST")
    private fun Variable.setValue(accessor: Any, value: Any) {
        when (this) {
            is Variable.Array -> (this.value.asArrayType() as MutableList<Any?>).set(
                index = accessor.asIntType().toInt(),
                element = value,
            )

            is Variable.CustomType -> (this.value.asCustomType() as MutableMap<String, Any?>).set(
                key = accessor.asStringType(),
                value = value,
            )

            is Variable.Map -> (this.value.asMapType() as MutableMap<Any?, Any?>).set(
                key = accessor.asType(keyType),
                value = value,
            )
        }
    }
}
