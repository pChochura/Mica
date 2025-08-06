package com.pointlessapps.granite.mica.semantics.checker

import com.pointlessapps.granite.mica.ast.statements.ReturnStatement
import com.pointlessapps.granite.mica.semantics.model.Scope

internal class ReturnStatementChecker(scope: Scope) : StatementChecker<ReturnStatement>(scope) {

    override fun check(statement: ReturnStatement) {
        if (scope.parent == null) {
            scope.addError(
                message = "Root level return statement is not supported",
                token = statement.startingToken,
            )
        }
    }
}
