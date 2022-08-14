package nbtscript.phase

import nbtscript.ast.Core as C
import nbtscript.ast.Staged as S

class Stage private constructor() {
    private fun stageRoot(
        root: C.Root,
    ): S.Root {
        val body = stageTermZ(root.body)
        return S.Root(body)
    }

    private fun stageTermZ(
        term: C.TermZ,
    ): S.TermZ = when (term) {
        is C.TermZ.ByteTag -> S.TermZ.ByteTag(term.data)
        is C.TermZ.ShortTag -> S.TermZ.ShortTag(term.data)
        is C.TermZ.IntTag -> S.TermZ.IntTag(term.data)
        is C.TermZ.LongTag -> S.TermZ.LongTag(term.data)
        is C.TermZ.FloatTag -> S.TermZ.FloatTag(term.data)
        is C.TermZ.DoubleTag -> S.TermZ.DoubleTag(term.data)
        is C.TermZ.StringTag -> S.TermZ.StringTag(term.data)
        is C.TermZ.ByteArrayTag -> {
            val elements = term.elements.map { stageTermZ(it) }
            S.TermZ.ByteArrayTag(elements)
        }

        is C.TermZ.IntArrayTag -> {
            val elements = term.elements.map { stageTermZ(it) }
            S.TermZ.IntArrayTag(elements)
        }

        is C.TermZ.LongArrayTag -> {
            val elements = term.elements.map { stageTermZ(it) }
            S.TermZ.LongArrayTag(elements)
        }

        is C.TermZ.ListTag -> {
            val elements = term.elements.map { stageTermZ(it) }
            S.TermZ.ListTag(elements)
        }

        is C.TermZ.CompoundTag -> {
            val elements = term.elements.mapValues { stageTermZ(it.value) }
            S.TermZ.CompoundTag(elements)
        }

        is C.TermZ.Function -> {
            val body = stageTermZ(term.body)
            val next = stageTermZ(term.next)
            S.TermZ.Function(term.name, body, next)
        }

        is C.TermZ.Run -> S.TermZ.Run(term.name)
        is C.TermZ.Splice -> {
            when (val element = normalize(term.element)) {
                is C.TermS.Quote -> stageTermZ(element.element)
                else -> error("expected: quote")
            }
        }

        is C.TermZ.Hole -> S.TermZ.Hole
    }

    companion object {
        operator fun invoke(
            root: C.Root,
        ): S.Root = Stage().stageRoot(root)
    }
}
