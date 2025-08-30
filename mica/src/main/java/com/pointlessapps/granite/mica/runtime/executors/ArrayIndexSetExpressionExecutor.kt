package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.runtime.errors.RuntimeTypeException
import com.pointlessapps.granite.mica.runtime.model.Variable
import com.pointlessapps.granite.mica.runtime.model.Variable.Companion.toVariable

internal object ArrayIndexSetExpressionExecutor {

    @Suppress("UNCHECKED_CAST")
    fun execute(
        arrayValue: Variable<*>,
        arrayIndices: List<Variable<*>>,
        value: Variable<*>,
    ): Variable<*> {
        val indices = arrayIndices.map {
            (it.type.valueAsSupertype<IntType>(it.value) as Long).toInt()
        }

        var element = arrayValue
        indices.subList(0, indices.size - 1).forEach { index ->
            element = when (element.type) {
                is ArrayType -> (element.value as List<*>)[index] as Variable<*>
                else -> throw RuntimeTypeException("Invalid array type: ${arrayValue.type.name}")
            }
        }

        when (element.type) {
            is ArrayType -> (element.value as MutableList<Variable<*>>).set(
                index = indices.last(),
                element = element.type.elementType.toVariable(
                    value.type.valueAsSupertype(value.value, element.type.elementType),
                ),
            )

            else -> throw RuntimeTypeException("Invalid array type: ${arrayValue.type.name}")
        }

        return arrayValue
    }
}
