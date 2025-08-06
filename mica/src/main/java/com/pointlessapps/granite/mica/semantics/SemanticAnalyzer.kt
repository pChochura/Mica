package com.pointlessapps.granite.mica.semantics

import com.pointlessapps.granite.mica.ast.Root
import com.pointlessapps.granite.mica.semantics.SymbolDeclarationHelper.declareScope
import com.pointlessapps.granite.mica.semantics.checker.StatementsChecker
import com.pointlessapps.granite.mica.semantics.model.Scope
import com.pointlessapps.granite.mica.semantics.model.ScopeType

class SemanticAnalyzer(private val root: Root) {

    private val scope: Scope = root.statements.declareScope(ScopeType.Root)

    init {
        checkRootLevelStatements()
    }

    private fun checkRootLevelStatements() {
        StatementsChecker(scope).check(root.statements)
        scope.warnings.forEach { println(it.formatAsString()) }
        scope.errors.forEach { println(it.formatAsString()) }
    }
}
