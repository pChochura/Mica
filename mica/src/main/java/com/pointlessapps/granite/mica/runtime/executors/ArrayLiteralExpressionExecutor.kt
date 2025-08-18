package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.linter.resolver.TypeCoercionResolver.resolveCommonBaseType
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.runtime.model.Variable
import com.pointlessapps.granite.mica.runtime.model.Variable.Companion.toVariable
import com.pointlessapps.granite.mica.runtime.resolver.ValueCoercionResolver.coerceToType

internal object ArrayLiteralExpressionExecutor {

    fun execute(elements: List<Variable<*>>): Variable<*> {
        val elementType = elements.map(Variable<*>::type).resolveCommonBaseType()
        return ArrayType(elementType).toVariable(
            elements.map { it.value?.coerceToType(it.type, elementType) },
        )
    }
}
