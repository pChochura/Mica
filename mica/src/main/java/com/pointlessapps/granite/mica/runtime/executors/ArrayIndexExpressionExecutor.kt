package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.runtime.model.Variable
import com.pointlessapps.granite.mica.runtime.model.Variable.Companion.toVariable

internal object ArrayIndexExpressionExecutor {

    fun execute(arrayValue: Variable<*>, arrayIndex: Variable<*>): Variable<*> =
        (arrayValue.type as ArrayType).elementType
            .toVariable((arrayValue.value as List<*>)[(arrayIndex.value as Long).toInt()])
}
