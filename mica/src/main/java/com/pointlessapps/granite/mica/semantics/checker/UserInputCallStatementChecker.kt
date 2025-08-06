package com.pointlessapps.granite.mica.semantics.checker

import com.pointlessapps.granite.mica.ast.statements.UserInputCallStatement
import com.pointlessapps.granite.mica.semantics.model.Scope
import com.pointlessapps.granite.mica.semantics.model.StringType
import com.pointlessapps.granite.mica.semantics.resolver.TypeCoercionResolver.canBeCoercedTo

internal class UserInputCallStatementChecker(
    scope: Scope,
) : StatementChecker<UserInputCallStatement>(scope) {

    override fun check(statement: UserInputCallStatement) {
        // Check whether the variable is resolvable to String
        statement.checkVariableType()
    }

    private fun UserInputCallStatement.checkVariableType() {
        val variable = scope.variables[contentToken.value]
        val variableType = variable?.type
        if (variable == null || variableType == null) {
            scope.addError(
                message = "Variable ${contentToken.value} is not declared",
                token = contentToken,
            )

            return
        }

        if (variableType.canBeCoercedTo(StringType)) {
            scope.addError(
                message = "Variable ${contentToken.value} type doesn't resolve to a String",
                token = contentToken,
            )
        } else if (variableType !is StringType) {
            scope.addWarning(
                message = "Variable ${
                    contentToken.value
                } is not a String. The input will be coerced to a String",
                token = contentToken,
            )
        }
    }
}
