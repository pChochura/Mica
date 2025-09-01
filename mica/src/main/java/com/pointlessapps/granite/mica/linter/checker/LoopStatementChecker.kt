package com.pointlessapps.granite.mica.linter.checker

import com.pointlessapps.granite.mica.ast.statements.LoopIfStatement
import com.pointlessapps.granite.mica.ast.statements.LoopInStatement
import com.pointlessapps.granite.mica.ast.statements.LoopStatement
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.model.ScopeType
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.EmptyArrayType

internal class LoopStatementChecker(
    scope: Scope,
    private val typeResolver: TypeResolver,
) : StatementChecker<LoopStatement>(scope) {

    override fun check(statement: LoopStatement) {
        // Check whether the expression type is resolvable to bool
        statement.checkExpressionType()

        // Check whether the loop is infinite and report a warning if the else declaration is set
        statement.checkUnreachableElseBody()

        // Check whether the expression for the LoopInStatement is an array
        statement.checkArrayExpression()

        listOf(
            statement.loopBody.statements,
            (statement as? LoopIfStatement)?.elseDeclaration?.elseBody?.statements,
        ).forEach {
            if (it == null) return@forEach

            val localScope = Scope(
                scopeType = ScopeType.Loop(statement),
                parent = scope,
            )

            if (statement is LoopInStatement) {
                val arrayType = typeResolver.resolveExpressionType(statement.arrayExpression)
                val elementType = arrayType.superTypes.filterIsInstance<ArrayType>()
                    .firstOrNull()?.elementType

                if (elementType != null) {
                    localScope.declareVariable(
                        name = statement.symbolToken.value,
                        startingToken = statement.symbolToken,
                        type = elementType,
                    )
                }
            }

            // Check the correctness of the body
            StatementsChecker(localScope).check(it)
            scope.addReports(localScope.reports)
        }
    }

    private fun LoopStatement.checkExpressionType() {
        if (this !is LoopIfStatement || ifConditionExpression == null) {
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

    private fun LoopStatement.checkUnreachableElseBody() {
        if (this is LoopIfStatement && ifConditionExpression == null && elseDeclaration != null) {
            scope.addWarning(
                message = "The else body in the infinite loop is unreachable",
                token = elseDeclaration.elseToken,
            )
        }
    }

    private fun LoopStatement.checkArrayExpression() {
        if (this !is LoopInStatement) {
            return
        }

        val type = typeResolver.resolveExpressionType(arrayExpression)
        if (!type.isSubtypeOf(EmptyArrayType)) {
            scope.addError(
                message = "Type of the expression (${type.name}) doesn't resolve to an array",
                token = arrayExpression.startingToken,
            )
        }
    }
}
