package com.craftinginterpreters.lox

import com.craftinginterpreters.lox.TokenType.*

class Interpreter : Expr.Visitor<Any?>, Stmt.Visitor<Unit> {
    private var environment = Environment()

    fun interpret(statements: List<Stmt?>) {
        try {
            for (statement in statements) {
                execute(statement)
            }
        } catch (error: RuntimeError) {
            Lox.runtimeError(error)
        }
    }

    override fun visitAssignExpr(expr: Expr.Assign): Any? {
        val value = evaluate(expr.value)
        environment.assign(expr.name, value)
        return value
    }

    override fun visitBinaryExpr(expr: Expr.Binary): Any? {
        val left = evaluate(expr.left)
        val right = evaluate(expr.right)

        when (expr.operator.type) {
            MINUS -> {
                checkNumberOperands(expr.operator, left, right)
                return (left as Double) - (right as Double)
            }
            SLASH -> {
                checkNumberOperands(expr.operator, left, right)
                if ((right as Double) == 0.0) {
                    throw RuntimeError(expr.operator, "Cannot divide by 0.")
                }
                return (left as Double) / right
            }
            STAR -> {
                checkNumberOperands(expr.operator, left, right)
                return (left as Double) * (right as Double)
            }
            PLUS -> {
                if (left is Double && right is Double) {
                    return left + right
                }
                if (left is String && right is String) {
                    return left + right
                }
                if (left is String || right is String) {
                    return stringify(left) + stringify(right)
                }
                throw RuntimeError(expr.operator, "Operands must be a number of a string.")
            }
            GREATER -> {
                checkNumberOperands(expr.operator, left, right)
                return (left as Double) > (right as Double)
            }
            GREAT_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                return (left as Double) >= (right as Double)
            }
            LESS -> {
                checkNumberOperands(expr.operator, left, right)
                return (left as Double) < (right as Double)
            }
            LESS_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                return (left as Double) <= (right as Double)
            }
            BANG_EQUAL -> return !isEqual(left, right)
            EQUAL_EQUAL -> return isEqual(left, right)
            else -> {}
        }
        // Unreachable
        return null
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): Any? {
        return evaluate(expr)
    }

    override fun visitLiteralExpr(expr: Expr.Literal): Any? {
        return expr.value
    }

    override fun visitUnaryExpr(expr: Expr.Unary): Any? {
        val right = evaluate(expr.right)

        when (expr.operator.type) {
            MINUS -> {
                checkNumberOperand(expr.operator, right)
                return -(right as Double)
            }
            BANG -> return !isTruthy(right)
            else -> {}
        }
        // Unreachable
        return null
    }

    override fun visitVariableExpr(expr: Expr.Variable): Any? {
        return environment.get(expr.name)
    }

    private fun checkNumberOperand(operator: Token, operand: Any?) {
        if (operand is Double) return
        throw RuntimeError(operator, "Operand must be a number.")

    }

    private fun checkNumberOperands(operator: Token, left: Any?, right: Any?) {
        if (left is Double && right is Double) return
        throw RuntimeError(operator, "Operands must be a number.")
    }

    private fun isTruthy(obj: Any?): Boolean {
        if (obj == null) return false
        if (obj is Boolean) return obj
        return true
    }

    private fun isEqual(a: Any?, b: Any?): Boolean {
        if (a == null && b == null) return true
        if (a == null) return false

        return a == b
    }

    private fun stringify(obj: Any?): String {
        if (obj == null) return "nil"

        if (obj is Double) {
            var text = obj.toString()
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length - 2)
            }
            return text
        }

        return obj.toString()
    }

    private fun evaluate(expr: Expr): Any? {
        return expr.accept(this)
    }

    private fun execute(stmt: Stmt?) {
        stmt?.accept(this)
    }

    private fun executeBlock(statements: List<Stmt?>, environment: Environment) {
        val previous = this.environment
        try {
            this.environment = environment

            for (statement in statements) {
                execute(statement)
            }
        } finally {
            this.environment = previous
        }
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        val value = evaluate(stmt.expression)
        println(stringify(value))
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        var value: Any? = null
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer)
        }

        environment.define(stmt.name.lexeme, value)
    }

    override fun visitBlockStmt(stmt: Stmt.Block) {
        executeBlock(stmt.statements, Environment(environment))
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        evaluate(stmt.expression)
    }
}