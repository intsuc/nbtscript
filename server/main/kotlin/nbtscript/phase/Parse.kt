package nbtscript.phase

import nbtscript.ast.Surface.Root
import nbtscript.ast.Surface.Term
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range

@Suppress("NOTHING_TO_INLINE")
class Parse private constructor(
    private val context: Phase.Context,
    private val text: String,
) {
    private var cursor: Int = 0
    private var line: Int = 0
    private var character: Int = 0

    private fun parseRoot(): Root = ranged {
        val body = parseTerm()
        val root = Root(body, range())
        skipWhitespace()
        if (cursor != text.length) {
            val end = position()
            context.addDiagnostic(endOfFileExpected(Range(end, end)))
        }
        root
    }

    private fun parseTerm(): Term = ranged {
        when (peek()) {
            '(' -> {
                skip()
                val left = parseTerm1()
                when (peek()) {
                    ':' -> {
                        skip()
                        val type = parseTerm()
                        expect(')')
                        when (peek()) {
                            '=' -> {
                                skip()
                                expect('>')
                                val body = parseTerm()
                                when (left) {
                                    is Term.Var -> Term.Abs(left.name, type, body, range())
                                    else -> hole()
                                }
                            }

                            '-' -> {
                                skip()
                                expect('>')
                                val cod = parseTerm()
                                when (left) {
                                    is Term.Var -> Term.FunctionType(left.name, type, cod, range())
                                    else -> hole()
                                }
                            }

                            else -> hole()
                        }
                    }

                    ')' -> {
                        skip()
                        left
                    }

                    else -> hole()
                }
            }

            else -> {
                val left = parseTerm1()
                when (peek()) {
                    '.' -> {
                        skip()
                        expect('[')
                        val index = parseTerm()
                        expect(']')
                        Term.IndexedElement(left, index, range())
                    }

                    '-' -> {
                        skip()
                        expect('>')
                        val cod = parseTerm()
                        Term.FunctionType(null, left, cod, range())
                    }

                    '(' -> {
                        skip()
                        val operand = parseTerm()
                        expect(')')
                        Term.Apply(left, operand, range())
                    }

                    else -> left
                }
            }
        }
    }

    private fun parseTerm1(): Term = ranged {
        when (val start = peek()) {
            '(' -> {
                skip()
                val term = parseTerm()
                expect(')')
                term
            }

            '"', '\'' -> parseStringTag()
            '[' -> {
                skip()
                when (text.getOrNull(cursor)) {
                    'B' -> {
                        skip()
                        expect(';')
                        val elements = parseList(']') { parseTerm() }
                        Term.ByteArrayTag(elements, range())
                    }

                    'I' -> {
                        skip()
                        expect(';')
                        val elements = parseList(']') { parseTerm() }
                        Term.IntArrayTag(elements, range())
                    }

                    'L' -> {
                        skip()
                        expect(';')
                        val elements = parseList(']') { parseTerm() }
                        Term.LongArrayTag(elements, range())
                    }

                    else -> {
                        val elements = parseList(']') { parseTerm() }
                        Term.ListTag(elements, range())
                    }
                }
            }

            '{' -> {
                skip()
                val elements = parseList('}') {
                    val key = parseWord()
                    expect(':')
                    val value = parseTerm()
                    key to value
                }.toMap()
                Term.CompoundTag(elements, range())
            }

            '`' -> {
                skip()
                val element = parseTerm()
                Term.Quote(element, range())
            }

            '$' -> {
                skip()
                val element = parseTerm()
                Term.Splice(element, range())
            }

            null -> hole()
            else -> {
                val word = readString()
                if (start.isNumericStart()) {
                    when {
                        word.endsWith('b') -> Term.ByteTag(word.dropLast(1).toByte(), range())
                        word.endsWith('s') -> Term.ShortTag(word.dropLast(1).toShort(), range())
                        word.endsWith('L') -> Term.LongTag(word.dropLast(1).toLong(), range())
                        word.endsWith('f') -> Term.FloatTag(word.dropLast(1).toFloat(), range())
                        word.endsWith('d') -> Term.DoubleTag(word.dropLast(1).toDouble(), range())
                        else -> {
                            when (val int = word.toIntOrNull()) {
                                null -> hole()
                                else -> Term.IntTag(int, range())
                            }
                        }
                    }
                } else {
                    when (word) {
                        "universe" -> Term.UniverseType(range())
                        "end" -> Term.EndType(range())
                        "byte" -> Term.ByteType(range())
                        "short" -> Term.ShortType(range())
                        "int" -> Term.IntType(range())
                        "long" -> Term.LongType(range())
                        "float" -> Term.FloatType(range())
                        "double" -> Term.DoubleType(range())
                        "string" -> Term.StringType(range())
                        "byte_array" -> Term.ByteArrayType(range())
                        "int_array" -> Term.IntArrayType(range())
                        "long_array" -> Term.LongArrayType(range())
                        "list" -> {
                            val element = parseTerm()
                            Term.ListType(element, range())
                        }

                        "compound" -> {
                            expect('{')
                            val elements = parseList('}') {
                                val key = parseWord()
                                expect(':')
                                val value = parseTerm()
                                key to value
                            }.toMap()
                            Term.CompoundType(elements, range())
                        }

                        "code" -> {
                            val element = parseTerm()
                            Term.CodeType(element, range())
                        }

                        "type" -> Term.TypeType(range())
                        "let" -> {
                            val name = parseWord()
                            val anno = when (peek()) {
                                ':' -> {
                                    skip()
                                    parseTerm()
                                }

                                else -> null
                            }
                            expect('=')
                            val init = parseTerm()
                            expect(';')
                            val next = parseTerm()
                            Term.Let(name, anno, init, next, range())
                        }

                        "function" -> {
                            val name = parseWord()
                            val anno = when (peek()) {
                                ':' -> {
                                    skip()
                                    parseTerm()
                                }

                                else -> null
                            }
                            expect('=')
                            val body = parseTerm()
                            expect(';')
                            val next = parseTerm()
                            Term.Function(name, anno, body, next, range())
                        }

                        "" -> hole()
                        else -> Term.Var(word, range())
                    }
                }
            }
        }
    }

    // TODO: improve error reporting and locality
    private fun parseStringTag(): Term = ranged {
        val builder = StringBuilder()
        val quote = peek()
        skip()
        var escaped = false
        while (canRead()) {
            val char = peek()
            skip()
            if (escaped) {
                when (char) {
                    '\\', quote -> {
                        builder.append(char)
                        escaped = false
                    }

                    else -> return@ranged hole()
                }
            } else {
                when (char) {
                    '\\' -> escaped = true
                    quote -> return@ranged Term.StringTag(builder.toString(), range())
                    else -> builder.append(char)
                }
            }
        }
        hole()
    }

    private fun RangeContext.hole(): Term {
        val range = range()
        context.addDiagnostic(termExpected(range))
        return Term.Hole(range)
    }

    private inline fun <A> parseList(close: Char, element: () -> A): List<A> {
        val elements = mutableListOf<A>()
        while (true) {
            when (peek()) {
                null, close -> break
            }
            elements += element()
            when (peek()) {
                null, close -> {}
                else -> expect(',')
            }
        }
        expect(close)
        return elements
    }

    private fun parseWord(): String = ranged {
        val word = readString()
        if (word.isEmpty()) context.addDiagnostic(wordExpected(range()))
        word
    }

    private fun readString(): String {
        skipWhitespace()
        val start = cursor
        while (text.getOrNull(cursor)?.isWordPart() == true) skip()
        return text.substring(start, cursor)
    }

    private inline fun Char.isNumericStart(): Boolean = when (this) {
        '+', '-', in '0'..'9' -> true
        else -> false
    }

    private inline fun Char.isWordPart(): Boolean = when (this) {
        '"', '\'', '(', ')', ',', '.', ':', ';', '=', '[', '\\', ']', '{', '}' -> false
        else -> !isWhitespace()
    }

    private inline fun <A> ranged(block: RangeContext.() -> A): A {
        skipWhitespace()
        return RangeContext().block()
    }

    private inline fun expect(expected: Char) {
        if (peek() == expected) skip()
        else {
            val position = position()
            context.addDiagnostic(charExpected(expected, Range(position, position)))
        }
    }

    private inline fun peek(): Char? {
        skipWhitespace()
        return text.getOrNull(cursor)
    }

    private tailrec fun skipWhitespace() {
        if (!canRead()) return
        when (val char = text[cursor]) {
            '\n' -> breakLine()
            '\r' -> {
                if (!canRead(1)) return
                when (text[cursor + 1]) {
                    '\n' -> {
                        cursor += 1
                        breakLine()
                    }

                    else -> breakLine()
                }
            }

            else -> {
                if (char.isWhitespace()) skip()
                else return
            }
        }
        skipWhitespace()
    }

    private inline fun breakLine() {
        cursor += 1
        line += 1
        character = 0
    }

    private inline fun skip(size: Int = 1) {
        cursor += size
        character += size
    }

    private inline fun canRead(offset: Int = 0): Boolean = cursor + offset <= text.lastIndex

    private inline fun position(): Position = Position(line, character)

    private inner class RangeContext {
        private val start: Position = position()
        inline fun range(): Range = Range(start, position())
    }

    companion object : Phase<String, Root> {
        override operator fun invoke(
            context: Phase.Context,
            input: String,
        ): Root = Parse(context, input).parseRoot()
    }
}
