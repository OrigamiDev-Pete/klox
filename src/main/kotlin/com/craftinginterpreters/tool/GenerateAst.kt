package com.craftinginterpreters.tool

import java.io.PrintWriter
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size != 1) {
        System.err.println("Usage: generate_ast <output directory>")
        exitProcess(64)
    }
    val outputDir = args[0]

    defineAst(outputDir, "Expr", listOf(
        "Assign   : val name: Token, val value: Expr",
        "Binary   : val left: Expr, val operator: Token, val right: Expr",
        "Call     : val callee: Expr, val paren: Token, val arguments: List<Expr>",
        "Grouping : val expression: Expr",
        "Literal  : val value: Any?",
        "Logical  : val left: Expr, val operator: Token, val right: Expr",
        "Unary    : val operator: Token, val right: Expr",
        "Variable : val name: Token",
        "Function : val params: List<Token>, val body: List<Stmt?>"
    ))

    defineAst(outputDir, "Stmt", listOf(
        "Block      : val statements: List<Stmt>",
        "Expression : val expression: Expr",
        "If         : val condition: Expr, val thenBranch: Stmt, val elseBranch: Stmt?",
        "Function   : val name: Token, val params: List<Token>, val body: List<Stmt?>",
        "Print      : val expression: Expr",
        "Return     : val keyword: Token, val value: Expr?",
        "Var        : val name: Token, val initializer: Expr?",
        "While      : val condition: Expr, val body: Stmt",
    ))
}

fun defineAst(outputDir: String, baseName: String, types: List<String>) {

    val path = "$outputDir/$baseName.kt"
    val writer = PrintWriter(path, "UTF-8")

     writer.println("""
        package com.craftinginterpreters.lox
        
        """.trimIndent()
     )
    writer.println("""
        abstract class $baseName {
            interface Visitor<R> {""".trimIndent())
    for (type in types) {
        val typeName = type.split(':')[0].trim()
        writer.println("        fun visit$typeName$baseName(${baseName.lowercase()}: $typeName): R")
    }
    writer.println("    }")
    writer.println()
    writer.println("    abstract fun <R> accept(visitor: Visitor<R>): R")
    writer.println()

    for (type in types) {
        val definition = type.split(":", ignoreCase = false, limit = 2)
        val className = definition[0].trim()
        val fields = definition[1].trim()
        writer.println("""
            |   class $className($fields) : $baseName() {
            |       override fun <R> accept(visitor: Visitor<R>): R {
            |           return visitor.visit$className$baseName(this)
            |       }
            |   }
            |
        """.trimMargin())
    }
    writer.println("}")
    writer.close()
}