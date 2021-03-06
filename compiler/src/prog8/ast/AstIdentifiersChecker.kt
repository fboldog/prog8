package prog8.ast

import prog8.compiler.HeapValues
import prog8.functions.BuiltinFunctions

/**
 * Checks the validity of all identifiers (no conflicts)
 * Also builds a list of all (scoped) symbol definitions
 * Also makes sure that subroutine's parameters also become local variable decls in the subroutine's scope.
 * Finally, it also makes sure the datatype of all Var decls and sub Return values is set correctly.
 */

fun Module.checkIdentifiers(heap: HeapValues): MutableMap<String, IStatement> {
    val checker = AstIdentifiersChecker(heap)
    this.process(checker)

    // add any anonymous variables for heap values that are used, and replace literalvalue by identifierref
    for (variable in checker.anonymousVariablesFromHeap) {
        val scope = variable.first.definingScope()
        scope.statements.add(variable.second)
        val parent = variable.first.parent
        when {
            parent is Assignment && parent.value === variable.first -> {
                val idref = IdentifierReference(listOf("auto_heap_value_${variable.first.heapId}"), variable.first.position)
                idref.linkParents(parent)
                parent.value = idref
            }
            parent is IFunctionCall -> {
                val parameterPos = parent.arglist.indexOf(variable.first)
                val idref = IdentifierReference(listOf("auto_heap_value_${variable.first.heapId}"), variable.first.position)
                idref.linkParents(parent)
                parent.arglist[parameterPos] = idref
            }
            else -> TODO("replace literalvalue by identifierref: $variable  (in $parent)")
        }
    }

    printErrors(checker.result(), name)
    return checker.symbols
}


private class AstIdentifiersChecker(val heap: HeapValues) : IAstProcessor {
    private val checkResult: MutableList<AstException> = mutableListOf()

    var symbols: MutableMap<String, IStatement> = mutableMapOf()
        private set

    fun result(): List<AstException> {
        return checkResult
    }

    private fun nameError(name: String, position: Position, existing: IStatement) {
        checkResult.add(NameError("name conflict '$name', first defined in ${existing.position.file} line ${existing.position.line}", position))
    }

    override fun process(block: Block): IStatement {
        val scopedName = block.scopedname
        val existing = symbols[scopedName]
        if(existing!=null) {
            nameError(block.name, block.position, existing)
        } else {
            symbols[scopedName] = block
        }
        return super.process(block)
    }

    override fun process(functionCall: FunctionCall): IExpression {
        if(functionCall.target.nameInSource.size==1 && functionCall.target.nameInSource[0]=="lsb") {
            // lsb(...) is just an alias for type cast to ubyte, so replace with "... as ubyte"
            val typecast = TypecastExpression(functionCall.arglist.single(), DataType.UBYTE, functionCall.position)
            typecast.linkParents(functionCall.parent)
            return super.process(typecast)
        }
        return super.process(functionCall)
    }

    override fun process(decl: VarDecl): IStatement {
        // first, check if there are datatype errors on the vardecl
        decl.datatypeErrors.forEach { checkResult.add(it) }

        // now check the identifier
        if(decl.name in BuiltinFunctions)
            // the builtin functions can't be redefined
            checkResult.add(NameError("builtin function cannot be redefined", decl.position))

        val scopedName = decl.scopedname
        val existing = symbols[scopedName]
        if(existing!=null) {
            nameError(decl.name, decl.position, existing)
        } else {
            symbols[scopedName] = decl
        }
        return super.process(decl)
    }

    override fun process(subroutine: Subroutine): IStatement {
        if(subroutine.name in BuiltinFunctions) {
            // the builtin functions can't be redefined
            checkResult.add(NameError("builtin function cannot be redefined", subroutine.position))
        } else {
            if (subroutine.parameters.any { it.name in BuiltinFunctions })
                checkResult.add(NameError("builtin function name cannot be used as parameter", subroutine.position))

            val scopedName = subroutine.scopedname
            val existing = symbols[scopedName]
            if (existing != null) {
                nameError(subroutine.name, subroutine.position, existing)
            } else {
                symbols[scopedName] = subroutine
            }

            // check that there are no local variables that redefine the subroutine's parameters
            val allDefinedNames = subroutine.allLabelsAndVariables()
            val paramNames = subroutine.parameters.map { it.name }.toSet()
            val paramsToCheck = paramNames.intersect(allDefinedNames)
            for(name in paramsToCheck) {
                val thing = subroutine.getLabelOrVariable(name)!!
                if(thing.position != subroutine.position)
                    nameError(name, thing.position, subroutine)
            }

            // inject subroutine params as local variables (if they're not there yet) (for non-kernel subroutines and non-asm parameters)
            // NOTE:
            // - numeric types BYTE and WORD and FLOAT are passed by value;
            // - strings, arrays, matrices are passed by reference (their 16-bit address is passed as an uword parameter)
            // - do NOT do this is the statement can be transformed into an asm subroutine later!
            if(subroutine.asmAddress==null && !subroutine.canBeAsmSubroutine) {
                if(subroutine.asmParameterRegisters.isEmpty()) {
                    subroutine.parameters
                            .filter { it.name !in allDefinedNames }
                            .forEach {
                                val vardecl = VarDecl(VarDeclType.VAR, it.type, false, null, it.name, null, subroutine.position)
                                vardecl.linkParents(subroutine)
                                subroutine.statements.add(0, vardecl)
                            }
                }
            }
        }
        return super.process(subroutine)
    }

    override fun process(label: Label): IStatement {
        if(label.name in BuiltinFunctions) {
            // the builtin functions can't be redefined
            checkResult.add(NameError("builtin function cannot be redefined", label.position))
        } else {
            val scopedName = label.scopedname
            val existing = symbols[scopedName]
            if (existing != null) {
                nameError(label.name, label.position, existing)
            } else {
                symbols[scopedName] = label
            }
        }
        return super.process(label)
    }

    override fun process(forLoop: ForLoop): IStatement {
        // If the for loop has a decltype, it means to declare the loopvar inside the loop body
        // rather than reusing an already declared loopvar from an outer scope.
        // For loops that loop over an interable variable (instead of a range of numbers) get an
        // additional interation count variable in their scope.
        if(forLoop.loopRegister!=null) {
            if(forLoop.decltype!=null)
                checkResult.add(SyntaxError("register loop variables cannot be explicitly declared with a datatype", forLoop.position))
            if(forLoop.loopRegister == Register.X)
                printWarning("writing to the X register is dangerous, because it's used as an internal pointer", forLoop.position)
        } else if(forLoop.loopVar!=null) {
            val varName = forLoop.loopVar.nameInSource.last()
            if(forLoop.decltype!=null) {
                val existing = if(forLoop.body.isEmpty()) null else forLoop.body.lookup(forLoop.loopVar.nameInSource, forLoop.body.statements.first())
                if(existing==null) {
                    // create the local scoped for loop variable itself
                    val vardecl = VarDecl(VarDeclType.VAR, forLoop.decltype, true, null, varName, null, forLoop.loopVar.position)
                    vardecl.linkParents(forLoop.body)
                    forLoop.body.statements.add(0, vardecl)
                    forLoop.loopVar.parent = forLoop.body   // loopvar 'is defined in the body'
                }

            }

            if(forLoop.iterable !is RangeExpr) {
                val existing = if(forLoop.body.isEmpty()) null else forLoop.body.lookup(listOf(ForLoop.iteratorLoopcounterVarname), forLoop.body.statements.first())
                if(existing==null) {
                    // create loop iteration counter variable (without value, to avoid an assignment)
                    val vardecl = VarDecl(VarDeclType.VAR, DataType.UBYTE, true, null, ForLoop.iteratorLoopcounterVarname, null, forLoop.loopVar.position)
                    vardecl.linkParents(forLoop.body)
                    forLoop.body.statements.add(0, vardecl)
                    forLoop.loopVar.parent = forLoop.body   // loopvar 'is defined in the body'
                }
            }
        }
        return super.process(forLoop)
    }

    override fun process(assignTarget: AssignTarget): AssignTarget {
        if(assignTarget.register==Register.X)
            printWarning("writing to the X register is dangerous, because it's used as an internal pointer", assignTarget.position)
        return super.process(assignTarget)
    }

    override fun process(returnStmt: Return): IStatement {
        if(returnStmt.values.isNotEmpty()) {
            // possibly adjust any literal values returned, into the desired returning data type
            val subroutine = returnStmt.definingSubroutine()!!
            if(subroutine.returntypes.size!=returnStmt.values.size)
                return returnStmt  // mismatch in number of return values, error will be printed later.
            val newValues = mutableListOf<IExpression>()
            for(returnvalue in returnStmt.values.zip(subroutine.returntypes)) {
                val lval = returnvalue.first as? LiteralValue
                if(lval!=null) {
                    val adjusted = lval.intoDatatype(returnvalue.second)
                    if(adjusted!=null && adjusted !== lval)
                        newValues.add(adjusted)
                    else
                        newValues.add(lval)
                }
                else
                    newValues.add(returnvalue.first)
            }
            returnStmt.values = newValues
        }
        return super.process(returnStmt)
    }


    internal val anonymousVariablesFromHeap = mutableSetOf<Pair<LiteralValue, VarDecl>>()

    override fun process(literalValue: LiteralValue): LiteralValue {
        if(literalValue.heapId!=null && literalValue.parent !is VarDecl) {
            // a literal value that's not declared as a variable, which refers to something on the heap.
            // we need to introduce an auto-generated variable for this to be able to refer to the value!
            val variable = VarDecl(VarDeclType.VAR, literalValue.type, false, null, "auto_heap_value_${literalValue.heapId}", literalValue, literalValue.position)
            anonymousVariablesFromHeap.add(Pair(literalValue, variable))
        }
        return super.process(literalValue)
    }
}
