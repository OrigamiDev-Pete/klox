package com.craftinginterpreters.lox

import kotlin.collections.HashMap

class Resolver(private val interpreter: Interpreter) : Expr.Visitor<Unit>, Stmt.Visitor<Unit> {
    private val scopes = ArrayDeque<MutableMap<String, Boolean>>()
    private var currentFunction = FunctionType.NONE
    private var currentClass = ClassType.NONE

    fun resolve(statements: List<Stmt?>) {
        for (statement in statements) {
            if (statement != null) {
                resolve(statement)
            }
        }
    }

    override fun visitAssignExpr(expr: Expr.Assign) {
        resolve(expr.value)
        resolveLocal(expr, expr.name)
    }

    override fun visitBinaryExpr(expr: Expr.Binary) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitCallExpr(expr: Expr.Call) {
        resolve(expr.callee)

        for (argument in expr.arguments) {
            resolve(argument)
        }
    }

    override fun visitGetExpr(expr: Expr.Get) {
        resolve(expr.obj)
    }

    override fun visitGroupingExpr(expr: Expr.Grouping) {
        resolve(expr.expression)
    }

    override fun visitLiteralExpr(expr: Expr.Literal) {

    }

    override fun visitLogicalExpr(expr: Expr.Logical) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitSetExpr(expr: Expr.Set) {
        resolve(expr.value)
        resolve(expr.obj)
    }

    override fun visitSuperExpr(expr: Expr.Super) {
        if (currentClass == ClassType.NONE) {
            Lox.error(expr.keyword, "Can't use 'super' outside of a class.")
        } else if (currentClass != ClassType.SUBCLASS) {
            Lox.error(expr.keyword, "Can't use 'super' in a class with no superclass.")
        }

        resolveLocal(expr, expr.keyword)
    }

    override fun visitThisExpr(expr: Expr.This) {
        if (currentClass == ClassType.NONE) {
            Lox.error(expr.keyword, "Can't use 'this' outside of a class")
            return
        }
        resolveLocal(expr, expr.keyword)
    }

    override fun visitUnaryExpr(expr: Expr.Unary) {
        resolve(expr.right)
    }

    override fun visitVariableExpr(expr: Expr.Variable) {
        if (!scopes.isEmpty() && scopes.last()[expr.name.lexeme] == false) {
            Lox.error(expr.name, "Can't read local variable in its own initializer")
        }

        resolveLocal(expr, expr.name)
    }

    override fun visitFunctionExpr(expr: Expr.Function) {
        beginScope()
        for (param in expr.params) {
            declare(param)
            define(param)
        }
        resolve(expr.body)
        endScope()
    }

    override fun visitBlockStmt(stmt: Stmt.Block) {
        beginScope()
        resolve(stmt.statements)
        endScope()
    }

    override fun visitClassStmt(stmt: Stmt.Class) {
        val enclosingClass = currentClass
        currentClass = ClassType.CLASS

        declare(stmt.name)
        define(stmt.name)

        if (stmt.superclass != null && stmt.name.lexeme == stmt.superclass.name.lexeme) {
            Lox.error(stmt.superclass.name, "A class can't inherit from itself.")
        }

        if (stmt.superclass != null) {
            currentClass = ClassType.SUBCLASS
            resolve(stmt.superclass)
        }

        if (stmt.superclass != null) {
            beginScope()
            scopes.last()["super"] = true
        }

        beginScope()
        scopes.last()["this"] = true

        for (method in stmt.methods) {
            var declaration = FunctionType.METHOD
            if (method.name!!.lexeme == "init") {
                declaration = FunctionType.INITIALIZER
            }
            resolveFunction(method, declaration)
        }

        endScope()

        if (stmt.superclass != null) endScope()

        currentClass = enclosingClass
    }

    private fun resolve(stmt: Stmt) {
        stmt.accept(this)
    }

    private fun resolve(expr: Expr) {
        expr.accept(this)
    }

    private fun beginScope() {
        scopes.addLast(HashMap())
    }

    private fun endScope() {
        scopes.removeLast()
    }

    private fun declare(name: Token) {
        if (scopes.isEmpty()) return

        val scope = scopes.last()
        if (scope.containsKey(name.lexeme)) {
            Lox.error(name, "Already a variable with this name in this scope.")
        }

        scope[name.lexeme] = false
    }

    private fun define(name: Token) {
        if (scopes.isEmpty()) return
        scopes.last()[name.lexeme] = true
    }

    private fun resolveLocal(expr: Expr, name: Token) {
        for (i in scopes.indices.reversed()) {
            if (scopes[i].containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size - 1 - i)
                return
            }
        }
    }

    private fun resolveFunction(function: Stmt.Function, type: FunctionType) {
        val enclosingFunction = currentFunction
        currentFunction = type
        beginScope()
        for (param in function.params) {
            declare(param)
            define(param)
        }
        resolve(function.body)
        endScope()
        currentFunction = enclosingFunction
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        resolve(stmt.expression)
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        resolve(stmt.condition)
        resolve(stmt.thenBranch)
        if (stmt.elseBranch != null) resolve(stmt.elseBranch)
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        if (stmt.name != null) {
            declare(stmt.name)
            define(stmt.name)
        }
        resolveFunction(stmt, FunctionType.FUNCTION)
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        resolve(stmt.expression)
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        if (currentFunction == FunctionType.NONE) {
            Lox.error(stmt.keyword, "Can't return from top-level code.")
        }
        if (stmt.value != null) {
            if (currentFunction == FunctionType.INITIALIZER) {
                Lox.error(stmt.keyword, "Can't return a value from an initializer")
            }

            resolve(stmt.value)
        }
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        declare(stmt.name)
        if (stmt.initializer != null) {
            resolve(stmt.initializer)
        }
        define(stmt.name)
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        resolve(stmt.condition)
        resolve(stmt.body)
    }

    private enum class FunctionType {
        NONE,
        FUNCTION,
        INITIALIZER,
        METHOD
    }

    private enum class ClassType {
        NONE,
        CLASS,
        SUBCLASS
    }
}