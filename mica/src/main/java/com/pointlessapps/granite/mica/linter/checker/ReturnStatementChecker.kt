package com.pointlessapps.granite.mica.linter.checker

import com.pointlessapps.granite.mica.ast.statements.ReturnStatement
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.model.ScopeType
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
import com.pointlessapps.granite.mica.model.UndefinedType

internal class ReturnStatementChecker(
    scope: Scope,
    private val typeResolver: TypeResolver,
) : StatementChecker<ReturnStatement>(scope) {

    override fun check(statement: ReturnStatement) {
        // Traverse the parents until we find a function scope
        // and then check for the type of the return statement
        statement.checkParentFunctionScopeReturnType()
    }

    private fun ReturnStatement.checkParentFunctionScopeReturnType() {
        var currentScope: Scope? = scope
        while (currentScope != null && currentScope.scopeType !is ScopeType.Function) {
            currentScope = currentScope.parent
        }

        if (currentScope?.scopeType !is ScopeType.Function) {
            scope.addError(
                message = "Return statement is not inside of a function",
                token = startingToken,
            )

            return
        }

        val functionScope = currentScope.scopeType
        val functionDeclarationStatement = functionScope.statement
        val returnType = functionDeclarationStatement.returnTypeExpression
            ?.let(typeResolver::resolveExpressionType)

        if (returnType == null) {
            if (returnExpression != null) {
                scope.addWarning(
                    message = "Unused return value",
                    token = returnExpression.startingToken,
                )
            }

            return
        }

        if (returnExpression == null) {
            currentScope.addError(
                message = "Missing return value",
                token = startingToken,
            )
        } else if (returnType !is UndefinedType) {
            val resolvedType = typeResolver.resolveExpressionType(returnExpression)
            if (!resolvedType.isSubtypeOf(returnType)) {
                currentScope.addError(
                    message = "Return type mismatch: expected $returnType, got $resolvedType",
                    token = returnExpression.startingToken,
                )
            }
        }
    }
}
