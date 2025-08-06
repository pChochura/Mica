package com.pointlessapps.granite.mica.ast

import com.pointlessapps.granite.mica.ast.statements.Statement

/**
 * Root of the AST.
 */
data class Root(
    val statements: List<Statement>,
)
