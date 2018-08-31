package il65.ast

import il65.parser.ParsingFailedError

/**
 * Checks the validity of all identifiers (no conflicts)
 * Also builds a list of all (scoped) symbol definitions
 */

fun Module.checkIdentifiers(globalNamespace: INameScope): MutableMap<String, IStatement> {
    val checker = AstIdentifiersChecker(globalNamespace)
    this.process(checker)
    val checkResult = checker.result()
    checkResult.forEach {
        System.err.println(it)
    }
    if(checkResult.isNotEmpty())
        throw ParsingFailedError("There are ${checkResult.size} errors in module '$name'.")
    return checker.symbols
}


class AstIdentifiersChecker(private val globalNamespace: INameScope) : IAstProcessor {
    private val checkResult: MutableList<AstException> = mutableListOf()

    var symbols: MutableMap<String, IStatement> = mutableMapOf()
        private set

    fun result(): List<AstException> {
        return checkResult
    }

    private fun nameError(name: String, position: Position?, existing: IStatement) {
        checkResult.add(NameError("name conflict '$name', first defined in ${existing.position?.file} line ${existing.position?.line}", position))
    }

    override fun process(block: Block): IStatement {
        val scopedName = block.scopedName(block.name)
        val existing = symbols[scopedName]
        if(existing!=null) {
            nameError(block.name, block.position, existing)
        } else {
            symbols[scopedName] = block
        }
        super.process(block)
        return block
    }

    override fun process(decl: VarDecl): IStatement {
        val scopedName = decl.scopedName(decl.name)
        val existing = symbols[scopedName]
        if(existing!=null) {
            nameError(decl.name, decl.position, existing)
        } else {
            symbols[scopedName] = decl
        }
        super.process(decl)
        return decl
    }

    override fun process(subroutine: Subroutine): IStatement {
        val scopedName = subroutine.scopedName(subroutine.name)
        val existing = symbols[scopedName]
        if(existing!=null) {
            nameError(subroutine.name, subroutine.position, existing)
        } else {
            symbols[scopedName] = subroutine
        }
        super.process(subroutine)
        return subroutine
    }

    override fun process(label: Label): IStatement {
        val scopedName = label.scopedName(label.name)
        val existing = symbols[scopedName]
        if(existing!=null) {
            nameError(label.name, label.position, existing)
        } else {
            symbols[scopedName] = label
        }
        super.process(label)
        return label
    }
}
