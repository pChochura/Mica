package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.runtime.model.Variable

internal object ArrayIndexGetExpressionExecutor {

    fun execute(arrayValue: Variable<*>, arrayIndices: List<Variable<*>>): Variable<*> {
        var array = arrayValue.type.valueAsSupertype<ArrayType>(arrayValue.value) as List<*>
        arrayIndices.subList(0, arrayIndices.size - 1).map {
            (it.type.valueAsSupertype<IntType>(it.value) as Long).toInt()
        }.forEach { index ->
            val element = array[index] as Variable<*>
            array = element.type.valueAsSupertype<ArrayType>(element.value) as List<*>
        }

        val lastIndex = arrayIndices.last()
        val index = (lastIndex.type.valueAsSupertype<IntType>(lastIndex.value) as Long).toInt()
        return array[index] as Variable<*>
    }
}
