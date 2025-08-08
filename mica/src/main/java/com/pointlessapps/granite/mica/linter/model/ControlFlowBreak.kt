package com.pointlessapps.granite.mica.linter.model

internal sealed class ControlFlowBreak {
    data class Return(val value: Any?) : ControlFlowBreak()
    data object Break : ControlFlowBreak()

    // TODO add continue
}