package com.pointlessapps.granite.mica.linter

import com.pointlessapps.granite.mica.ast.Root
import com.pointlessapps.granite.mica.builtins.builtinFunctions
import com.pointlessapps.granite.mica.linter.checker.StatementChecker
import com.pointlessapps.granite.mica.linter.checker.StatementsChecker
import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.linter.model.Report
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.model.ScopeType

/**
 * Analyzes the code and checks for errors or unresolvable types.
 * The rules are defined for each statement type as a [StatementChecker].
 */
class Linter(private val root: Root) {

    private val scope: Scope = Scope(scopeType = ScopeType.Root, parent = null).apply {
        addFunctions(
            builtinFunctions.groupingBy { it.name to it.parameters.size }
                .aggregate { _, acc: MutableMap<List<FunctionOverload.Parameter>, FunctionOverload>?, element, first ->
                    val overload = FunctionOverload(
                        parameters = element.parameters,
                        getReturnType = element.getReturnType,
                        accessType = element.accessType,
                    )
                    if (first) {
                        mutableMapOf(element.parameters to overload)
                    } else {
                        requireNotNull(acc).apply { put(element.parameters, overload) }
                    }
                }.toMutableMap(),
        )
    }

    fun analyze() = checkRootLevelStatements()

    private fun checkRootLevelStatements(): List<Report> {
        StatementsChecker(scope).check(root.statements)
        scope.reports.forEach { println(it.formatAsString()) }

        return scope.reports
    }
}
