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
import com.pointlessapps.granite.mica.model.UndefinedType

internal class TypeDeclarationStatementChecker(
    scope: Scope,
    private val typeResolver: TypeResolver,
) : StatementChecker<TypeDeclarationStatement>(scope) {

    override fun check(statement: TypeDeclarationStatement) {
        // Declare the type at the beginning to allow for references
        scope.declareType(
            startingToken = statement.startingToken,
            name = statement.nameToken.value,
            baseType = typeResolver.resolveExpressionType(statement.baseTypeExpression),
        )

        // Declare a function with the type name and properties to be used as a constructor
        scope.declareFunction(
            startingToken = statement.nameToken,
            name = statement.nameToken.value,
            parameters = statement.properties.map {
                typeResolver.resolveExpressionType(it.typeExpression)
            },
            returnType = requireNotNull(scope.getType(statement.nameToken.value)),
            accessType = FunctionOverload.AccessType.GLOBAL_ONLY,
        )

        // Check whether the return statements have the same type as the return type
        statement.checkBaseType()

        // Check whether the parameter types are resolvable
        statement.checkAsBaseTypeMethod()

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
            FunctionDeclarationStatementChecker(localScope, typeResolver).check(
                it.copy(
                    parameters = listOf(
                        FunctionParameterDeclarationStatement(
                            nameToken = Token.Symbol(statement.nameToken.location, "this"),
                            colonToken = Token.Colon(Location.EMPTY),
                            typeExpression = SymbolTypeExpression(statement.nameToken),
                        ),
                    ) + it.parameters,
                ),
            )
        }

        scope.addReports(localScope.reports)
    }

    private fun TypeDeclarationStatement.checkBaseType() {
        val type = typeResolver.resolveExpressionType(baseTypeExpression)
        if (type is UndefinedType) {
            scope.addError(
                message = "Base type (${type.name}) is not defined",
                token = baseTypeExpression.startingToken,
            )
        }
    }

    private fun TypeDeclarationStatement.checkAsBaseTypeMethod() {
        val baseType = typeResolver.resolveExpressionType(baseTypeExpression)
        val baseTypeName = baseType.name.replaceFirstChar { it.uppercase() }
        val asBaseTypeFunction = functions.filter {
            it.nameToken.value == "as$baseTypeName" && it.parameters.isEmpty() &&
                    it.returnTypeExpression?.let(typeResolver::resolveExpressionType) == baseType
        }

        if (asBaseTypeFunction.isEmpty()) {
            scope.addError(
                message = "Type declaration requires an implementation of 'as$baseTypeName(): ${
                    baseType.name
                }' function",
                token = startingToken,
            )
        }
    }
}
