package com.pointlessapps.granite.mica.parser.expression

import com.pointlessapps.granite.mica.ast.ArrayIndex
import com.pointlessapps.granite.mica.ast.expressions.BooleanLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.CharLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.NumberLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.ParenthesisedExpression
import com.pointlessapps.granite.mica.ast.expressions.PostfixAssignmentExpression
import com.pointlessapps.granite.mica.ast.expressions.PrefixAssignmentExpression
import com.pointlessapps.granite.mica.ast.expressions.StringLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolExpression
import com.pointlessapps.granite.mica.ast.expressions.UnaryExpression
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser
import com.pointlessapps.granite.mica.parser.isFunctionCallStatementStarting
import com.pointlessapps.granite.mica.parser.isPostfixUnaryExpressionStarting

internal fun Parser.parseLhsExpression(
    parseUntilCondition: (Token) -> Boolean,
) = when (val token = getToken()) {
    is Token.Symbol -> parseSymbolExpression(parseUntilCondition)
    is Token.NumberLiteral -> NumberLiteralExpression(token).also { advance() }
    is Token.BooleanLiteral -> BooleanLiteralExpression(token).also { advance() }
    is Token.CharLiteral -> CharLiteralExpression(token).also { advance() }
    is Token.StringLiteral -> StringLiteralExpression(token).also { advance() }
    is Token.Operator -> parseUnaryExpression(parseUntilCondition)
    is Token.BracketOpen -> parseParenthesisedExpression(parseUntilCondition)
    is Token.SquareBracketOpen -> parseArrayLiteralExpression(parseUntilCondition)
    is Token.CurlyBracketOpen -> parseSetLiteralExpression(parseUntilCondition)
    is Token.Increment, is Token.Decrement -> parsePrefixAssignmentExpression()
    else -> null
}

private fun Parser.parseSymbolExpression(
    parseUntilCondition: (Token) -> Boolean,
): Expression = when {
    isFunctionCallStatementStarting() -> parseFunctionCallExpression(parseUntilCondition)
    isPostfixUnaryExpressionStarting() -> parsePostfixUnaryExpression()
    else -> SymbolExpression(expectToken<Token.Symbol>("symbol expression") { it !is Token.Keyword })
}

private fun Parser.parsePostfixUnaryExpression(): PostfixAssignmentExpression {
    val symbolToken = expectToken<Token.Symbol>("postfix assignment expression") {
        it !is Token.Keyword
    }
    val indexExpressions = mutableListOf<ArrayIndex>()
    while (isToken<Token.SquareBracketOpen>()) {
        val openBracketToken = expectToken<Token.SquareBracketOpen>("postfix assignment expression")
        val expression = parseExpression(0f) { it is Token.SquareBracketClose }
            ?: throw UnexpectedTokenException(
                expectedToken = "expression",
                actualToken = getToken(),
                currentlyParsing = "postfix assignment expression",
            )
        val closeBracketToken =
            expectToken<Token.SquareBracketClose>("postfix assignment expression")
        indexExpressions.add(
            ArrayIndex(openBracketToken, closeBracketToken, expression),
        )
    }
    val postfixOperatorToken = expectToken<Token>("postfix assignment expression") {
        it is Token.Increment || it is Token.Decrement
    }

    return PostfixAssignmentExpression(symbolToken, indexExpressions, postfixOperatorToken)
}

private fun Parser.parsePrefixAssignmentExpression(): PrefixAssignmentExpression {
    val prefixOperatorToken = expectToken<Token>("prefix assignment expression") {
        it is Token.Increment || it is Token.Decrement
    }
    val symbolToken = expectToken<Token.Symbol>("prefix assignment expression") {
        it !is Token.Keyword
    }
    val indexExpressions = mutableListOf<ArrayIndex>()
    while (isToken<Token.SquareBracketOpen>()) {
        val openBracketToken = expectToken<Token.SquareBracketOpen>("prefix assignment expression")
        val expression = parseExpression(0f) { it is Token.SquareBracketClose }
            ?: throw UnexpectedTokenException(
                expectedToken = "expression",
                actualToken = getToken(),
                currentlyParsing = "prefix assignment expression",
            )
        val closeBracketToken =
            expectToken<Token.SquareBracketClose>("prefix assignment expression")
        indexExpressions.add(
            ArrayIndex(openBracketToken, closeBracketToken, expression),
        )
    }

    return PrefixAssignmentExpression(prefixOperatorToken, symbolToken, indexExpressions)
}

private fun Parser.parseUnaryExpression(
    parseUntilCondition: (Token) -> Boolean,
): UnaryExpression {
    val operatorToken = expectToken<Token.Operator>("unary expression")
    val rbp = getPrefixBindingPower(operatorToken, "unary expression")
    val expression = parseExpression(rbp, parseUntilCondition)
        ?: throw UnexpectedTokenException("expression", getToken(), "unary expression")

    return UnaryExpression(operatorToken, expression)
}

private fun Parser.parseParenthesisedExpression(
    parseUntilCondition: (Token) -> Boolean,
): ParenthesisedExpression {
    val openBracketToken = expectToken<Token.BracketOpen>("parenthesised expression")
    val expression = parseExpression(0f) { parseUntilCondition(it) || it is Token.BracketClose }
        ?: throw UnexpectedTokenException("expression", getToken(), "parenthesised expression")
    val closeBracketToken = expectToken<Token.BracketClose>("parenthesised expression")
    return ParenthesisedExpression(
        openBracketToken = openBracketToken,
        closeBracketToken = closeBracketToken,
        expression = expression,
    )
}

private fun getPrefixBindingPower(token: Token, currentlyParsing: String): Float = when (token) {
    is Token.Operator -> when (token.type) {
        Token.Operator.Type.Not -> 11f
        Token.Operator.Type.Add, Token.Operator.Type.Subtract -> 9.5f
        else -> throw UnexpectedTokenException("prefix unary operator or (", token, currentlyParsing)
    }

    is Token.BracketOpen -> 0.5f
    else -> throw UnexpectedTokenException("prefix unary operator or (", token, currentlyParsing)
}
