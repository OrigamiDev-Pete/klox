package com.craftinginterpreters.lox

import com.craftinginterpreters.lox.TokenType.*

class Parser(private val tokens: List<Token>) {
    companion object {
        private class ParseError : java.lang.RuntimeException()
    }

    private var current = 0

    fun parse(): Expr? {
        try {
            return expression()
        } catch (error: ParseError) {
            return null
        }
    }

    private fun expression(): Expr {
        return equality()
    }

    private fun equality(): Expr {
        return leftAssociativeExpression(::comparison, BANG_EQUAL, EQUAL_EQUAL)
    }

    private fun comparison(): Expr {
        return leftAssociativeExpression(::term, GREATER, GREAT_EQUAL, LESS, LESS_EQUAL)
    }

    private fun term(): Expr {
        return leftAssociativeExpression(::factor, MINUS, PLUS)
    }

    private fun factor(): Expr {
        return leftAssociativeExpression(::unary, SLASH, STAR)
    }

    private fun unary(): Expr {
        if (match(arrayOf(BANG, MINUS))) {
            val operator = previous()
            val right = unary()
            return Unary(operator, right)
        }
        return primary()
    }

    private fun primary(): Expr {
        if (match(FALSE)) return Literal(false)
        if (match(TRUE)) return Literal(true)
        if (match(NIL)) return Literal(null)

        if (match(arrayOf(NUMBER, STRING))) {
            return Literal(previous().literal)
        }

        if (match(LEFT_PAREN)) {
            val expr = expression()
            consume(RIGHT_PAREN, "Expect ')' after expression.")
            return Grouping(expr)
        }

        throw error(peek(), "Expect expression.")
    }

    private fun leftAssociativeExpression(operand: () -> Expr, vararg types: TokenType): Expr {
        var expr = operand()

        while (match(types)) {
            val operator = previous()
            val right = operand()
            expr = Binary(expr, operator, right)
        }
        return expr
    }

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        throw error(peek(), message)
    }

    private fun error(token: Token, message: String): ParseError {
        Lox.error(token, message)
        return ParseError()
    }

    private fun synchronize() {
        advance()

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return

            when (peek().type) {
                CLASS, FUN, VAR, FOR, IF, WHILE, PRINT, RETURN -> return
                else -> advance()
            }
        }
    }

    private fun match(type: TokenType): Boolean {
        if (check(type)) {
            advance()
            return true
        }
        return false
    }

    private fun match(types: Array<out TokenType>): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }


    private fun check(type: TokenType): Boolean {
        if (isAtEnd()) return false
        return peek().type == type;
    }

    private fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }

    private fun isAtEnd() = peek().type == EOF
    private fun peek() = tokens[current]
    private fun previous() = tokens[current - 1]
}