package com.pointlessapps.granite.mica.linter

import com.pointlessapps.granite.mica.ast.Root
import com.pointlessapps.granite.mica.builtins.builtinFunctionDeclarations
import com.pointlessapps.granite.mica.linter.checker.StatementChecker
import com.pointlessapps.granite.mica.linter.checker.StatementsChecker
import com.pointlessapps.granite.mica.linter.model.Report
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.model.ScopeType
import com.pointlessapps.granite.mica.model.Type

/**
 * Analyzes the code and checks for errors or unresolvable types.
 * The rules are defined for each statement type as a [StatementChecker].
 */
class Linter(private val root: Root) {

    private val scope: Scope = Scope(scopeType = ScopeType.Root, parent = null).apply {
        functions.putAll(
            builtinFunctionDeclarations.mapValues { (_, v) ->
                v.mapValues { (_, v2) ->
                    v2.getReturnType(v2.parameters.map(Pair<String, Type>::second))
                }.toMutableMap()
            },
        )
    }

    fun analyze() = checkRootLevelStatements()

    private fun checkRootLevelStatements(): List<Report> {
        StatementsChecker(scope).check(root.statements)
        scope.reports.forEach { println(it.formatAsString()) }

        return scope.reports
    }
}
