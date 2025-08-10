package com.pointlessapps.granite.mica.linter.checker

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.statements.IfConditionStatement
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.model.ScopeType
import com.pointlessapps.granite.mica.linter.resolver.TypeCoercionResolver.canBeCoercedTo
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
import com.pointlessapps.granite.mica.model.BoolType

internal class IfConditionStatementChecker(
    scope: Scope,
    private val typeResolver: TypeResolver,
) : StatementChecker<IfConditionStatement>(scope) {

    override fun check(statement: IfConditionStatement) {
        // Check whether the expression type is resolvable to bool
        statement.checkExpressionType()

        val ifStatementBodies = listOf(statement.ifConditionDeclaration.ifBody) +
                statement.elseIfConditionDeclarations?.map { it.elseIfBody }.orEmpty() +
                statement.elseDeclaration?.elseBody?.let { listOf(it) }.orEmpty()

        ifStatementBodies.forEach {
            val localScope = Scope(
                scopeType = ScopeType.If(statement),
                parent = scope,
            )

            // Check the correctness of the body
            StatementsChecker(localScope).check(it)
            scope.addReports(localScope.reports)
        }
    }

    private fun IfConditionStatement.checkExpressionType() {
        val flattenExpressions: List<Expression> =
            listOf(ifConditionDeclaration.ifConditionExpression) +
                    elseIfConditionDeclarations?.map { it.elseIfConditionExpression }.orEmpty()

        flattenExpressions.forEach {
            val type = typeResolver.resolveExpressionType(it)
            if (!type.canBeCoercedTo(BoolType)) {
                scope.addError(
                    message = "Type of the expression (${type.name}) doesn't resolve to a bool",
                    token = it.startingToken,
                )
            }
        }
    }
}
