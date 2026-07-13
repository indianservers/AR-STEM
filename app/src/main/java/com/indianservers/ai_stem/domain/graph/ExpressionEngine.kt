package com.indianservers.ai_stem.domain.graph

import kotlin.math.*

data class ParseError(val message: String, val position: Int)
data class EvaluationResult(val value: Double?, val error: String? = null) {
    val isSuccess: Boolean get() = error == null && value != null && value.isFinite()
}

private enum class TokenType { Number, Identifier, Operator, LeftParen, RightParen, Comma, End }
private data class Token(val type: TokenType, val text: String, val position: Int)

class MathExpressionParser {
    fun parse(source: String): ParseOutcome {
        val expressionSource = extractRightHandExpression(source)
        val tokens = tokenize(expressionSource)
        val parser = Parser(tokens)
        return try {
            val ast = parser.parseExpression()
            parser.expectEnd()
            ParseOutcome.Success(ast, ast.variables())
        } catch (error: ParserException) {
            ParseOutcome.Failure(error.message ?: "This formula is incomplete.", error.position)
        }
    }

    private fun tokenize(source: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < source.length) {
            val c = source[i]
            when {
                c.isWhitespace() -> i++
                c.isDigit() || c == '.' -> {
                    val start = i
                    i++
                    while (i < source.length && (source[i].isDigit() || source[i] == '.')) i++
                    tokens += Token(TokenType.Number, source.substring(start, i), start)
                }
                c.isLetter() || c == '_' -> {
                    val start = i
                    i++
                    while (i < source.length && (source[i].isLetterOrDigit() || source[i] == '_')) i++
                    tokens += Token(TokenType.Identifier, source.substring(start, i), start)
                }
                c == '(' -> {
                    tokens += Token(TokenType.LeftParen, "(", i)
                    i++
                }
                c == ')' -> {
                    tokens += Token(TokenType.RightParen, ")", i)
                    i++
                }
                c == ',' -> {
                    tokens += Token(TokenType.Comma, ",", i)
                    i++
                }
                c in "+-*/^%!" -> {
                    tokens += Token(TokenType.Operator, c.toString(), i)
                    i++
                }
                else -> throw ParserException("This formula contains an unsupported symbol.", i)
            }
        }
        tokens += Token(TokenType.End, "", source.length)
        return insertImplicitMultiplication(tokens)
    }

    private fun insertImplicitMultiplication(tokens: List<Token>): List<Token> {
        val out = mutableListOf<Token>()
        tokens.zipWithNext().forEach { (a, b) ->
            out += a
            val leftValue = a.type == TokenType.Number || a.type == TokenType.Identifier || a.type == TokenType.RightParen
            val rightValue = b.type == TokenType.Number || b.type == TokenType.Identifier || b.type == TokenType.LeftParen
            val functionCall = a.type == TokenType.Identifier && b.type == TokenType.LeftParen
            if (leftValue && rightValue && !functionCall && b.type != TokenType.End) {
                out += Token(TokenType.Operator, "*", b.position)
            }
        }
        out += tokens.last()
        return out
    }

    private class Parser(private val tokens: List<Token>) {
        private var index = 0
        private val current: Token get() = tokens[index]

        fun parseExpression(): MathExpressionNode = parseAddSub()

        fun expectEnd() {
            if (current.type != TokenType.End) throw ParserException("Check the expression near '${current.text}'.", current.position)
        }

        private fun parseAddSub(): MathExpressionNode {
            var node = parseMulDiv()
            while (current.type == TokenType.Operator && current.text in listOf("+", "-")) {
                val op = if (advance().text == "+") BinaryOperator.Add else BinaryOperator.Subtract
                node = MathExpressionNode.BinaryOperation(op, node, parseMulDiv())
            }
            return node
        }

        private fun parseMulDiv(): MathExpressionNode {
            var node = parseUnary()
            while (current.type == TokenType.Operator && current.text in listOf("*", "/", "%")) {
                val op = when (advance().text) {
                    "*" -> BinaryOperator.Multiply
                    "/" -> BinaryOperator.Divide
                    else -> BinaryOperator.Modulo
                }
                node = MathExpressionNode.BinaryOperation(op, node, parseUnary())
            }
            return node
        }

        private fun parsePower(): MathExpressionNode {
            val base = parsePostfix()
            return if (current.type == TokenType.Operator && current.text == "^") {
                advance()
                MathExpressionNode.BinaryOperation(BinaryOperator.Power, base, parseUnary())
            } else {
                base
            }
        }

        private fun parseUnary(): MathExpressionNode =
            if (current.type == TokenType.Operator && current.text in listOf("+", "-")) {
                val op = if (advance().text == "-") UnaryOperator.Negative else UnaryOperator.Positive
                MathExpressionNode.UnaryOperation(op, parsePower())
            } else {
                parsePower()
            }

        private fun parsePostfix(): MathExpressionNode {
            var node = parsePrimary()
            while (current.type == TokenType.Operator && current.text == "!") {
                advance()
                node = MathExpressionNode.UnaryOperation(UnaryOperator.Factorial, node)
            }
            return node
        }

        private fun parsePrimary(): MathExpressionNode =
            when (current.type) {
                TokenType.Number -> {
                    val token = advance()
                    MathExpressionNode.Constant(token.text.toDoubleOrNull() ?: throw ParserException("This number is not valid.", token.position))
                }
                TokenType.Identifier -> {
                    val name = advance().text
                    if (current.type == TokenType.LeftParen) {
                        advance()
                        val args = mutableListOf<MathExpressionNode>()
                        if (current.type != TokenType.RightParen) {
                            do {
                                args += parseExpression()
                            } while (match(TokenType.Comma))
                        }
                        expect(TokenType.RightParen, "Check the bracket near '$name('.")
                        MathExpressionNode.FunctionCall(name.lowercase(), args)
                    } else {
                        MathExpressionNode.Variable(name.lowercase())
                    }
                }
                TokenType.LeftParen -> {
                    advance()
                    val node = parseExpression()
                    expect(TokenType.RightParen, "Check the bracket near this expression.")
                    node
                }
                else -> throw ParserException("This formula is incomplete.", current.position)
            }

        private fun match(type: TokenType): Boolean =
            if (current.type == type) {
                advance()
                true
            } else {
                false
            }

        private fun expect(type: TokenType, message: String) {
            if (!match(type)) throw ParserException(message, current.position)
        }

        private fun advance(): Token = tokens[index++]
    }
}

class MathExpressionEvaluator {
    fun evaluate(node: MathExpressionNode, variables: Map<String, Double>): EvaluationResult =
        try {
            val value = eval(node, variables)
            if (value.isFinite()) EvaluationResult(value) else EvaluationResult(null, "The graph is not defined at some points.")
        } catch (error: ArithmeticException) {
            EvaluationResult(null, error.message ?: "This value is not defined.")
        } catch (error: IllegalArgumentException) {
            EvaluationResult(null, error.message ?: "This formula could not be evaluated.")
        }

    private fun eval(node: MathExpressionNode, variables: Map<String, Double>): Double =
        when (node) {
            is MathExpressionNode.Constant -> node.value
            is MathExpressionNode.Variable -> variables[node.name] ?: throw IllegalArgumentException("Add a value for ${node.name}.")
            is MathExpressionNode.UnaryOperation -> {
                val value = eval(node.operand, variables)
                when (node.operation) {
                    UnaryOperator.Positive -> value
                    UnaryOperator.Negative -> -value
                    UnaryOperator.Factorial -> factorial(value)
                }
            }
            is MathExpressionNode.BinaryOperation -> {
                val left = eval(node.left, variables)
                val right = eval(node.right, variables)
                when (node.operation) {
                    BinaryOperator.Add -> left + right
                    BinaryOperator.Subtract -> left - right
                    BinaryOperator.Multiply -> left * right
                    BinaryOperator.Divide -> if (right == 0.0) Double.NaN else left / right
                    BinaryOperator.Power -> if (abs(right) > 1000.0) Double.NaN else left.pow(right)
                    BinaryOperator.Modulo -> left.mod(right)
                }
            }
            is MathExpressionNode.FunctionCall -> {
                if (node.name == "sum" || node.name == "product") {
                    aggregate(node.name, node.arguments, variables)
                } else {
                    call(node.name, node.arguments.map { eval(it, variables) })
                }
            }
            is MathExpressionNode.Conditional -> if (evalBoolean(node.condition, variables)) eval(node.whenTrue, variables) else node.whenFalse?.let { eval(it, variables) } ?: Double.NaN
        }

    private fun evalBoolean(node: BooleanExpressionNode, variables: Map<String, Double>): Boolean =
        when (node) {
            is BooleanExpressionNode.And -> evalBoolean(node.left, variables) && evalBoolean(node.right, variables)
            is BooleanExpressionNode.Or -> evalBoolean(node.left, variables) || evalBoolean(node.right, variables)
            is BooleanExpressionNode.Comparison -> {
                val left = eval(node.left, variables)
                val right = eval(node.right, variables)
                when (node.operation) {
                    ComparisonOperator.LessThan -> left < right
                    ComparisonOperator.LessThanOrEqual -> left <= right
                    ComparisonOperator.GreaterThan -> left > right
                    ComparisonOperator.GreaterThanOrEqual -> left >= right
                    ComparisonOperator.Equal -> abs(left - right) < 1e-9
                    ComparisonOperator.NotEqual -> abs(left - right) >= 1e-9
                }
            }
        }

    private fun call(name: String, args: List<Double>): Double =
        when (name) {
            "sin" -> sin(one(name, args))
            "cos" -> cos(one(name, args))
            "tan" -> tan(one(name, args))
            "sec" -> 1.0 / cos(one(name, args))
            "csc" -> 1.0 / sin(one(name, args))
            "cot" -> 1.0 / tan(one(name, args))
            "asin" -> asin(one(name, args))
            "acos" -> acos(one(name, args))
            "atan" -> atan(one(name, args))
            "atan2" -> atan2(args.getOrElse(0) { 0.0 }, args.getOrElse(1) { 1.0 })
            "sinh" -> sinh(one(name, args))
            "cosh" -> cosh(one(name, args))
            "tanh" -> tanh(one(name, args))
            "sqrt" -> sqrt(one(name, args))
            "cbrt" -> cbrt(one(name, args))
            "abs" -> abs(one(name, args))
            "exp" -> exp(one(name, args))
            "ln" -> ln(one(name, args))
            "log" -> log10(one(name, args))
            "floor" -> floor(one(name, args))
            "ceil" -> ceil(one(name, args))
            "round" -> round(one(name, args))
            "sign" -> sign(one(name, args))
            "min" -> args.minOrNull() ?: Double.NaN
            "max" -> args.maxOrNull() ?: Double.NaN
            "mod" -> args.getOrElse(0) { 0.0 }.mod(args.getOrElse(1) { 1.0 })
            "gcd" -> gcd(args.getOrElse(0) { 0.0 }.toLong(), args.getOrElse(1) { 0.0 }.toLong()).toDouble()
            "lcm" -> lcm(args.getOrElse(0) { 0.0 }.toLong(), args.getOrElse(1) { 0.0 }.toLong()).toDouble()
            "combinations", "ncr" -> combinations(args.getOrElse(0) { 0.0 }, args.getOrElse(1) { 0.0 })
            "permutations", "npr" -> permutations(args.getOrElse(0) { 0.0 }, args.getOrElse(1) { 0.0 })
            else -> throw IllegalArgumentException("The function $name is not supported yet.")
        }

    private fun aggregate(name: String, args: List<MathExpressionNode>, variables: Map<String, Double>): Double {
        require(args.size == 4) { "$name needs expression, variable, start and end." }
        val variable = (args[1] as? MathExpressionNode.Variable)?.name
            ?: throw IllegalArgumentException("$name needs a variable name as its second value.")
        val start = eval(args[2], variables).toInt()
        val end = eval(args[3], variables).toInt()
        require(end >= start && end - start <= 10_000) { "$name range is too large." }
        return (start..end).fold(if (name == "sum") 0.0 else 1.0) { acc, value ->
            val next = eval(args[0], variables + (variable to value.toDouble()))
            if (name == "sum") acc + next else acc * next
        }
    }

    private fun one(name: String, args: List<Double>): Double {
        require(args.size == 1) { "$name needs one value." }
        return args.first()
    }

    private fun factorial(value: Double): Double {
        if (value < 0 || value > 170 || value % 1.0 != 0.0) return Double.NaN
        return (1..value.toInt()).fold(1.0) { acc, n -> acc * n }
    }

    private fun combinations(nRaw: Double, rRaw: Double): Double {
        val n = nRaw.toInt()
        val r = rRaw.toInt()
        if (n < 0 || r < 0 || r > n) return Double.NaN
        return factorial(n.toDouble()) / (factorial(r.toDouble()) * factorial((n - r).toDouble()))
    }

    private fun permutations(nRaw: Double, rRaw: Double): Double {
        val n = nRaw.toInt()
        val r = rRaw.toInt()
        if (n < 0 || r < 0 || r > n) return Double.NaN
        return factorial(n.toDouble()) / factorial((n - r).toDouble())
    }

    private fun gcd(aRaw: Long, bRaw: Long): Long {
        var a = abs(aRaw)
        var b = abs(bRaw)
        while (b != 0L) {
            val next = a % b
            a = b
            b = next
        }
        return a
    }

    private fun lcm(a: Long, b: Long): Long = if (a == 0L || b == 0L) 0 else abs(a / gcd(a, b) * b)
}

private fun MathExpressionNode.variables(): Set<String> =
    when (this) {
        is MathExpressionNode.Constant -> emptySet()
        is MathExpressionNode.Variable -> setOf(name)
        is MathExpressionNode.UnaryOperation -> operand.variables()
        is MathExpressionNode.BinaryOperation -> left.variables() + right.variables()
        is MathExpressionNode.FunctionCall -> arguments.flatMap { it.variables() }.toSet()
        is MathExpressionNode.Conditional -> whenTrue.variables() + (whenFalse?.variables() ?: emptySet())
    }

private class ParserException(message: String, val position: Int) : IllegalArgumentException(message)
