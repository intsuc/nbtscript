package nbtscript.phase

import nbtscript.ast.Surface.*
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
        val body = parseTermZ()
        val root = Root(body, range())
        skipWhitespace()
        if (cursor != text.length) {
            val end = position()
            context.addDiagnostic(endOfFileExpected(Range(end, end)))
        }
        root
    }

    private fun parseTypeZ(): TypeZ = ranged {
        when (readString()) {
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

            else -> typeZHole()
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
                val data = readString() // TODO: escape
                expect('"')
                TermZ.StringTag(data, range())
            }

            '[' -> {
                skip()
                when (text.getOrNull(cursor)) {
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

            null -> termZHole()
            else -> {
                val word = readString()
                if (start.isNumericStart()) {
                    when {
                        word.endsWith('b') -> TermZ.ByteTag(word.dropLast(1).toByte(), range())
                        word.endsWith('s') -> TermZ.ShortTag(word.dropLast(1).toShort(), range())
                        word.endsWith('L') -> TermZ.LongTag(word.dropLast(1).toLong(), range())
                        word.endsWith('f') -> TermZ.FloatTag(word.dropLast(1).toFloat(), range())
                        word.endsWith('d') -> TermZ.DoubleTag(word.dropLast(1).toDouble(), range())
                        else -> {
                            when (val int = word.toIntOrNull()) {
                                null -> termZHole()
                                else -> TermZ.IntTag(int, range())
                            }
                        }
                    }
                } else {
                    when (word) {
                        "function" -> {
                            val name = parseWord()
                            val anno = when (peek()) {
                                ':' -> {
                                    skip()
                                    parseTypeZ()
                                }

                                else -> null
                            }
                            expect('=')
                            val body = parseTermZ()
                            expect(';')
                            val next = parseTermZ()
                            TermZ.Function(name, anno, body, next, range())
                        }

                        "" -> termZHole()
                        else -> TermZ.Run(word, range())
                    }
                }
            }
        }
    }

    private fun parseTermS(): TermS = ranged {
        when (peek()) {
            '(' -> {
                skip()
                val left = parseTermS1()
                when (peek()) {
                    ':' -> {
                        skip()
                        val type = parseTermS()
                        expect(')')
                        when (peek()) {
                            '=' -> {
                                skip()
                                expect('>')
                                val body = parseTermS()
                                when (left) {
                                    is TermS.Var -> TermS.Abs(left.name, type, body, range())
                                    else -> termSHole()
                                }
                            }

                            '-' -> {
                                skip()
                                expect('>')
                                val cod = parseTermS()
                                when (left) {
                                    is TermS.Var -> TermS.ArrowS(left.name, type, cod, range())
                                    else -> termSHole()
                                }
                            }

                            else -> termSHole()
                        }
                    }

                    ')' -> {
                        skip()
                        left
                    }

                    else -> termSHole()
                }
            }

            else -> {
                val left = parseTermS1()
                when (peek()) {
                    '-' -> {
                        skip()
                        expect('>')
                        val cod = parseTermS()
                        TermS.ArrowS(null, left, cod, range())
                    }

                    '(' -> {
                        skip()
                        val operand = parseTermS()
                        expect(')')
                        TermS.Apply(left, operand, range())
                    }

                    else -> left
                }
            }
        }
    }

    private fun parseTermS1(): TermS = ranged {
        when (val start = peek()) {
            '(' -> {
                skip()
                val term = parseTermS()
                expect(')')
                term
            }

            '"' -> {
                skip()
                val data = readString() // TODO: escape
                expect('"')
                TermS.StringTag(data, range())
            }

            '[' -> {
                skip()
                when (text.getOrNull(cursor)) {
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

            '`' -> {
                skip()
                val element = parseTermZ()
                TermS.Quote(element, range())
            }

            null -> termSHole()
            else -> {
                val word = readString()
                if (start.isNumericStart()) {
                    when {
                        word.endsWith('b') -> TermS.ByteTag(word.dropLast(1).toByte(), range())
                        word.endsWith('s') -> TermS.ShortTag(word.dropLast(1).toShort(), range())
                        word.endsWith('L') -> TermS.LongTag(word.dropLast(1).toLong(), range())
                        word.endsWith('f') -> TermS.FloatTag(word.dropLast(1).toFloat(), range())
                        word.endsWith('d') -> TermS.DoubleTag(word.dropLast(1).toDouble(), range())
                        else -> {
                            when (val int = word.toIntOrNull()) {
                                null -> termSHole()
                                else -> TermS.IntTag(int, range())
                            }
                        }
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

                        "indexed_element" -> { // TODO: use better syntax
                            val target = parseTermZ()
                            val index = parseTermS()
                            TermS.IndexedElement(target, index, range())
                        }

                        "code" -> {
                            val element = parseTypeZ()
                            TermS.CodeS(element, range())
                        }

                        "type" -> TermS.TypeZ(range())
                        "let" -> {
                            val name = parseWord()
                            val anno = when (peek()) {
                                ':' -> {
                                    skip()
                                    parseTermS()
                                }

                                else -> null
                            }
                            expect('=')
                            val init = parseTermS()
                            expect(';')
                            val next = parseTermS()
                            TermS.Let(name, anno, init, next, range())
                        }

                        "" -> termSHole()
                        else -> TermS.Var(word, range())
                    }
                }
            }
        }
    }

    private fun RangeContext.typeZHole(): TypeZ {
        val range = range()
        context.addDiagnostic(typeZExpected(range))
        return TypeZ.Hole(range)
    }

    private fun RangeContext.termZHole(): TermZ {
        val range = range()
        context.addDiagnostic(termZExpected(range))
        return TermZ.Hole(range)
    }

    private fun RangeContext.termSHole(): TermS {
        val range = range()
        context.addDiagnostic(termSExpected(range))
        return TermS.Hole(range)
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
        '"', '(', ')', ',', '.', ':', ';', '=', '[', '\\', ']', '{', '}' -> false
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
