package com.pointlessapps.granite.mica.semantics

import com.pointlessapps.granite.mica.ast.Root
import com.pointlessapps.granite.mica.semantics.SymbolDeclarationHelper.declareScope
import com.pointlessapps.granite.mica.semantics.checker.StatementChecker
import com.pointlessapps.granite.mica.semantics.checker.StatementsChecker
import com.pointlessapps.granite.mica.semantics.model.Scope
import com.pointlessapps.granite.mica.semantics.model.ScopeType

/**
 * Analyzes the code and checks for errors or unresolvable types.
 * The rules are defined for each statement type as a [StatementChecker].
 */
class SemanticAnalyzer(private val root: Root) {

    private val scope: Scope = root.statements.declareScope(ScopeType.Root)

    init {
        checkRootLevelStatements()
    }

    private fun checkRootLevelStatements() {
        StatementsChecker(scope).check(root.statements)
        scope.reports.forEach { println(it.formatAsString()) }
    }
}
