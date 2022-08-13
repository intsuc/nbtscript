package nbtscript.phase

import nbtscript.ast.Core.*

class Stage private constructor() {
    private fun stageRoot(
        root: Root,
    ): Root {
        val body = stageTermZ(root.body)
        return Root(body)
    }

    private fun stageTermZ(
        term: TermZ,
    ): TermZ = when (term) {
        is TermZ.ByteTag -> term
        is TermZ.ShortTag -> term
        is TermZ.IntTag -> term
        is TermZ.LongTag -> term
        is TermZ.FloatTag -> term
        is TermZ.DoubleTag -> term
        is TermZ.StringTag -> term
        is TermZ.ByteArrayTag -> {
            val elements = term.elements.map { stageTermZ(it) }
            TermZ.ByteArrayTag(elements, term.type)
        }

        is TermZ.IntArrayTag -> {
            val elements = term.elements.map { stageTermZ(it) }
            TermZ.IntArrayTag(elements, term.type)
        }

        is TermZ.LongArrayTag -> {
            val elements = term.elements.map { stageTermZ(it) }
            TermZ.LongArrayTag(elements, term.type)
        }

        is TermZ.ListTag -> {
            val elements = term.elements.map { stageTermZ(it) }
            TermZ.ListTag(elements, term.type)
        }

        is TermZ.CompoundTag -> {
            val elements = term.elements.mapValues { stageTermZ(it.value) }
            TermZ.CompoundTag(elements, term.type)
        }

        is TermZ.Function -> {
            val body = stageTermZ(term.body)
            val next = stageTermZ(term.next)
            TermZ.Function(term.name, body, next, term.type)
        }

        is TermZ.Run -> term
        is TermZ.Splice -> {
            when (val element = normalize(term.element)) {
                is TermS.Quote -> element.element
                else -> error("expected: quote")
            }
        }

        is TermZ.Hole -> error("unexpected: hole")
    }

    companion object {
        operator fun invoke(
            root: Root,
        ): Root = Stage().stageRoot(root)
    }
}
