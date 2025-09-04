package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.mapper.asArrayType
import com.pointlessapps.granite.mica.mapper.asIntType
import com.pointlessapps.granite.mica.runtime.model.VariableType

internal object ArrayIndexSetExpressionExecutor {

    fun execute(
        arrayValue: Any,
        arrayIndices: List<Any>,
        value: Any,
    ): VariableType.Value {
        val indices = arrayIndices.map { it.asIntType().toInt() }

        var element: Any? = arrayValue
        indices.subList(0, indices.size - 1).forEach { index ->
            element = element.asArrayType()[index]
        }

        (element.asArrayType() as MutableList<Any?>).set(
            index = indices.last(),
            element = value,
        )

        return VariableType.Value(arrayValue)
    }
}
