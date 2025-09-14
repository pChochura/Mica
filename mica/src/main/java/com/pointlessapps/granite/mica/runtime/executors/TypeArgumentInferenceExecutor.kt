package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.helper.commonSupertype
import com.pointlessapps.granite.mica.helper.inferTypeParameter
import com.pointlessapps.granite.mica.helper.isTypeParameter
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.runtime.errors.RuntimeTypeException
import com.pointlessapps.granite.mica.runtime.model.VariableType

internal object TypeArgumentInferenceExecutor {

    fun execute(
        parameterTypes: List<Type>,
        argumentTypes: List<Type>,
    ): VariableType.Type {
        val argumentsToInfer = parameterTypes
            .zip(argumentTypes)
            .filter { it.first.isTypeParameter() }
            .map { it.first.inferTypeParameter(it.second) }
            .onEach { if (it == null) throw RuntimeTypeException("Type argument inference failed") }
            .filterNotNull()

        return VariableType.Type(argumentsToInfer.commonSupertype())
    }
}
