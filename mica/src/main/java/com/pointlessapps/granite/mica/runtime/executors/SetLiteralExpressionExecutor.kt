package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.helper.commonSupertype
import com.pointlessapps.granite.mica.model.EmptySetType
import com.pointlessapps.granite.mica.model.SetType
import com.pointlessapps.granite.mica.runtime.model.Variable
import com.pointlessapps.granite.mica.runtime.model.Variable.Companion.toVariable

internal object SetLiteralExpressionExecutor {

    fun execute(elements: List<Variable<*>>): Variable<*> {
        if (elements.isEmpty()) return EmptySetType.toVariable(mutableSetOf<Any>())
        val commonSupertype = elements.map(Variable<*>::type).commonSupertype()

        return SetType(commonSupertype).toVariable(elements.toMutableSet())
    }
}
