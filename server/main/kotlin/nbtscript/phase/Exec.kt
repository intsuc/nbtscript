package nbtscript.phase

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.plus
import nbtscript.ast.Staged.Root
import nbtscript.ast.Staged.Term

class Exec private constructor() {
    private fun execRoot(
        root: Root,
    ): Root {
        val body = execTerm(persistentMapOf(), root.body)
        return Root(body)
    }

    private fun execTerm(
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
            val elements = term.elements.map { execTerm(ctx, it) }
            Term.ByteArrayTag(elements)
        }

        is Term.IntArrayTag -> {
            val elements = term.elements.map { execTerm(ctx, it) }
            Term.IntArrayTag(elements)
        }

        is Term.LongArrayTag -> {
            val elements = term.elements.map { execTerm(ctx, it) }
            Term.LongArrayTag(elements)
        }

        is Term.ListTag -> {
            val elements = term.elements.map { execTerm(ctx, it) }
            Term.ListTag(elements)
        }

        is Term.CompoundTag -> {
            val elements = term.elements.mapValues { execTerm(ctx, it.value) }
            Term.CompoundTag(elements)
        }

        is Term.Fun -> execTerm(ctx + (term.name to term.body), term.next)
        is Term.Run -> execTerm(ctx, ctx[term.name]!!)
        is Term.Hole -> term
    }

    companion object {
        operator fun invoke(
            root: Root,
        ): Root = Exec().execRoot(root)
    }
}
