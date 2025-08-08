package com.pointlessapps.granite.mica.parser.statement

import com.pointlessapps.granite.mica.ast.statements.FunctionCallStatement
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser
import com.pointlessapps.granite.mica.parser.parseFunctionCallExpression

internal fun Parser.parseFunctionCallStatement(
    parseUntilCondition: (Token) -> Boolean,
): FunctionCallStatement {
    val functionCallExpression = parseFunctionCallExpression(parseUntilCondition)
    return FunctionCallStatement(functionCallExpression)
}
