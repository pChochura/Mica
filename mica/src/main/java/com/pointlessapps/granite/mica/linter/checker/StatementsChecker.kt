package com.pointlessapps.granite.mica.linter.checker

import com.pointlessapps.granite.mica.ast.statements.ArrayAssignmentStatement
import com.pointlessapps.granite.mica.ast.statements.AssignmentStatement
import com.pointlessapps.granite.mica.ast.statements.BreakStatement
import com.pointlessapps.granite.mica.ast.statements.ExpressionStatement
import com.pointlessapps.granite.mica.ast.statements.FunctionDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.IfConditionStatement
import com.pointlessapps.granite.mica.ast.statements.LoopStatement
import com.pointlessapps.granite.mica.ast.statements.ReturnStatement
import com.pointlessapps.granite.mica.ast.statements.Statement
import com.pointlessapps.granite.mica.ast.statements.TypeDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.UserInputCallStatement
import com.pointlessapps.granite.mica.ast.statements.UserOutputCallStatement
import com.pointlessapps.granite.mica.ast.statements.VariableDeclarationStatement
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver

internal class StatementsChecker(scope: Scope) {
    private val typeResolver = TypeResolver(scope)

    private val typeDeclarationStatementChecker =
        TypeDeclarationStatementChecker(scope, typeResolver)
    private val functionDeclarationStatementChecker =
        FunctionDeclarationStatementChecker(scope, typeResolver)
    private val variableDeclarationStatementChecker =
        VariableDeclarationStatementChecker(scope, typeResolver)
    private val assignmentStatementChecker = AssignmentStatementChecker(scope, typeResolver)
    private val arrayAssignmentStatementChecker =
        ArrayAssignmentStatementChecker(scope, typeResolver)
    private val expressionStatementChecker = ExpressionStatementChecker(scope, typeResolver)
    private val loopStatementChecker = LoopStatementChecker(scope, typeResolver)
    private val ifConditionStatementChecker = IfConditionStatementChecker(scope, typeResolver)
    private val returnStatementChecker = ReturnStatementChecker(scope, typeResolver)
    private val breakStatementChecker = BreakStatementChecker(scope)
    private val userOutputCallStatementChecker = UserOutputCallStatementChecker(scope, typeResolver)
    private val userInputCallStatementChecker = UserInputCallStatementChecker(scope)

    fun check(statements: List<Statement>) {
        statements.forEach { statement ->
            when (statement) {
                is TypeDeclarationStatement ->
                    typeDeclarationStatementChecker.check(statement)

                is FunctionDeclarationStatement ->
                    functionDeclarationStatementChecker.check(statement)

                is VariableDeclarationStatement ->
                    variableDeclarationStatementChecker.check(statement)

                is AssignmentStatement -> assignmentStatementChecker.check(statement)
                is ArrayAssignmentStatement -> arrayAssignmentStatementChecker.check(statement)
                is ExpressionStatement -> expressionStatementChecker.check(statement)
                is LoopStatement -> loopStatementChecker.check(statement)
                is IfConditionStatement -> ifConditionStatementChecker.check(statement)
                is ReturnStatement -> returnStatementChecker.check(statement)
                is BreakStatement -> breakStatementChecker.check(statement)
                is UserOutputCallStatement -> userOutputCallStatementChecker.check(statement)
                is UserInputCallStatement -> userInputCallStatementChecker.check(statement)
            }
        }
    }
}
