package com.pointlessapps.granite.mica.semantics.checker

import com.pointlessapps.granite.mica.ast.statements.EmptyStatement
import com.pointlessapps.granite.mica.semantics.model.Scope

internal class EmptyStatementChecker(scope: Scope) : StatementChecker<EmptyStatement>(scope) {

    override fun check(statement: EmptyStatement) {
        scope.addWarning(
            message = "Empty statement",
            token = statement.startingToken,
        )
    }
}
