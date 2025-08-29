package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.helper.commonSupertype
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.EmptyArrayType
import com.pointlessapps.granite.mica.runtime.model.Variable
import com.pointlessapps.granite.mica.runtime.model.Variable.Companion.toVariable

internal object ArrayLiteralExpressionExecutor {

    fun execute(elements: List<Variable<*>>): Variable<*> {
        if (elements.isEmpty()) return EmptyArrayType.toVariable(mutableListOf<Any>())
        val commonSupertype = elements.map(Variable<*>::type).commonSupertype()

        return ArrayType(commonSupertype).toVariable(elements.toMutableList())
    }
}
