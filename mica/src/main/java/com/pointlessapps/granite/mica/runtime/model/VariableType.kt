package com.pointlessapps.granite.mica.runtime.model

import com.pointlessapps.granite.mica.model.Type as MicaType

internal sealed interface VariableType {
    @JvmInline
    value class Value(val value: Any?) : VariableType

    @JvmInline
    value class Type(val type: MicaType) : VariableType

    companion object {
        val Undefined = Value(null)
    }
}
