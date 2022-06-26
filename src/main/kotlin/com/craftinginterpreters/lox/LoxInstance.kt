package com.craftinginterpreters.lox

class LoxInstance(val kclass: LoxClass) {
    private val fields = HashMap<String, Any?>()

    fun get(name: Token): Any? {
        if (fields.containsKey(name.lexeme)) {
            return fields[name.lexeme]
        }

        val method = kclass.findMethod(name.lexeme)
        if (method != null) return method.bind(this)

        throw RuntimeError(name, "Undefined property ${name.lexeme}.")
    }

    override fun toString() = "${kclass.name} instance"
    fun set(name: Token, value: Any?) {
        fields[name.lexeme] = value
    }
}
