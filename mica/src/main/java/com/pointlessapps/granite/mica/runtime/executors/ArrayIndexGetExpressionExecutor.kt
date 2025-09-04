package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.mapper.asArrayType
import com.pointlessapps.granite.mica.mapper.asIntType
import com.pointlessapps.granite.mica.runtime.model.VariableType

internal object ArrayIndexGetExpressionExecutor {

    fun execute(arrayValue: Any, arrayIndices: List<Any>): VariableType.Value {
        var array = arrayValue.asArrayType()
        arrayIndices.subList(0, arrayIndices.size - 1)
            .map { it.asIntType().toInt() }
            .forEach { array = array[it].asArrayType() }

        val lastIndex = arrayIndices.last()
        val index = lastIndex.asIntType().toInt()
        return VariableType.Value(array[index])
    }
}
