package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.ast.expressions.ArrayIndexExpression
import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.linter.resolver.TypeCoercionResolver.canBeCoercedTo
import com.pointlessapps.granite.mica.linter.resolver.TypeCoercionResolver.resolveElementTypeCoercedToArray
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.NumberType
import com.pointlessapps.granite.mica.runtime.model.Variable
import com.pointlessapps.granite.mica.runtime.model.Variable.Companion.toVariable
import com.pointlessapps.granite.mica.runtime.resolver.ValueCoercionResolver.coerceToType

internal object ArrayIndexExpressionExecutor {

    suspend fun execute(
        expression: ArrayIndexExpression,
        typeResolver: TypeResolver,
        onAnyExpressionCallback: suspend (Expression) -> Variable<*>,
    ): Variable<*> {
        val arrayType = typeResolver.resolveExpressionType(expression.arrayExpression)
        val indexType = typeResolver.resolveExpressionType(expression.indexExpression)
        val indexValue = onAnyExpressionCallback(expression.indexExpression).value
        val elementType = arrayType.resolveElementTypeCoercedToArray()
        val arrayValue = onAnyExpressionCallback(expression.arrayExpression).value?.coerceToType(
            originalType = arrayType,
            targetType = ArrayType(elementType),
        ) as List<Any>

        if (indexType.canBeCoercedTo(ArrayType(NumberType))) {
            // Return the list consisting only of the provided indices
            val indices = indexValue?.coerceToType(indexType, ArrayType(NumberType)) as List<Double>
            return ArrayType(elementType).toVariable(indices.map { arrayValue[it.toInt()] })
        }

        return elementType.toVariable(
            arrayValue[(indexValue?.coerceToType(indexType, NumberType) as Double).toInt()],
        )
    }
}
