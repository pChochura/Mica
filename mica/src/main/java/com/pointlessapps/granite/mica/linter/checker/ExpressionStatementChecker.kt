package com.pointlessapps.granite.mica.linter.checker

import com.pointlessapps.granite.mica.ast.expressions.AffixAssignmentExpression
import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.IfConditionExpression
import com.pointlessapps.granite.mica.ast.statements.ExpressionStatement
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.model.ScopeType
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.UndefinedType

internal class ExpressionStatementChecker(
    scope: Scope,
    private val typeResolver: TypeResolver,
) : StatementChecker<ExpressionStatement>(scope) {

    override fun check(statement: ExpressionStatement) {
        // Check whether the expression type is resolvable
        statement.checkExpressionType()

        if (statement.expression is IfConditionExpression) {
            statement.expression.checkIfConditionExpression()
        }

        if (statement.expression is AffixAssignmentExpression) {
            typeResolver.resolveExpressionType(statement.expression)
        }
    }

    private fun ExpressionStatement.checkExpressionType() {
        if (expression is AffixAssignmentExpression || expression is IfConditionExpression) {
            return
        }

        val expressionType = typeResolver.resolveExpressionType(expression)
        if (expressionType != UndefinedType) {
            scope.addWarning(
                message = "Unused expression result",
                token = startingToken,
            )
        }
    }

    private fun IfConditionExpression.checkIfConditionExpression() {
        val ifStatementBodies = listOf(ifConditionDeclaration.ifBody.statements) +
                elseIfConditionDeclarations?.map { it.elseIfBody.statements }.orEmpty() +
                elseDeclaration?.elseBody?.statements?.let { listOf(it) }.orEmpty()

        ifStatementBodies.forEach {
            val localScope = Scope(
                scopeType = ScopeType.If(this),
                parent = scope,
            )

            // Check the correctness of the body
            StatementsChecker(localScope).check(it)
            scope.addReports(localScope.reports)
        }

        checkExpressionType()
    }

    private fun IfConditionExpression.checkExpressionType() {
        val flattenExpressions: List<Expression> =
            listOf(ifConditionDeclaration.ifConditionExpression) +
                    elseIfConditionDeclarations?.map { it.elseIfConditionExpression }.orEmpty()

        flattenExpressions.forEach {
            val type = typeResolver.resolveExpressionType(it)
            if (!type.isSubtypeOf(BoolType)) {
                scope.addError(
                    message = "Type of the expression (${type.name}) doesn't resolve to a bool",
                    token = it.startingToken,
                )
            }
        }
    }
}
