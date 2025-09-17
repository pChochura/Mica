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
        val parentType = statement.parentTypeExpression?.let(typeResolver::resolveExpressionType)
        scope.declareType(
            startingToken = statement.startingToken,
            name = statement.nameToken.value,
            parentType = parentType,
            properties = statement.properties.associate {
                it.nameToken.value to typeResolver.resolveExpressionType(it.typeExpression)
            },
        )

        // Declare a function with the type name and properties to be used as a constructor
        scope.declareFunction(
            startingToken = statement.nameToken,
            name = statement.nameToken.value,
            isVararg = false,
            typeParameterConstraint = null,
            parameters = statement.properties.map {
                typeResolver.resolveExpressionType(it.typeExpression) to false
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
        val parentProperties = parentType?.let(scope::getTypeProperties).orEmpty().toMutableMap()
        statement.properties.forEach {
            val name = it.nameToken.value
            val type = typeResolver.resolveExpressionType(it.typeExpression)
            localScope.declareVariable(it.nameToken, name, type)
            if (parentProperties.containsKey(name)) {
                if (parentProperties[name] != type) {
                    localScope.addError(
                        message = "Property $name does not match the parent type",
                        token = it.nameToken,
                    )
                }
                parentProperties.remove(name)
            }
        }
        if (parentProperties.isNotEmpty()) {
            scope.addError(
                message = "Missing properties: ${parentProperties.keys.joinToString(", ")}",
                token = statement.parentTypeExpression?.startingToken ?: statement.nameToken,
            )
        }

        // Check the correctness of the body
        val receiverParameter = listOf(
            FunctionParameterDeclarationStatement(
                varargToken = null,
                nameToken = Token.Symbol(statement.nameToken.location, "this"),
                colonToken = Token.Colon(Location.EMPTY),
                typeExpression = SymbolTypeExpression(statement.nameToken),
                exclamationMarkToken = null,
                equalsToken = null,
                defaultValueExpression = null,
            ),
        )
        statement.functions.forEach {
            FunctionDeclarationStatementChecker(localScope, TypeResolver(localScope)).check(
                it.copy(parameters = receiverParameter + it.parameters),
            )
        }

        scope.addReports(localScope.reports)
    }
}
