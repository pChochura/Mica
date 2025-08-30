package com.pointlessapps.granite.mica.linter.checker

import com.pointlessapps.granite.mica.ast.statements.LoopIfStatement
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.model.ScopeType
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
import com.pointlessapps.granite.mica.model.BoolType

internal class LoopIfStatementChecker(
    scope: Scope,
    private val typeResolver: TypeResolver,
) : StatementChecker<LoopIfStatement>(scope) {

    override fun check(statement: LoopIfStatement) {
        // Check whether the expression type is resolvable to bool
        statement.checkExpressionType()

        // Check whether the loop is infinite and report a warning if the else declaration is set
        statement.checkUnreachableElseBody()

        listOf(
            statement.loopBody.statements,
            statement.elseDeclaration?.elseBody?.statements,
        ).forEach {
            if (it == null) return@forEach

            val localScope = Scope(
                scopeType = ScopeType.LoopIf(statement),
                parent = scope,
            )

            // Check the correctness of the body
            StatementsChecker(localScope).check(it)
            scope.addReports(localScope.reports)
        }
    }

    private fun LoopIfStatement.checkExpressionType() {
        if (ifConditionExpression == null) {
            return
        }

        val type = typeResolver.resolveExpressionType(ifConditionExpression)
        if (!type.isSubtypeOf(BoolType)) {
            scope.addError(
                message = "Type of the expression (${type.name}) doesn't resolve to a bool",
                token = ifConditionExpression.startingToken,
            )
        }
    }

    private fun LoopIfStatement.checkUnreachableElseBody() {
        if (ifConditionExpression == null && elseDeclaration != null) {
            scope.addWarning(
                message = "The else body in the infinite loop is unreachable",
                token = elseDeclaration.elseToken,
            )
        }
    }
}
