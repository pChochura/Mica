package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.runtime.model.VariableType

internal object MapLiteralExpressionExecutor {

    fun execute(elements: List<Pair<Any, Any>>) = VariableType.Value(elements.toMap().toMutableMap())
}
