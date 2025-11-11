package com.pointlessapps.granite.mica.linter.checker

import com.pointlessapps.granite.mica.ast.statements.BreakStatement
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.model.ScopeType

internal class BreakStatementChecker(scope: Scope) : StatementChecker<BreakStatement>(scope) {

    override fun check(statement: BreakStatement) {
        // Traverse the parents until we find a loop if scope
        statement.checkParentLoopIfScope()
    }

    private fun BreakStatement.checkParentLoopIfScope() {
        var currentScope: Scope? = scope
        while (currentScope != null && currentScope.scopeType !is ScopeType.Loop) {
            currentScope = currentScope.parent
        }

        if (currentScope == null) {
            scope.addError(
                message = "Break statement is not inside of a loop",
                token = startingToken,
            )
        }
    }
}
