package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.builtins.builtinTypeProperties
import com.pointlessapps.granite.mica.builtins.properties.BuiltinTypePropertyDeclaration
import com.pointlessapps.granite.mica.helper.getMatchingTypeDeclaration
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
import com.pointlessapps.granite.mica.runtime.errors.RuntimeTypeException
import com.pointlessapps.granite.mica.runtime.model.VariableType

internal object AccessorExpressionExecutor {

    private data class Variable(val value: Any?, val type: Type) {
        enum class Type { Array, CustomType, Map, Other }
    }

    private val typeProperties = builtinTypeProperties.groupingBy { it.receiverType }
        .aggregate { _, acc: MutableMap<String, BuiltinTypePropertyDeclaration>?, element, first ->
            if (first) {
                mutableMapOf(element.name to element)
            } else {
                requireNotNull(acc).apply { put(element.name, element) }
            }
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
            type.isSubtypeOf(EmptyArrayType) -> Variable(variable, Variable.Type.Array)
            type.isSubtypeOf(EmptyCustomType) -> Variable(variable, Variable.Type.CustomType)
            type.isSubtypeOf(EmptyMapType) -> Variable(variable, Variable.Type.Map)
            else -> Variable(variable, Variable.Type.Other)
        }
    }

    private fun Variable.getValue(accessor: Any): Any? {
        if (accessor is String) {
            val propertyName = accessor.asStringType()
            typeProperties.getMatchingTypeDeclaration(value.toType(), propertyName)?.let {
                return it.execute(value)
            }
        }

        return when (type) {
            Variable.Type.Array -> value.asArrayType()[accessor.asIntType().toInt()]
            Variable.Type.CustomType -> value.asCustomType()[accessor.asStringType()]
            Variable.Type.Map -> value.asMapType()[
                accessor.asType(value.toType().superTypes.filterIsInstance<MapType>().first().keyType),
            ]

            Variable.Type.Other -> throw RuntimeTypeException(
                "Cannot get the value of type ${value.toType()}[Kt${accessor.javaClass.simpleName}]",
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun Variable.setValue(accessor: Any, value: Any) {
        when (this.type) {
            Variable.Type.Array -> (this.value.asArrayType() as MutableList<Any?>).set(
                index = accessor.asIntType().toInt(),
                element = value,
            )

            Variable.Type.CustomType -> (this.value.asCustomType() as MutableMap<String, Any?>).set(
                key = accessor.asStringType(),
                value = value,
            )

            Variable.Type.Map -> (this.value.asMapType() as MutableMap<Any?, Any?>).set(
                key = accessor.asType(
                    this.value.toType().superTypes.filterIsInstance<MapType>().first().keyType,
                ),
                value = value,
            )

            Variable.Type.Other -> throw RuntimeTypeException(
                "Cannot set the value of type $${this.value.toType()}[Kt${accessor.javaClass.simpleName}]",
            )
        }
    }
}
