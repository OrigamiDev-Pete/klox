package com.craftinginterpreters.lox

class LoxClass(val name: String, val methods: Map<String, LoxFunction>) : LoxCallable {
    override val arity: Int
        get() {
            val initializer = findMethod("init") as LoxFunction?
            if (initializer == null) return 0
            return initializer.arity
        }

    fun findMethod(name: String): LoxFunction? {
        if (methods.containsKey(name)) {
            return methods[name]
        }

        return null
    }

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any {
        val instance = LoxInstance(this)
        val initializer = findMethod("init")
        initializer?.bind(instance)?.call(interpreter, arguments)

        return instance
    }

    override fun toString() = name
}
