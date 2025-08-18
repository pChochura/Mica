package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.linter.resolver.TypeCoercionResolver.canBeCoercedTo
import com.pointlessapps.granite.mica.linter.resolver.TypeCoercionResolver.resolveElementTypeCoercedToArray
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.NumberType
import com.pointlessapps.granite.mica.runtime.model.Variable
import com.pointlessapps.granite.mica.runtime.model.Variable.Companion.toVariable
import com.pointlessapps.granite.mica.runtime.resolver.ValueCoercionResolver.coerceToType

internal object ArrayIndexExpressionExecutor {

    fun execute(arrayValue: Variable<*>, arrayIndex: Variable<*>): Variable<*> {
        val elementType = arrayValue.type.resolveElementTypeCoercedToArray()
        val arrayValue = arrayValue.value?.coerceToType(
            originalType = arrayValue.type,
            targetType = ArrayType(elementType),
        ) as List<Any>

        if (arrayIndex.type.canBeCoercedTo(ArrayType(NumberType))) {
            // Return the list consisting only of the provided indices
            val indices = arrayIndex.value
                ?.coerceToType(arrayIndex.type, ArrayType(NumberType)) as List<Double>
            return ArrayType(elementType).toVariable(indices.map { arrayValue[it.toInt()] })
        }

        val index = (arrayIndex.value?.coerceToType(arrayIndex.type, NumberType) as Double).toInt()
        return elementType.toVariable(arrayValue[index])
    }
}
