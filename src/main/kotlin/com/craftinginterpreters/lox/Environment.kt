package com.craftinginterpreters.lox

class Environment() {
    var enclosing: Environment? = null
    private val values = HashMap<String, Any?>()

    constructor(enclosing: Environment) : this() {
        this.enclosing = enclosing
    }


    fun define(name: String, value: Any?) {
        values[name] = value
    }

    fun get(name: Token): Any? {
        if (values.containsKey(name.lexeme)) {
            return values.getValue(name.lexeme)
        }

        if (enclosing != null) {
            return enclosing!!.get(name)
        }

        throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
    }

    fun assign(name: Token, value: Any?) {
        if (values.containsKey(name.lexeme)) {
            values[name.lexeme] = value
            return
        }

        if (enclosing != null) {
            enclosing!!.assign(name, value)
            return
        }

        throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
    }
}