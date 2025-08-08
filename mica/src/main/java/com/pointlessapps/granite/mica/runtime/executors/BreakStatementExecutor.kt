package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.linter.model.ControlFlowBreak
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.model.ScopeType

internal object BreakStatementExecutor {

    fun execute(scope: Scope) {
        // Find a loop scope
        var currentScope = scope
        while (currentScope.scopeType !is ScopeType.LoopIf) {
            // It was checked by the linter whether the break statement
            // exist in a loop scope
            currentScope = requireNotNull(currentScope.parent)
        }

        currentScope.controlFlowBreakValue = ControlFlowBreak.Break
    }
}
