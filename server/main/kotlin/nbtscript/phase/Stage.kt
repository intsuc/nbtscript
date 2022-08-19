package nbtscript.phase

import kotlinx.collections.immutable.persistentListOf
import nbtscript.ast.Core as C
import nbtscript.ast.Staged as S

class Stage private constructor(
    private val context: Phase.Context,
) {
    private fun stageRoot(
        root: C.Root,
    ): S.Root {
        val body = stageTermZ(root.body)
        return S.Root(body)
    }

    private fun stageTermZ(
        term: C.TermZ,
    ): S.Term = when (term) {
        is C.TermZ.ByteTag -> S.Term.ByteTag(term.data)
        is C.TermZ.ShortTag -> S.Term.ShortTag(term.data)
        is C.TermZ.IntTag -> S.Term.IntTag(term.data)
        is C.TermZ.LongTag -> S.Term.LongTag(term.data)
        is C.TermZ.FloatTag -> S.Term.FloatTag(term.data)
        is C.TermZ.DoubleTag -> S.Term.DoubleTag(term.data)
        is C.TermZ.StringTag -> S.Term.StringTag(term.data)
        is C.TermZ.ByteArrayTag -> {
            val elements = term.elements.map { stageTermZ(it) }
            S.Term.ByteArrayTag(elements)
        }

        is C.TermZ.IntArrayTag -> {
            val elements = term.elements.map { stageTermZ(it) }
            S.Term.IntArrayTag(elements)
        }

        is C.TermZ.LongArrayTag -> {
            val elements = term.elements.map { stageTermZ(it) }
            S.Term.LongArrayTag(elements)
        }

        is C.TermZ.ListTag -> {
            val elements = term.elements.map { stageTermZ(it) }
            S.Term.ListTag(elements)
        }

        is C.TermZ.CompoundTag -> {
            val elements = term.elements.mapValues { stageTermZ(it.value) }
            S.Term.CompoundTag(elements)
        }

        is C.TermZ.Function -> {
            val body = stageTermZ(term.body)
            val next = stageTermZ(term.next)
            S.Term.Function(term.name, body, next)
        }

        is C.TermZ.Run -> S.Term.Run(term.name)
        is C.TermZ.Splice -> {
            when (val element = context.unifier.normalize(term.element)) {
                is C.TermS.IndexedElement -> {
                    val target = stageTermZ(element.target)
                    val index = (context.unifier.reflectTermS(persistentListOf(), element.index) as C.VTermS.IntTag).data
                    S.Term.IndexedElement(target, index)
                }

                is C.TermS.QuoteTerm -> stageTermZ(element.element)
                else -> error("expected: quote")
            }
        }

        is C.TermZ.Hole -> S.Term.Hole
    }

    companion object : Phase<C.Root, S.Root> {
        override operator fun invoke(
            context: Phase.Context,
            input: C.Root,
        ): S.Root = Stage(context).stageRoot(input)
    }
}
