package com.pointlessapps.granite.mica.semantics.checker

import com.pointlessapps.granite.mica.ast.statements.AssignmentStatement
import com.pointlessapps.granite.mica.ast.statements.CommentStatement
import com.pointlessapps.granite.mica.ast.statements.EmptyStatement
import com.pointlessapps.granite.mica.ast.statements.ExpressionStatement
import com.pointlessapps.granite.mica.ast.statements.FunctionCallStatement
import com.pointlessapps.granite.mica.ast.statements.FunctionDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.IfConditionStatement
import com.pointlessapps.granite.mica.ast.statements.ReturnStatement
import com.pointlessapps.granite.mica.ast.statements.Statement
import com.pointlessapps.granite.mica.ast.statements.UserInputCallStatement
import com.pointlessapps.granite.mica.ast.statements.UserOutputCallStatement
import com.pointlessapps.granite.mica.ast.statements.VariableDeclarationStatement
import com.pointlessapps.granite.mica.semantics.model.Scope
import com.pointlessapps.granite.mica.semantics.resolver.TypeResolver

internal class StatementsChecker(scope: Scope) {
    private val typeResolver = TypeResolver(scope)

    private val emptyStatementChecker = EmptyStatementChecker(scope)
    private val functionDeclarationStatementChecker =
        FunctionDeclarationStatementChecker(scope, typeResolver)
    private val variableDeclarationStatementChecker =
        VariableDeclarationStatementChecker(scope, typeResolver)
    private val assignmentStatementChecker = AssignmentStatementChecker(scope, typeResolver)
    private val functionCallStatementChecker = FunctionCallStatementChecker(scope, typeResolver)
    private val expressionStatementChecker = ExpressionStatementChecker(scope, typeResolver)
    private val ifConditionStatementChecker = IfConditionStatementChecker(scope, typeResolver)
    private val returnStatementChecker = ReturnStatementChecker(scope, typeResolver)
    private val userOutputCallStatementChecker = UserOutputCallStatementChecker(scope, typeResolver)
    private val userInputCallStatementChecker = UserInputCallStatementChecker(scope)

    fun check(statements: List<Statement>) {
        statements.forEach { statement ->
            when (statement) {
                is EmptyStatement -> emptyStatementChecker.check(statement)
                is FunctionDeclarationStatement ->
                    functionDeclarationStatementChecker.check(statement)

                is VariableDeclarationStatement ->
                    variableDeclarationStatementChecker.check(statement)

                is AssignmentStatement -> assignmentStatementChecker.check(statement)
                is FunctionCallStatement -> functionCallStatementChecker.check(statement)
                is ExpressionStatement -> expressionStatementChecker.check(statement)
                is IfConditionStatement -> ifConditionStatementChecker.check(statement)
                is ReturnStatement -> returnStatementChecker.check(statement)
                is UserOutputCallStatement -> userOutputCallStatementChecker.check(statement)
                is UserInputCallStatement -> userInputCallStatementChecker.check(statement)
                is CommentStatement -> {}
            }
        }
    }
}
