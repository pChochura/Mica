package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.ast.expressions.ArrayLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.runtime.resolver.ValueCoercionResolver.coerceToType

internal object ArrayLiteralExpressionExecutor {

    suspend fun execute(
        expression: ArrayLiteralExpression,
        typeResolver: TypeResolver,
        onAnyExpressionCallback: suspend (Expression) -> Any,
    ): List<Any> {
        val arrayType = typeResolver.resolveExpressionType(expression) as ArrayType
        val elementType = arrayType.elementType

        // Coerce every element to the same type
        return expression.elements.map {
            val originalType = typeResolver.resolveExpressionType(it)
            onAnyExpressionCallback(it).coerceToType(originalType, elementType)
        }
    }
}
