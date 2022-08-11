package nbtscript.phase

import org.eclipse.lsp4j.Range
import nbtscript.ast.Core as C
import nbtscript.ast.Core.Value as TypeS

sealed interface Message {
    val range: Range

    sealed interface Error : Message {
        data class NotFound(val name: String, override val range: Range) : Error
        data class ArrowExpected(val actual: C.Value, override val range: Range) : Error
        data class CodeExpected(val actual: C.Value, override val range: Range) : Error
        data class TypeZMismatched(val expected: C.TypeZ, val actual: C.TypeZ, override val range: Range) : Error
        data class TypeSMismatched(val expected: TypeS, val actual: TypeS, override val range: Range) : Error
    }
}
