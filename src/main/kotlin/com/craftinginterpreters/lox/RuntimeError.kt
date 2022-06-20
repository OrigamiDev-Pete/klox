package com.craftinginterpreters.lox

class RuntimeError(val token: Token, message: String) : Throwable(message) {

}
