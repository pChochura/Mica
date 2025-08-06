package com.pointlessapps.granite.mica.semantics.checker

import com.pointlessapps.granite.mica.ast.statements.ReturnStatement
import com.pointlessapps.granite.mica.semantics.model.Scope
import com.pointlessapps.granite.mica.semantics.model.ScopeType
import com.pointlessapps.granite.mica.semantics.model.VoidType
import com.pointlessapps.granite.mica.semantics.resolver.TypeCoercionResolver.canBeCoercedTo
import com.pointlessapps.granite.mica.semantics.resolver.TypeResolver

internal class ReturnStatementChecker(
    scope: Scope,
    private val typeResolver: TypeResolver,
) : StatementChecker<ReturnStatement>(scope) {

    override fun check(statement: ReturnStatement) {
        if (scope.scopeType is ScopeType.Root) {
            scope.addError(
                message = "Root level return statement is not supported",
                token = statement.startingToken,
            )
        }

        // Traverse the parents until we find a function scope
        // and then check for the type of the return statement
        statement.checkParentFunctionScopeReturnType()
    }

    private fun ReturnStatement.checkParentFunctionScopeReturnType() {
        var currentScope: Scope? = scope.parent
        while (currentScope != null && currentScope.scopeType !is ScopeType.Function) {
            currentScope = currentScope.parent
        }

        if (currentScope?.scopeType !is ScopeType.Function) return

        val functionScope = currentScope.scopeType
        val functionDeclarationStatement = functionScope.functionDeclarationStatement
        val returnType = functionDeclarationStatement.returnType
        if (returnType != null && returnType !is VoidType && returnExpression != null) {
            val resolvedType = typeResolver.resolveExpressionType(returnExpression)
            if (!resolvedType.canBeCoercedTo(returnType)) {
                currentScope.addError(
                    message = "Return type mismatch: expected $returnType, got $resolvedType",
                    token = returnExpression.startingToken,
                )
            }
        }
    }
}
