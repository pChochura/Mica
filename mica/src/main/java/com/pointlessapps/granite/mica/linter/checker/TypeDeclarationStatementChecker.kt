package com.pointlessapps.granite.mica.linter.checker

import com.pointlessapps.granite.mica.ast.expressions.SymbolTypeExpression
import com.pointlessapps.granite.mica.ast.statements.FunctionParameterDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.TypeDeclarationStatement
import com.pointlessapps.granite.mica.helper.isTypeParameter
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
        // Declare the type parameter constraint as a `type` keyword
        // Do this before declaring the function to allow for referencing
        statement.checkTypeParameterConstraint()

        // Declare the type at the beginning to allow for references
        val parentType = statement.parentTypeExpression?.let(typeResolver::resolveExpressionType)
        scope.declareType(
            startingToken = statement.startingToken,
            name = statement.nameToken.value,
            parentType = parentType,
            typeParameterConstraint = statement.typeParameterConstraint
                ?.let(typeResolver::resolveExpressionType),
            properties = statement.properties.associate {
                val type = typeResolver.resolveExpressionType(it.typeExpression)
                val hasDefaultValue = it.defaultValueExpression != null
                it.nameToken.value to (type to hasDefaultValue)
            },
        )

        // Declare a function with the type name and properties to be used as a constructor
        scope.declareFunction(
            startingToken = statement.nameToken,
            name = statement.nameToken.value,
            typeParameterConstraint = statement.typeParameterConstraint
                ?.let(typeResolver::resolveExpressionType),
            parameters = statement.properties.map {
                FunctionOverload.Parameter(
                    type = typeResolver.resolveExpressionType(it.typeExpression),
                    vararg = false,
                    resolver = FunctionOverload.Parameter.Resolver.SUBTYPE_MATCH,
                )
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
        statement.checkParentProperties(localScope)

        // Check the correctness of the body
        statement.checkBody(localScope)

        // Check whether the types of the properties match
        statement.checkPropertiesTypes()

        scope.addReports(localScope.reports)
    }

    private fun TypeDeclarationStatement.checkParentProperties(localScope: Scope) {
        val parentType = parentTypeExpression?.let(typeResolver::resolveExpressionType)
        val parentProperties = parentType?.let(scope::getTypeProperties).orEmpty().toMutableMap()
        properties.forEach {
            val name = it.nameToken.value
            val type = typeResolver.resolveExpressionType(it.typeExpression)
            localScope.declareVariable(it.nameToken, name, type)
            if (parentProperties.containsKey(name)) {
                if (parentProperties[name]?.returnType != type) {
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
                token = parentTypeExpression?.startingToken ?: nameToken,
            )
        }
    }

    private fun TypeDeclarationStatement.checkBody(localScope: Scope) {
        val receiverParameter = listOf(
            FunctionParameterDeclarationStatement(
                varargToken = null,
                nameToken = Token.Symbol(nameToken.location, "this"),
                colonToken = Token.Colon(Location.EMPTY),
                typeExpression = SymbolTypeExpression(
                    symbolToken = nameToken,
                    atToken = atToken,
                    typeParameterConstraint = typeParameterConstraint,
                ),
                exclamationMarkToken = null,
                equalsToken = null,
                defaultValueExpression = null,
            ),
        )
        functions.forEach {
            FunctionDeclarationStatementChecker(localScope, TypeResolver(localScope)).check(
                it.copy(parameters = receiverParameter + it.parameters),
            )
        }
    }

    private fun TypeDeclarationStatement.checkPropertiesTypes() {
        properties.forEach { property ->
            val type = typeResolver.resolveExpressionType(property.typeExpression)
            if (property.defaultValueExpression == null) {
                return@forEach
            }

            val defaultValueType =
                typeResolver.resolveExpressionType(property.defaultValueExpression)
            if (type.isTypeParameter()) {
                scope.addError(
                    message = "Default property values are allowed only for the concrete types",
                    token = property.nameToken,
                )
            } else if (!defaultValueType.isSubtypeOf(type)) {
                scope.addError(
                    message = "Property default value type (${
                        defaultValueType
                    }) does not match the property type ($type)",
                    token = property.defaultValueExpression.startingToken,
                )
            }
        }
    }

    private fun TypeDeclarationStatement.checkTypeParameterConstraint() {
        if (typeParameterConstraint == null) return
        scope.declareGenericType(
            parentType = typeResolver.resolveExpressionType(typeParameterConstraint),
        )
    }
}
