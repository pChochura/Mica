package com.pointlessapps.granite.mica

import com.pointlessapps.granite.mica.ast.Root
import com.pointlessapps.granite.mica.ast.expressions.BinaryExpression
import com.pointlessapps.granite.mica.ast.expressions.BooleanLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.FunctionCallExpression
import com.pointlessapps.granite.mica.ast.expressions.NumberLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.StringLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolExpression
import com.pointlessapps.granite.mica.ast.expressions.UnaryExpression
import com.pointlessapps.granite.mica.ast.statements.AssignmentStatement
import com.pointlessapps.granite.mica.ast.statements.CommentStatement
import com.pointlessapps.granite.mica.ast.statements.EmptyStatement
import com.pointlessapps.granite.mica.ast.statements.FunctionCallStatement
import com.pointlessapps.granite.mica.ast.statements.FunctionDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.FunctionParameterDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.Statement
import com.pointlessapps.granite.mica.ast.statements.UserInputCallStatement
import com.pointlessapps.granite.mica.ast.statements.UserOutputCallStatement
import com.pointlessapps.granite.mica.errors.MissingTokenException
import com.pointlessapps.granite.mica.errors.UnexpectedRootLevelInputException
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token

data class Parser(
    private val tokens: List<Token>,
) {
    private var currentIndex: Int = 0

    private fun advance() = currentIndex++
    private fun getToken(): Token = tokens.getOrNull(currentIndex) ?: Token.EOF
    private fun getNextToken(): Token = advance().let { getToken() }

    private inline fun <reified T : Token> expectToken(): T {
        val token = getToken()
        assert(T::class.isInstance(token)) {
            throw UnexpectedTokenException("Expected ${T::class.simpleName}, but got $token")
        }

        advance()

        return token as T
    }

    private fun expectEOForEOL() {
        val token = getToken()
        assert(token.let { it == Token.EOF || it == Token.EOL }) {
            throw UnexpectedTokenException(
                "Expected ${Token.EOF} or ${Token.EOL}, but got $token",
            )
        }

        advance()
    }

    private fun parseListOfStatements(
        parseUntilCondition: (Token) -> Boolean,
    ): List<Statement> {
        val statements = mutableListOf<Statement>()

        do {
            while (getToken() == Token.EOL) {
                advance()
            }

            if (!parseUntilCondition(getToken())) {
                break
            }

            val statement = parseStatement()
            if (statement == null) {
                throw UnexpectedRootLevelInputException("Only statements are allowed on the root level")
            }

            statements.add(statement)
        } while (parseUntilCondition(getToken()))

        return statements.toList()
    }

    private fun parseStatement(): Statement? = when (val token = getToken()) {
        is Token.Whitespace -> EmptyStatement
        is Token.Comment -> parseCommentStatement()
        Token.GraterThan -> parseUserOutputCallStatement()
        Token.LessThan -> parseUserInputCallStatement()
        is Token.Symbol -> parseInSequence(
            ::parseFunctionDeclarationStatement,
            ::parseFunctionCallStatement,
            ::parseAssignmentStatement,
        )

        else -> throw UnexpectedTokenException("Expected statement, but got $token")
    }

    private fun <T> parseInSequence(vararg statements: () -> T): T? {
        statements.forEach {
            val savedIndex = currentIndex
            val statement = runCatching { it.invoke() }.getOrNull()

            if (statement != null) {
                return statement
            }

            currentIndex = savedIndex
        }

        return null
    }

    private fun parseCommentStatement(): CommentStatement {
        val currentToken = expectToken<Token.Comment>()
        expectEOForEOL()

        return CommentStatement(currentToken)
    }

    // TODO support more types to be output
    private fun parseUserOutputCallStatement(): UserOutputCallStatement {
        val userOutputStartingToken = expectToken<Token.GraterThan>()
        val stringLiteralToken = expectToken<Token.StringLiteral>()
        expectEOForEOL()

        return UserOutputCallStatement(userOutputStartingToken, stringLiteralToken)
    }

    private fun parseUserInputCallStatement(): UserInputCallStatement {
        val userInputStartingToken = expectToken<Token.LessThan>()
        val stringLiteralToken = expectToken<Token.Symbol>()
        expectEOForEOL()

        return UserInputCallStatement(userInputStartingToken, stringLiteralToken)
    }

    private fun parseFunctionDeclarationStatement(): FunctionDeclarationStatement {
        val nameToken = expectToken<Token.Symbol>()
        val openBracketToken = expectToken<Token.BracketOpen>()
        val parameters = mutableListOf<FunctionParameterDeclarationStatement>()
        while (getToken() != Token.BracketClose) {
            val parameterNameToken = expectToken<Token.Symbol>()
            val parameterColonToken = expectToken<Token.Colon>()
            val parameterTypeToken = expectToken<Token.Keyword>()

            parameters.add(
                FunctionParameterDeclarationStatement(
                    nameToken = parameterNameToken,
                    colonToken = parameterColonToken,
                    typeToken = parameterTypeToken,
                ),
            )

            if (getToken() == Token.Comma) {
                advance()

                assert(getToken() != Token.BracketClose) {
                    throw UnexpectedTokenException(
                        "Expected a parameter declaration, but got ${getToken()}",
                    )
                }
            }
        }
        val closeBracketToken = expectToken<Token.BracketClose>()

        var colonToken: Token.Colon? = null
        // TODO support custom return types
        var returnTypeToken: Token.Keyword? = null
        if (getToken() == Token.Colon) {
            colonToken = expectToken<Token.Colon>()
            returnTypeToken = expectToken<Token.Keyword>()
        }

        val openCurlyToken = expectToken<Token.CurlyBracketOpen>()
        val body = parseListOfStatements(parseUntilCondition = { it != Token.CurlyBracketClose })
        val closeCurlyToken = expectToken<Token.CurlyBracketClose>()

        return FunctionDeclarationStatement(
            nameToken = nameToken,
            openBracketToken = openBracketToken,
            closeBracketToken = closeBracketToken,
            openCurlyToken = openCurlyToken,
            closeCurlyToken = closeCurlyToken,
            colonToken = colonToken,
            returnTypeToken = returnTypeToken,
            parameters = parameters,
            body = body,
        )
    }

    private fun parseFunctionCallStatement(): FunctionCallStatement {
        val functionCallExpression = parseFunctionCallExpression()
        expectEOForEOL()

        return FunctionCallStatement(functionCallExpression)
    }

    private fun parseAssignmentStatement(): AssignmentStatement {
        val lhsToken = expectToken<Token.Symbol>()
        val equalSignToken = expectToken<Token.Equals>()
        val rhs = requireNotNull(parseExpression()) {
            throw MissingTokenException("Expected expression after $equalSignToken")
        }
        expectEOForEOL()

        return AssignmentStatement(lhsToken, equalSignToken, rhs)
    }

    private fun parseExpression(
        minBindingPower: Int = 0,
        parseUntilCondition: (Token) -> Boolean = { it == Token.EOL || it == Token.EOF },
    ): Expression? {
        val token = getToken()
        var lhs = when (token) {
            is Token.Symbol -> parseInSequence(
                { parseFunctionCallExpression() },
                { SymbolExpression(token) },
            )

            is Token.NumberLiteral -> NumberLiteralExpression(token)
            is Token.BooleanLiteral -> BooleanLiteralExpression(token)
            is Token.StringLiteral -> StringLiteralExpression(token)
            is Token.Operator -> {
                val (_, rbp) = getBindingPowers(token)
                val expression = parseExpression(rbp)
                if (expression == null) {
                    throw UnexpectedTokenException("Expected expression, but got $token")
                }

                UnaryExpression(token, expression)
            }

            else -> null
        } ?: throw UnexpectedTokenException("Expected expression, but got $token")

        if (parseUntilCondition(getNextToken())) {
            return lhs
        }

        while (true) {
            val operator = when (val token = getToken()) {
                Token.EOF, Token.EOL -> break
                is Token.Operator -> token
                else -> throw UnexpectedTokenException("Expected operator, but got $token")
            }

            val (lbp, rbp) = getBindingPowers(operator)
            if (lbp < minBindingPower) {
                break
            }

            advance()

            val rhs = parseExpression(rbp)
            if (rhs == null) {
                throw UnexpectedTokenException("Expected expression, but got ${getToken()}")
            }

            lhs = BinaryExpression(lhs, operator, rhs)
        }

        return lhs
    }

    private fun parseFunctionCallExpression(): FunctionCallExpression {
        val nameToken = expectToken<Token.Symbol>()
        val openBracketToken = expectToken<Token.BracketOpen>()
        val arguments = mutableListOf<Expression>()
        while (getToken() != Token.BracketClose) {
            val argument = parseExpression(
                parseUntilCondition = {
                    it == Token.Comma || it == Token.BracketClose
                },
            )
            if (argument == null) {
                throw UnexpectedTokenException("Expected expression, but got ${getToken()}")
            }

            arguments.add(argument)

            if (getToken() == Token.Comma) {
                advance()

                assert(getToken() != Token.BracketClose) {
                    throw UnexpectedTokenException(
                        "Expected an expression, but got ${getToken()}",
                    )
                }
            }
        }
        val closeBracketToken = expectToken<Token.BracketClose>()

        return FunctionCallExpression(nameToken, openBracketToken, closeBracketToken, arguments)
    }

    fun parse(): Root {
        if (getToken() == Token.EOF) return Root(emptyList())
        return Root(parseListOfStatements(parseUntilCondition = { it != Token.EOF }))
    }

    private fun getBindingPowers(token: Token.Operator): Pair<Int, Int> {
        return when (token.type) {
            Token.Operator.Type.Or -> 1 to 2
            Token.Operator.Type.And -> 3 to 4
            Token.Operator.Type.Compare -> 5 to 6
            Token.Operator.Type.Add, Token.Operator.Type.Subtract -> 7 to 8
            Token.Operator.Type.Multiply, Token.Operator.Type.Divide -> 9 to 10
            Token.Operator.Type.Exponent -> 12 to 11
            Token.Operator.Type.Range -> 13 to 14
            else -> throw UnexpectedTokenException("Unexpected operator $token")
        }
    }
}
