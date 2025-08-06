package com.pointlessapps.granite.mica.semantics.checker

import com.pointlessapps.granite.mica.ast.statements.Statement
import com.pointlessapps.granite.mica.semantics.model.Scope

internal abstract class StatementChecker<T : Statement>(protected val scope: Scope) {
    abstract fun check(statement: T)
}
