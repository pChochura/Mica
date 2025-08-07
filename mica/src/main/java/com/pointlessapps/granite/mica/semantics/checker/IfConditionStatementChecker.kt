package com.pointlessapps.granite.mica.semantics.checker

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.statements.IfConditionStatement
import com.pointlessapps.granite.mica.semantics.model.BoolType
import com.pointlessapps.granite.mica.semantics.model.Scope
import com.pointlessapps.granite.mica.semantics.model.ScopeType
import com.pointlessapps.granite.mica.semantics.resolver.TypeCoercionResolver.canBeCoercedTo
import com.pointlessapps.granite.mica.semantics.resolver.TypeResolver

internal class IfConditionStatementChecker(
    scope: Scope,
    private val typeResolver: TypeResolver,
) : StatementChecker<IfConditionStatement>(scope) {

    override fun check(statement: IfConditionStatement) {
        // Takes care of the redeclaration
        val ifStatementBodies = listOf(statement.body) +
                statement.elseIfConditionStatements?.map { it.elseIfBody }.orEmpty() +
                statement.elseStatement?.elseBody?.let { listOf(it) }.orEmpty()

        ifStatementBodies.forEach {
            val localScope = Scope(
                scopeType = ScopeType.If(statement),
                parent = scope,
            )

            // Check the correctness of the body
            StatementsChecker(localScope).check(it)
            scope.addReports(localScope.reports)
        }

        // Check whether the expression type is resolvable to Bool
        statement.checkExpressionType()
    }

    private fun IfConditionStatement.checkExpressionType() {
        val flattenExpressions: List<Expression> = listOf(conditionExpression) +
                elseIfConditionStatements?.map { it.elseIfConditionExpression }.orEmpty()

        flattenExpressions.forEach {
            val type = typeResolver.resolveExpressionType(it)
            if (!type.canBeCoercedTo(BoolType)) {
                scope.addError(
                    message = "Expression type doesn't resolve to a Boolean",
                    token = it.startingToken,
                )
            }
        }
    }
}
