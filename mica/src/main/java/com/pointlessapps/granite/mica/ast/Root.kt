package com.pointlessapps.granite.mica.ast

import com.pointlessapps.granite.mica.ast.statements.Statement

data class Root(
    val statements: List<Statement>,
)
