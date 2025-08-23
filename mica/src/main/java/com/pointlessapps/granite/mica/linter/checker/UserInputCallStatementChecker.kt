package com.pointlessapps.granite.mica.linter.checker

import com.pointlessapps.granite.mica.ast.statements.UserInputCallStatement
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.model.StringType

internal class UserInputCallStatementChecker(scope: Scope) :
    StatementChecker<UserInputCallStatement>(scope) {

    override fun check(statement: UserInputCallStatement) {
        // Check whether the variable is resolvable to String
        statement.checkVariableType()
    }

    private fun UserInputCallStatement.checkVariableType() {
        // TODO check if the variable exists and force the input value to be that type
        val variableType = scope.getVariable(contentToken.value)
        if (variableType == null) {
            scope.declareVariable(
                startingToken = contentToken,
                name = contentToken.value,
                type = StringType,
            )

            return
        }

        if (!variableType.isSubtypeOf(StringType)) {
            scope.addError(
                message = "Type of the variable ${contentToken.value} (${
                    variableType.name
                }) doesn't resolve to a string",
                token = contentToken,
            )
        }
    }
}
