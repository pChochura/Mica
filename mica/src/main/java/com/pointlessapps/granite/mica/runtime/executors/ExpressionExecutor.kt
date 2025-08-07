package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver

internal interface ExpressionExecutor<T : Expression> {
    fun execute(
        expression: T,
        typeResolver: TypeResolver,
        onAnyExpressionCallback: (Expression) -> Any,
    ): Any
}
