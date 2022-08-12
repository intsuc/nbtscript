package nbtscript.phase

import nbtscript.ast.Surface.*
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range

// TODO: error reporting
@Suppress("NOTHING_TO_INLINE")
class Parse private constructor(
    private val messages: Messages,
    private val text: String,
) {
    private var cursor: Int = 0
    private var line: Int = 0
    private var character: Int = 0

    private fun parseRoot(): Root = ranged {
        try {
            val body = parseTermZ()
            Root(body, range())
        } catch (e: StringIndexOutOfBoundsException) {
            TODO()
        }
    }

    private fun parseTypeZ(): TypeZ = ranged {
        when (parseWord()) {
            "byte" -> TypeZ.ByteZ(range())
            "short" -> TypeZ.ShortZ(range())
            "int" -> TypeZ.IntZ(range())
            "long" -> TypeZ.LongZ(range())
            "float" -> TypeZ.FloatZ(range())
            "double" -> TypeZ.DoubleZ(range())
            "string" -> TypeZ.StringZ(range())
            "byte_array" -> TypeZ.ByteArrayZ(range())
            "int_array" -> TypeZ.IntArrayZ(range())
            "long_array" -> TypeZ.LongArrayZ(range())
            "list" -> {
                val element = parseTypeZ()
                TypeZ.ListZ(element, range())
            }

            "compound" -> {
                expect('{')
                val elements = parseList('}') {
                    val key = parseWord()
                    expect(':')
                    val value = parseTypeZ()
                    key to value
                }.toMap()
                TypeZ.CompoundZ(elements, range())
            }

            else -> TODO()
        }
    }

    private fun parseTermZ(): TermZ = ranged {
        when (val start = peek()) {
            '(' -> {
                skip()
                val term = parseTermZ()
                expect(')')
                term
            }

            '"' -> {
                skip()
                val data = parseWord() // TODO: escape
                expect('"')
                TermZ.StringTag(data, range())
            }

            '[' -> {
                skip()
                when (text[cursor + 1]) {
                    'B' -> {
                        skip()
                        expect(';')
                        val elements = parseList(']') { parseTermZ() }
                        TermZ.ByteArrayTag(elements, range())
                    }

                    'I' -> {
                        skip()
                        expect(';')
                        val elements = parseList(']') { parseTermZ() }
                        TermZ.IntArrayTag(elements, range())
                    }

                    'L' -> {
                        skip()
                        expect(';')
                        val elements = parseList(']') { parseTermZ() }
                        TermZ.LongArrayTag(elements, range())
                    }

                    else -> {
                        val elements = parseList(']') { parseTermZ() }
                        TermZ.ListTag(elements, range())
                    }
                }
            }

            '{' -> {
                skip()
                val elements = parseList('}') {
                    val key = parseWord()
                    expect(':')
                    val value = parseTermZ()
                    key to value
                }.toMap()
                TermZ.CompoundTag(elements, range())
            }

            '$' -> {
                skip()
                val element = parseTermS()
                TermZ.Splice(element, range())
            }

            else -> {
                val word = parseWord()
                if (start.isNumericStart()) {
                    when {
                        word.endsWith('b') -> TermZ.ByteTag(word.dropLast(1).toByte(), range())
                        word.endsWith('s') -> TermZ.ShortTag(word.dropLast(1).toShort(), range())
                        word.endsWith('L') -> TermZ.LongTag(word.dropLast(1).toLong(), range())
                        word.endsWith('f') -> TermZ.FloatTag(word.dropLast(1).toFloat(), range())
                        word.endsWith('d') -> TermZ.DoubleTag(word.dropLast(1).toDouble(), range())
                        else -> TermZ.IntTag(word.toInt(), range())
                    }
                } else {
                    when (word) {
                        "function" -> {
                            val name = parseWord()
                            expect('=')
                            val body = parseTermZ()
                            expect(';')
                            val next = parseTermZ()
                            TermZ.Function(name, body, next, range())
                        }

                        else -> TermZ.Run(word, range())
                    }
                }
            }
        }
    }

    private fun parseTermS(): TermS = ranged {
        when (val start = peek()) {
            '(' -> {
                skip()
                val term = parseTermS()
                expect(')')
                term
            }

            '"' -> {
                skip()
                val data = parseWord() // TODO: escape
                expect('"')
                TermS.StringTag(data, range())
            }

            '[' -> {
                skip()
                when (text[cursor + 1]) {
                    'B' -> {
                        skip()
                        expect(';')
                        val elements = parseList(']') { parseTermS() }
                        TermS.ByteArrayTag(elements, range())
                    }

                    'I' -> {
                        skip()
                        expect(';')
                        val elements = parseList(']') { parseTermS() }
                        TermS.IntArrayTag(elements, range())
                    }

                    'L' -> {
                        skip()
                        expect(';')
                        val elements = parseList(']') { parseTermS() }
                        TermS.LongArrayTag(elements, range())
                    }

                    else -> {
                        val elements = parseList(']') { parseTermS() }
                        TermS.ListTag(elements, range())
                    }
                }
            }

            '{' -> {
                skip()
                val elements = parseList('}') {
                    val key = parseWord()
                    expect(':')
                    val value = parseTermS()
                    key to value
                }.toMap()
                TermS.CompoundTag(elements, range())
            }

            '\\' -> {
                skip()
                val name = parseWord()
                expect(':')
                val anno = parseTermS()
                expect('.')
                val body = parseTermS()
                TermS.Abs(name, anno, body, range())
            }

            '@' -> {
                skip()
                val operator = parseTermS()
                val operand = parseTermS()
                TermS.Apply(operator, operand, range())
            }

            '`' -> {
                val element = parseTermZ()
                TermS.Quote(element, range())
            }

            else -> {
                val word = parseWord()
                if (start.isNumericStart()) {
                    when {
                        word.endsWith('b') -> TermS.ByteTag(word.dropLast(1).toByte(), range())
                        word.endsWith('s') -> TermS.ShortTag(word.dropLast(1).toShort(), range())
                        word.endsWith('L') -> TermS.LongTag(word.dropLast(1).toLong(), range())
                        word.endsWith('f') -> TermS.FloatTag(word.dropLast(1).toFloat(), range())
                        word.endsWith('d') -> TermS.DoubleTag(word.dropLast(1).toDouble(), range())
                        else -> TermS.IntTag(word.toInt(), range())
                    }
                } else {
                    when (word) {
                        "universe" -> TermS.UniverseS(range())
                        "end" -> TermS.EndS(range())
                        "byte" -> TermS.ByteS(range())
                        "short" -> TermS.ShortS(range())
                        "int" -> TermS.IntS(range())
                        "long" -> TermS.LongS(range())
                        "float" -> TermS.FloatS(range())
                        "double" -> TermS.DoubleS(range())
                        "string" -> TermS.StringS(range())
                        "byte_array" -> TermS.ByteArrayS(range())
                        "int_array" -> TermS.IntArrayS(range())
                        "long_array" -> TermS.LongArrayS(range())
                        "list" -> {
                            val element = parseTermS()
                            TermS.ListS(element, range())
                        }

                        "compound" -> {
                            expect('{')
                            val elements = parseList('}') {
                                val key = parseWord()
                                expect(':')
                                val value = parseTermS()
                                key to value
                            }.toMap()
                            TermS.CompoundS(elements, range())
                        }

                        "arrow" -> {
                            val name = parseWord() // TODO
                            expect(':')
                            val dom = parseTermS()
                            expect('.')
                            val cod = parseTermS()
                            TermS.ArrowS(name, dom, cod, range())
                        }

                        "code" -> {
                            val element = parseTypeZ()
                            TermS.CodeS(element, range())
                        }

                        "type" -> TermS.TypeZ(range())
                        "let" -> {
                            val name = parseWord()
                            expect('=')
                            val init = parseTermS()
                            expect(';')
                            val next = parseTermS()
                            TermS.Let(name, init, next, range())
                        }

                        else -> TermS.Var(word, range())
                    }
                }
            }
        }
    }

    private inline fun <A> parseList(close: Char, element: () -> A): List<A> {
        val elements = mutableListOf<A>()
        while (peek() != close) {
            elements += element()
            if (peek() == close) break
            else expect(',')
        }
        skip()
        return elements
    }

    private fun parseWord(): String {
        skipWhitespace()
        val start = cursor
        while (text[cursor].isWordPart()) skip()
        return text.substring(start, cursor)
    }

    private inline fun Char.isNumericStart(): Boolean = when (this) {
        '+', '-', in '0'..'9' -> true
        else -> false
    }

    private inline fun Char.isWordPart(): Boolean = when (this) {
        '"', '(', ')', ',', '.', ':', ';', '=', '[', '\\', ']', '{', '}' -> false
        else -> !isWhitespace()
    }

    private inline fun <A> ranged(block: RangeContext.() -> A): A = RangeContext().block()

    private tailrec fun skipWhitespace() {
        when (val char = text[cursor]) {
            '\n' -> linebreak()
            '\r' -> {
                when (text[cursor + 1]) {
                    '\n' -> {
                        cursor += 1
                        linebreak()
                    }

                    else -> linebreak()
                }
            }

            else -> {
                if (char.isWhitespace()) skip()
                else return
            }
        }
        skipWhitespace()
    }

    private inline fun peek(): Char {
        skipWhitespace()
        return text[cursor]
    }

    private inline fun expect(expected: Char) {
        if (peek() == expected) skip()
        else TODO()
    }

    private inline fun linebreak() {
        cursor += 1
        line += 1
        character = 0
    }

    private inline fun skip(size: Int = 1) {
        cursor += size
        character += size
    }

    private inline fun position(): Position = Position(line, character)

    private inner class RangeContext {
        private val start: Position = position()
        inline fun range(): Range = Range(start, position())
    }

    companion object : Phase<String, Root> {
        override operator fun invoke(
            messages: Messages,
            input: String,
        ): Root = Parse(messages, input).parseRoot()
    }
}
