package com.pointlessapps.granite.mica.linter.checker

import com.pointlessapps.granite.mica.ast.statements.Statement
import com.pointlessapps.granite.mica.linter.model.Scope

internal sealed class StatementChecker<T : Statement>(protected val scope: Scope) {
    abstract fun check(statement: T)
}
