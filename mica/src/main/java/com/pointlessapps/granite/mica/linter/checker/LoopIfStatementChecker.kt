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

        // TODO check for infinite loops without break and return statements

        val loopIfStatementBodies = listOf(
            statement.ifConditionDeclaration.ifBody,
            statement.elseDeclaration?.elseBody,
        )

        loopIfStatementBodies.forEach {
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
        val type = typeResolver.resolveExpressionType(ifConditionDeclaration.ifConditionExpression)
        if (type != BoolType) {
            scope.addError(
                message = "Type of the expression (${type.name}) doesn't resolve to a bool",
                token = ifConditionDeclaration.ifConditionExpression.startingToken,
            )
        }
    }
}
