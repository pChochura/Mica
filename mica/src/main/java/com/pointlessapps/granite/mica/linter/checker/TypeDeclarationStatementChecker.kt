package com.pointlessapps.granite.mica.linter.checker

import com.pointlessapps.granite.mica.ast.expressions.SymbolTypeExpression
import com.pointlessapps.granite.mica.ast.statements.FunctionParameterDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.TypeDeclarationStatement
import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.model.ScopeType
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
import com.pointlessapps.granite.mica.model.Location
import com.pointlessapps.granite.mica.model.Token

internal class TypeDeclarationStatementChecker(
    scope: Scope,
    private val typeResolver: TypeResolver,
) : StatementChecker<TypeDeclarationStatement>(scope) {

    override fun check(statement: TypeDeclarationStatement) {
        // Declare the type at the beginning to allow for references
        scope.declareType(
            startingToken = statement.startingToken,
            name = statement.nameToken.value,
            properties = statement.properties.associate {
                it.nameToken.value to typeResolver.resolveExpressionType(it.typeExpression)
            },
        )

        // Declare a function with the type name and properties to be used as a constructor
        scope.declareFunction(
            startingToken = statement.nameToken,
            name = statement.nameToken.value,
            parameters = statement.properties.map {
                typeResolver.resolveExpressionType(it.typeExpression)
            },
            returnType = requireNotNull(
                value = scope.getType(statement.nameToken.value),
                lazyMessage = { "Type ${statement.nameToken.value} not found" },
            ),
            accessType = FunctionOverload.AccessType.GLOBAL_ONLY,
        )

        val localScope = Scope(
            scopeType = ScopeType.Type(statement),
            parent = scope,
        )
        // Declare properties as variables
        statement.properties.forEach {
            localScope.declareVariable(
                startingToken = it.nameToken,
                name = it.nameToken.value,
                type = typeResolver.resolveExpressionType(it.typeExpression),
            )
        }

        // Check the correctness of the body
        statement.functions.forEach {
            FunctionDeclarationStatementChecker(localScope, TypeResolver(localScope)).check(
                it.copy(
                    parameters = listOf(
                        FunctionParameterDeclarationStatement(
                            nameToken = Token.Symbol(statement.nameToken.location, "this"),
                            colonToken = Token.Colon(Location.EMPTY),
                            typeExpression = SymbolTypeExpression(statement.nameToken),
                            equalsToken = null,
                            defaultValueExpression = null,
                        ),
                    ) + it.parameters,
                ),
            )
        }

        scope.addReports(localScope.reports)
    }
}
