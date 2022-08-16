package nbtscript.phase

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.plus
import nbtscript.ast.Staged.Root
import nbtscript.ast.Staged.Term

class Execute private constructor() {
    private fun executeRoot(
        root: Root,
    ): Root {
        val body = executeTerm(persistentMapOf(), root.body)
        return Root(body)
    }

    private fun executeTerm(
        ctx: PersistentMap<String, Term>,
        term: Term,
    ): Term = when (term) {
        is Term.ByteTag -> term
        is Term.ShortTag -> term
        is Term.IntTag -> term
        is Term.LongTag -> term
        is Term.FloatTag -> term
        is Term.DoubleTag -> term
        is Term.StringTag -> term
        is Term.ByteArrayTag -> {
            val elements = term.elements.map { executeTerm(ctx, it) }
            Term.ByteArrayTag(elements)
        }

        is Term.IntArrayTag -> {
            val elements = term.elements.map { executeTerm(ctx, it) }
            Term.IntArrayTag(elements)
        }

        is Term.LongArrayTag -> {
            val elements = term.elements.map { executeTerm(ctx, it) }
            Term.LongArrayTag(elements)
        }

        is Term.ListTag -> {
            val elements = term.elements.map { executeTerm(ctx, it) }
            Term.ListTag(elements)
        }

        is Term.CompoundTag -> {
            val elements = term.elements.mapValues { executeTerm(ctx, it.value) }
            Term.CompoundTag(elements)
        }

        is Term.IndexedElement -> {
            when (term.target) {
                is Term.ByteArrayTag -> term.target.elements[term.index]
                is Term.IntArrayTag -> term.target.elements[term.index]
                is Term.LongArrayTag -> term.target.elements[term.index]
                is Term.ListTag -> term.target.elements[term.index]
                else -> error("")
            }
        }

        is Term.Function -> executeTerm(ctx + (term.name to term.body), term.next)
        is Term.Run -> executeTerm(ctx, ctx[term.name]!!)
        is Term.Hole -> term
    }

    companion object {
        operator fun invoke(
            root: Root,
        ): Root = Execute().executeRoot(root)
    }
}
