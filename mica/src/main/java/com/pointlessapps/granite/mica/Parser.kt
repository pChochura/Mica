package com.pointlessapps.granite.mica

import com.pointlessapps.granite.mica.ast.Root
import com.pointlessapps.granite.mica.ast.expressions.BinaryExpression
import com.pointlessapps.granite.mica.ast.expressions.BooleanLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.FunctionCallExpression
import com.pointlessapps.granite.mica.ast.expressions.NumberLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.ParenthesisedExpression
import com.pointlessapps.granite.mica.ast.expressions.StringLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolExpression
import com.pointlessapps.granite.mica.ast.expressions.UnaryExpression
import com.pointlessapps.granite.mica.ast.statements.AssignmentStatement
import com.pointlessapps.granite.mica.ast.statements.CommentStatement
import com.pointlessapps.granite.mica.ast.statements.ElseIfConditionStatement
import com.pointlessapps.granite.mica.ast.statements.ElseStatement
import com.pointlessapps.granite.mica.ast.statements.EmptyStatement
import com.pointlessapps.granite.mica.ast.statements.FunctionCallStatement
import com.pointlessapps.granite.mica.ast.statements.FunctionDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.FunctionParameterDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.IfConditionStatement
import com.pointlessapps.granite.mica.ast.statements.ReturnStatement
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

    private inline fun <reified T : Token> expectToken(condition: (T) -> Boolean = { true }): T {
        val token = getToken()
        assert(T::class.isInstance(token) && condition(token as T)) {
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
        is Token.Operator -> when (token.type) {
            Token.Operator.Type.GraterThan -> parseUserOutputCallStatement()
            Token.Operator.Type.LessThan -> parseUserInputCallStatement()
            else -> throw UnexpectedTokenException("Expected statement, but got $token")
        }

        is Token.Symbol -> parseInSequence(
            ::parseFunctionDeclarationStatement,
            ::parseFunctionCallStatement,
            ::parseAssignmentStatement,
        )

        is Token.Keyword -> parseInSequence(
            ::parseIfConditionStatement,
            ::parseReturnStatement,
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
        val userOutputStartingToken = expectToken<Token.Operator> {
            it.type == Token.Operator.Type.GraterThan
        }
        val stringLiteralToken = expectToken<Token.StringLiteral>()
        expectEOForEOL()

        return UserOutputCallStatement(userOutputStartingToken, stringLiteralToken)
    }

    private fun parseUserInputCallStatement(): UserInputCallStatement {
        val userInputStartingToken = expectToken<Token.Operator> {
            it.type == Token.Operator.Type.LessThan
        }
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

    private fun parseIfConditionStatement(): IfConditionStatement {
        val ifToken = expectToken<Token.Keyword> { it.value == "if" }
        val expression = parseExpression(parseUntilCondition = { it == Token.CurlyBracketOpen })
            ?: throw UnexpectedTokenException("Expected expression, but got ${getToken()}")

        val openCurlyToken = expectToken<Token.CurlyBracketOpen>()
        val body = parseListOfStatements(parseUntilCondition = { it != Token.CurlyBracketClose })
        val closeCurlyToken = expectToken<Token.CurlyBracketClose>()

        if (getToken().let { it !is Token.Keyword || it.value != "else" }) {
            expectEOForEOL()

            return IfConditionStatement(
                ifToken = ifToken,
                conditionExpression = expression,
                openCurlyToken = openCurlyToken,
                closeCurlyToken = closeCurlyToken,
                body = body,
                elseIfConditionStatements = null,
                elseStatement = null,
            )
        }

        var elseToken: Token.Keyword? = expectToken<Token.Keyword> { it.value == "else" }
        val elseIfConditionStatements = mutableListOf<ElseIfConditionStatement>()

        while (getToken().let { it is Token.Keyword && it.value == "if" }) {
            val elseIfToken = expectToken<Token.Keyword> { it.value == "if" }
            val elseIfExpression =
                parseExpression(parseUntilCondition = { it == Token.CurlyBracketOpen })
                    ?: throw UnexpectedTokenException("Expected expression, but got ${getToken()}")
            val elseIfOpenCurlyToken = expectToken<Token.CurlyBracketOpen>()
            val elseIfBody =
                parseListOfStatements(parseUntilCondition = { it != Token.CurlyBracketClose })
            val elseIfCloseCurlyToken = expectToken<Token.CurlyBracketClose>()
            elseIfConditionStatements.add(
                ElseIfConditionStatement(
                    elseIfToken = requireNotNull(elseToken) to elseIfToken,
                    elseIfConditionExpression = elseIfExpression,
                    elseIfOpenCurlyToken = elseIfOpenCurlyToken,
                    elseIfCloseCurlyToken = elseIfCloseCurlyToken,
                    elseIfBody = elseIfBody,
                ),
            )

            if (getToken().let { it !is Token.Keyword || it.value != "else" }) {
                elseToken = null
                expectEOForEOL()

                break
            }

            elseToken = expectToken<Token.Keyword> { it.value == "else" }
        }

        var elseStatement: ElseStatement? = null
        if (elseToken != null) {
            val elseOpenCurlyToken = expectToken<Token.CurlyBracketOpen>()
            val elseBody =
                parseListOfStatements(parseUntilCondition = { it != Token.CurlyBracketClose })
            val elseCloseCurlyToken = expectToken<Token.CurlyBracketClose>()
            expectEOForEOL()

            elseStatement = ElseStatement(
                elseToken = elseToken,
                elseOpenCurlyToken = elseOpenCurlyToken,
                elseCloseCurlyToken = elseCloseCurlyToken,
                elseBody = elseBody,
            )
        }

        return IfConditionStatement(
            ifToken = ifToken,
            conditionExpression = expression,
            openCurlyToken = openCurlyToken,
            closeCurlyToken = closeCurlyToken,
            body = body,
            elseIfConditionStatements = elseIfConditionStatements.takeIf { it.isNotEmpty() },
            elseStatement = elseStatement,
        )
    }

    private fun parseReturnStatement(): ReturnStatement {
        val returnToken = expectToken<Token.Keyword> { it.value == "return" }
        if (getToken().let { it == Token.EOL || it == Token.EOF }) {
            expectEOForEOL()
            return ReturnStatement(returnToken, null)
        }

        val returnValue = parseExpression()
            ?: throw UnexpectedTokenException("Expected expression after $returnToken")
        expectEOForEOL()

        return ReturnStatement(returnToken, returnValue)
    }

    private fun parseExpression(
        minBindingPower: Float = 0f,
        parseUntilCondition: (Token) -> Boolean = { it == Token.EOL || it == Token.EOF },
    ): Expression? {
        val token = getToken()
        var lhs = when (token) {
            is Token.Symbol -> parseInSequence(
                { parseFunctionCallExpression() },
                { SymbolExpression(token) },
            ).also { advance() }

            is Token.NumberLiteral -> NumberLiteralExpression(token).also { advance() }
            is Token.BooleanLiteral -> BooleanLiteralExpression(token).also { advance() }
            is Token.StringLiteral -> StringLiteralExpression(token).also { advance() }
            is Token.Operator -> {
                val rbp = getPrefixBindingPowers(token)
                advance()
                val expression = parseExpression(rbp)
                if (expression == null) {
                    throw UnexpectedTokenException("Expected expression, but got $token")
                }

                UnaryExpression(token, expression)
            }

            Token.BracketOpen -> {
                advance()
                val expression = parseExpression(0f) { it == Token.BracketClose }
                if (expression == null) {
                    throw UnexpectedTokenException("Expected expression inside parentheses, but got ${getToken()}")
                }
                expectToken<Token.BracketClose>()
                ParenthesisedExpression(expression)
            }

            else -> null
        } ?: throw UnexpectedTokenException("Expected expression, but got $token")

        if (parseUntilCondition(getToken())) {
            return lhs
        }

        do {
            val currentToken = getToken()

            if (
                parseUntilCondition(currentToken) ||
                currentToken == Token.BracketClose ||
                currentToken !is Token.Operator
            ) {
                break
            }

            val (lbp, rbp) = getInfixBindingPowers(currentToken)
            if (lbp <= minBindingPower) {
                break
            }

            if (currentToken == Token.BracketClose) {
                throw UnexpectedTokenException("Unmatched closing parenthesis")
            }

            advance()

            val rhs = parseExpression(rbp)
            if (rhs == null) {
                throw UnexpectedTokenException(
                    "Expected expression after operator $currentToken, but got ${getToken()}",
                )
            }

            lhs = BinaryExpression(lhs, currentToken, rhs)
        } while (!parseUntilCondition(getToken()))

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

    private fun getInfixBindingPowers(token: Token): Pair<Float, Float> = when (token) {
        is Token.Operator -> when (token.type) {
            Token.Operator.Type.Or -> 1f to 2f
            Token.Operator.Type.And -> 3f to 4f
            Token.Operator.Type.Equals, Token.Operator.Type.NotEquals -> 5f to 6f
            Token.Operator.Type.GraterThan, Token.Operator.Type.LessThan,
            Token.Operator.Type.GraterThanOrEquals, Token.Operator.Type.LessThanOrEquals,
                -> 7f to 8f

            Token.Operator.Type.Add, Token.Operator.Type.Subtract -> 9f to 10f
            Token.Operator.Type.Multiply, Token.Operator.Type.Divide -> 12f to 11f
            Token.Operator.Type.Exponent -> 13f to 14f
            Token.Operator.Type.Range -> 15f to 16f
            else -> throw UnexpectedTokenException("Unexpected operator $token")
        }

        Token.BracketClose -> 0f to 0f

        else -> throw UnexpectedTokenException("Unexpected token $token")
    }


    private fun getPrefixBindingPowers(token: Token): Float = when (token) {
        is Token.Operator -> when (token.type) {
            Token.Operator.Type.Not -> 11f
            Token.Operator.Type.Add, Token.Operator.Type.Subtract -> 9.5f
            else -> throw UnexpectedTokenException("Unexpected operator $token")
        }

        Token.BracketOpen -> 0.5f
        else -> throw UnexpectedTokenException("Unexpected token $token")
    }
}