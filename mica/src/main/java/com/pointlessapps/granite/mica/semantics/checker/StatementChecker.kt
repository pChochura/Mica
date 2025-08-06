package com.pointlessapps.granite.mica.semantics.checker

import com.pointlessapps.granite.mica.ast.statements.Statement
import com.pointlessapps.granite.mica.semantics.model.CheckRapport

internal interface StatementChecker<T : Statement> {
    fun check(statement: T): CheckRapport
}
