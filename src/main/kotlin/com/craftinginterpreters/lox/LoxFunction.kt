package com.craftinginterpreters.lox

class LoxFunction(private val declaration: Stmt.Function, private val closure: Environment) : LoxCallable {
    override val arity: Int = declaration.params.size

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val environment = Environment(closure)
        for (i in declaration.params.indices) {
            environment.define(declaration.params[i].lexeme, arguments[i])
        }

        try {
            interpreter.executeBlock(declaration.body, environment)
        } catch (returnValue: Return) {
            return returnValue.value
        }
        return null
    }

    override fun toString(): String {
        if (declaration.name != null) {
            return "<fn ${declaration.name.lexeme}>"
        } else {
            return "<fn LoxAnonymous"
        }
    }
}