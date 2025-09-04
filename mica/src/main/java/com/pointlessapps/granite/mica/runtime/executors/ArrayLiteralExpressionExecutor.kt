package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.runtime.model.VariableType

internal object ArrayLiteralExpressionExecutor {

    fun execute(elements: List<Any>) = VariableType.Value(elements.toMutableList())
}
