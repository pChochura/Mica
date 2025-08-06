package com.pointlessapps.granite.mica.semantics

import com.pointlessapps.granite.mica.ast.Root
import com.pointlessapps.granite.mica.ast.statements.AssignmentStatement
import com.pointlessapps.granite.mica.ast.statements.CommentStatement
import com.pointlessapps.granite.mica.ast.statements.EmptyStatement
import com.pointlessapps.granite.mica.ast.statements.ExpressionStatement
import com.pointlessapps.granite.mica.ast.statements.FunctionCallStatement
import com.pointlessapps.granite.mica.ast.statements.FunctionDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.IfConditionStatement
import com.pointlessapps.granite.mica.ast.statements.ReturnStatement
import com.pointlessapps.granite.mica.ast.statements.UserInputCallStatement
import com.pointlessapps.granite.mica.ast.statements.UserOutputCallStatement
import com.pointlessapps.granite.mica.ast.statements.VariableDeclarationStatement
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.semantics.checker.AssignmentStatementChecker
import com.pointlessapps.granite.mica.semantics.checker.EmptyStatementChecker
import com.pointlessapps.granite.mica.semantics.checker.ExpressionStatementChecker
import com.pointlessapps.granite.mica.semantics.checker.FunctionCallStatementChecker
import com.pointlessapps.granite.mica.semantics.checker.FunctionDeclarationStatementChecker
import com.pointlessapps.granite.mica.semantics.checker.IfConditionStatementChecker
import com.pointlessapps.granite.mica.semantics.checker.ReturnStatementChecker
import com.pointlessapps.granite.mica.semantics.checker.UserInputCallStatementChecker
import com.pointlessapps.granite.mica.semantics.checker.UserOutputCallStatementChecker
import com.pointlessapps.granite.mica.semantics.checker.VariableDeclarationStatementChecker
import com.pointlessapps.granite.mica.semantics.resolver.TypeResolver

class SemanticAnalyzer(private val root: Root) {

    private val functions: MutableMap<String, FunctionDeclarationStatement> = mutableMapOf()
    private val variables: MutableMap<String, VariableDeclarationStatement> = mutableMapOf()

    init {
        declareSymbols()
        resolveTypes()
    }

    private fun declareSymbols() {
        // TODO check for type declarations first
        root.statements.forEach { statement ->
            when (statement) {
                is FunctionDeclarationStatement -> declareFunction(statement)
                is VariableDeclarationStatement -> declareVariable(statement)
                else -> {} // Ignore other statements
            }
        }
    }

    private fun declareFunction(statement: FunctionDeclarationStatement) {
        val signature = statement.signature
        if (functions[signature] != null) {
            showError("Redeclaration of the function: $signature", statement.nameToken)

            return
        }

        functions[signature] = statement
    }

    private fun declareVariable(statement: VariableDeclarationStatement) {
        val name = statement.lhsToken.value
        val declaredVariable = variables[name]
        if (declaredVariable != null) {
            showError(
                message = "Redeclaration of the variable: $name. Use the assignment operator instead",
                token = statement.lhsToken,
            )

            return
        }

        variables[name] = statement
    }

    private fun resolveTypes() {
        val typeResolver = TypeResolver(functions, variables)

        val functionDeclarationStatementChecker =
            FunctionDeclarationStatementChecker(variables, typeResolver)
        val variableDeclarationStatementChecker = VariableDeclarationStatementChecker(typeResolver)
        val assignmentStatementChecker = AssignmentStatementChecker(variables, typeResolver)
        val functionCallStatementChecker = FunctionCallStatementChecker(typeResolver)
        val expressionStatementChecker = ExpressionStatementChecker(typeResolver)
        val ifConditionStatementChecker = IfConditionStatementChecker(typeResolver)
        val userOutputCallStatementChecker = UserOutputCallStatementChecker(typeResolver)
        val userInputCallStatementChecker = UserInputCallStatementChecker(variables)

        val checkRapport = root.statements.mapNotNull {
            when (it) {
                is EmptyStatement -> EmptyStatementChecker.check(it)
                is FunctionDeclarationStatement -> functionDeclarationStatementChecker.check(it)
                is VariableDeclarationStatement -> variableDeclarationStatementChecker.check(it)
                is AssignmentStatement -> assignmentStatementChecker.check(it)
                is FunctionCallStatement -> functionCallStatementChecker.check(it)
                is ExpressionStatement -> expressionStatementChecker.check(it)
                is IfConditionStatement -> ifConditionStatementChecker.check(it)
                is ReturnStatement -> ReturnStatementChecker.check(it)
                is UserOutputCallStatement -> userOutputCallStatementChecker.check(it)
                is UserInputCallStatement -> userInputCallStatementChecker.check(it)
                is CommentStatement -> null
            }
        }

        checkRapport.forEach {
            it.warnings.forEach { warning -> showWarning(warning.message, warning.token) }
            it.errors.forEach { error -> showError(error.message, error.token) }
        }
    }

    private fun showWarning(message: String, token: Token) {
        println("${token.location.line}:${token.location.column}: WARN: $message")
    }

    private fun showError(message: String, token: Token) {
        println("${token.location.line}:${token.location.column}: ERROR: $message")
    }
}
