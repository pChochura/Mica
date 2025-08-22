package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.runtime.model.Variable

internal object ArrayIndexExpressionExecutor {

    fun execute(arrayValue: Variable<*>, arrayIndex: Variable<*>): Variable<*> {
        val index = (arrayIndex.type.valueAsSupertype<IntType>(arrayIndex.value) as Long).toInt()
        val arraySupertype = arrayValue.type.superTypes.filterIsInstance<ArrayType>().firstOrNull()
        if (arraySupertype == null || arrayValue.value == null) {
            throw IllegalStateException("Unsupported type: ${arrayValue.type}")
        }

        val item = (arrayValue.type.valueAsSupertype<ArrayType>(arrayValue.value) as List<*>)[index]
        return item as Variable<*>
    }
}
