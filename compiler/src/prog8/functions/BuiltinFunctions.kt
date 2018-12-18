package prog8.functions

import prog8.ast.*
import prog8.compiler.CompilerException
import prog8.compiler.HeapValues
import kotlin.math.log2


class BuiltinFunctionParam(val name: String, val possibleDatatypes: Set<DataType>)

class FunctionSignature(val pure: Boolean,      // does it have side effects?
                        val parameters: List<BuiltinFunctionParam>,
                        val returntype: DataType?,
                        val constExpressionFunc: ((args: List<IExpression>, position: Position, namespace: INameScope, heap: HeapValues) -> LiteralValue)? = null)


val BuiltinFunctions = mapOf(
        // this set of function have no return value and operate in-place:
    "rol"         to FunctionSignature(false, listOf(BuiltinFunctionParam("item", setOf(DataType.UBYTE, DataType.UWORD))), null),
    "ror"         to FunctionSignature(false, listOf(BuiltinFunctionParam("item", setOf(DataType.UBYTE, DataType.UWORD))), null),
    "rol2"        to FunctionSignature(false, listOf(BuiltinFunctionParam("item", setOf(DataType.UBYTE, DataType.UWORD))), null),
    "ror2"        to FunctionSignature(false, listOf(BuiltinFunctionParam("item", setOf(DataType.UBYTE, DataType.UWORD))), null),
    "lsl"         to FunctionSignature(false, listOf(BuiltinFunctionParam("item", setOf(DataType.UBYTE, DataType.UWORD))), null),
    "lsr"         to FunctionSignature(false, listOf(BuiltinFunctionParam("item", setOf(DataType.UBYTE, DataType.UWORD))), null),
        // these few have a return value depending on the argument(s):
    "max"         to FunctionSignature(true, listOf(BuiltinFunctionParam("values", ArrayDatatypes)), null) { a, p, n, h -> collectionArgOutputNumber(a, p, n, h) { it.max()!! }},        // type depends on args
    "min"         to FunctionSignature(true, listOf(BuiltinFunctionParam("values", ArrayDatatypes)), null) { a, p, n, h -> collectionArgOutputNumber(a, p, n, h) { it.min()!! }},        // type depends on args
    "sum"         to FunctionSignature(true, listOf(BuiltinFunctionParam("values", ArrayDatatypes)), null) { a, p, n, h -> collectionArgOutputNumber(a, p, n, h) { it.sum() }},        // type depends on args
    "abs"         to FunctionSignature(true, listOf(BuiltinFunctionParam("value", NumericDatatypes)), null, ::builtinAbs),      // type depends on argument
        // normal functions follow:
    "sin"         to FunctionSignature(true, listOf(BuiltinFunctionParam("rads", setOf(DataType.FLOAT))), DataType.FLOAT) { a, p, n, h -> oneDoubleArg(a, p, n, h, Math::sin) },
    "cos"         to FunctionSignature(true, listOf(BuiltinFunctionParam("rads", setOf(DataType.FLOAT))), DataType.FLOAT) { a, p, n, h -> oneDoubleArg(a, p, n, h, Math::cos) },
    "tan"         to FunctionSignature(true, listOf(BuiltinFunctionParam("rads", setOf(DataType.FLOAT))), DataType.FLOAT) { a, p, n, h -> oneDoubleArg(a, p, n, h, Math::tan) },
    "atan"        to FunctionSignature(true, listOf(BuiltinFunctionParam("rads", setOf(DataType.FLOAT))), DataType.FLOAT) { a, p, n, h -> oneDoubleArg(a, p, n, h, Math::atan) },
    "ln"          to FunctionSignature(true, listOf(BuiltinFunctionParam("value", setOf(DataType.FLOAT))), DataType.FLOAT) { a, p, n, h -> oneDoubleArg(a, p, n, h, Math::log) },
    "log2"        to FunctionSignature(true, listOf(BuiltinFunctionParam("value", setOf(DataType.FLOAT))), DataType.FLOAT) { a, p, n, h -> oneDoubleArg(a, p, n, h, ::log2) },
    "sqrt"        to FunctionSignature(true, listOf(BuiltinFunctionParam("value", setOf(DataType.FLOAT))), DataType.FLOAT) { a, p, n, h -> oneDoubleArg(a, p, n, h, Math::sqrt) },
    "rad"         to FunctionSignature(true, listOf(BuiltinFunctionParam("value", setOf(DataType.FLOAT))), DataType.FLOAT) { a, p, n, h -> oneDoubleArg(a, p, n, h, Math::toRadians) },
    "deg"         to FunctionSignature(true, listOf(BuiltinFunctionParam("value", setOf(DataType.FLOAT))), DataType.FLOAT) { a, p, n, h -> oneDoubleArg(a, p, n, h, Math::toDegrees) },
    "avg"         to FunctionSignature(true, listOf(BuiltinFunctionParam("values", ArrayDatatypes)), DataType.FLOAT, ::builtinAvg),
    "round"       to FunctionSignature(true, listOf(BuiltinFunctionParam("value", setOf(DataType.FLOAT))), DataType.FLOAT) { a, p, n, h -> oneDoubleArgOutputWord(a, p, n, h, Math::round) },
    "floor"       to FunctionSignature(true, listOf(BuiltinFunctionParam("value", setOf(DataType.FLOAT))), DataType.FLOAT) { a, p, n, h -> oneDoubleArgOutputWord(a, p, n, h, Math::floor) },
    "ceil"        to FunctionSignature(true, listOf(BuiltinFunctionParam("value", setOf(DataType.FLOAT))), DataType.FLOAT) { a, p, n, h -> oneDoubleArgOutputWord(a, p, n, h, Math::ceil) },
    "len"         to FunctionSignature(true, listOf(BuiltinFunctionParam("values", IterableDatatypes)), DataType.UBYTE, ::builtinLen),
    "any"         to FunctionSignature(true, listOf(BuiltinFunctionParam("values", ArrayDatatypes)), DataType.UBYTE) { a, p, n, h -> collectionArgOutputBoolean(a, p, n, h) { it.any { v -> v != 0.0} }},
    "all"         to FunctionSignature(true, listOf(BuiltinFunctionParam("values", ArrayDatatypes)), DataType.UBYTE) { a, p, n, h -> collectionArgOutputBoolean(a, p, n, h) { it.all { v -> v != 0.0} }},
    "lsb"         to FunctionSignature(true, listOf(BuiltinFunctionParam("value", setOf(DataType.UWORD, DataType.WORD))), DataType.UBYTE) { a, p, n, h -> oneIntArgOutputInt(a, p, n, h) { x: Int -> x and 255 }},
    "msb"         to FunctionSignature(true, listOf(BuiltinFunctionParam("value", setOf(DataType.UWORD, DataType.WORD))), DataType.UBYTE) { a, p, n, h -> oneIntArgOutputInt(a, p, n, h) { x: Int -> x ushr 8 and 255}},
    "flt"         to FunctionSignature(true, listOf(BuiltinFunctionParam("value", NumericDatatypes)), DataType.FLOAT, ::builtinFlt),
    "uwrd"        to FunctionSignature(true, listOf(BuiltinFunctionParam("value", setOf(DataType.UBYTE, DataType.BYTE, DataType.WORD))), DataType.UWORD, ::builtinUwrd),
    "wrd"         to FunctionSignature(true, listOf(BuiltinFunctionParam("value", setOf(DataType.UBYTE, DataType.BYTE, DataType.UWORD))), DataType.WORD, ::builtinWrd),
    "fintb"       to FunctionSignature(true, listOf(BuiltinFunctionParam("value", setOf(DataType.FLOAT))), DataType.BYTE, ::builtinFintb),
    "fintw"       to FunctionSignature(true, listOf(BuiltinFunctionParam("value", setOf(DataType.FLOAT))), DataType.WORD, ::builtinFintw),
    "b2ub"        to FunctionSignature(true, listOf(BuiltinFunctionParam("value", setOf(DataType.BYTE))), DataType.UBYTE, ::builtinB2ub),
    "ub2b"        to FunctionSignature(true, listOf(BuiltinFunctionParam("value", setOf(DataType.UBYTE))), DataType.BYTE, ::builtinUb2b),
    "rnd"         to FunctionSignature(true, emptyList(), DataType.UBYTE),
    "rndw"        to FunctionSignature(true, emptyList(), DataType.UWORD),
    "rndf"        to FunctionSignature(true, emptyList(), DataType.FLOAT),
    "rsave"       to FunctionSignature(false, emptyList(), null),
    "rrestore"    to FunctionSignature(false, emptyList(), null),
    "set_carry"   to FunctionSignature(false, emptyList(), null),
    "clear_carry" to FunctionSignature(false, emptyList(), null),
    "set_irqd"    to FunctionSignature(false, emptyList(), null),
    "clear_irqd"  to FunctionSignature(false, emptyList(), null),
    "str2byte"    to FunctionSignature(true, listOf(BuiltinFunctionParam("string", StringDatatypes)), DataType.BYTE),
    "str2ubyte"   to FunctionSignature(true, listOf(BuiltinFunctionParam("string", StringDatatypes)), DataType.UBYTE),
    "str2word"    to FunctionSignature(true, listOf(BuiltinFunctionParam("string", StringDatatypes)), DataType.WORD),
    "str2uword"   to FunctionSignature(true, listOf(BuiltinFunctionParam("string", StringDatatypes)), DataType.UWORD),
    "str2float"   to FunctionSignature(true, listOf(BuiltinFunctionParam("string", StringDatatypes)), DataType.FLOAT),
    "vm_write_memchr"  to FunctionSignature(false, listOf(BuiltinFunctionParam("address", setOf(DataType.UWORD))), null),
    "vm_write_memstr"  to FunctionSignature(false, listOf(BuiltinFunctionParam("address", setOf(DataType.UWORD))), null),
    "vm_write_num"     to FunctionSignature(false, listOf(BuiltinFunctionParam("number", NumericDatatypes)), null),
    "vm_write_char"    to FunctionSignature(false, listOf(BuiltinFunctionParam("char", setOf(DataType.UBYTE))), null),
    "vm_write_str"     to FunctionSignature(false, listOf(BuiltinFunctionParam("string", StringDatatypes)), null),
    "vm_input_str"     to FunctionSignature(false, listOf(BuiltinFunctionParam("intovar", StringDatatypes)), null),
    "vm_gfx_clearscr"  to FunctionSignature(false, listOf(BuiltinFunctionParam("color", setOf(DataType.UBYTE))), null),
    "vm_gfx_pixel"     to FunctionSignature(false, listOf(
                                                        BuiltinFunctionParam("x", IntegerDatatypes),
                                                        BuiltinFunctionParam("y", IntegerDatatypes),
                                                        BuiltinFunctionParam("color", IntegerDatatypes)), null),
    "vm_gfx_line"     to FunctionSignature(false, listOf(
                                                        BuiltinFunctionParam("x1", IntegerDatatypes),
                                                        BuiltinFunctionParam("y1", IntegerDatatypes),
                                                        BuiltinFunctionParam("x2", IntegerDatatypes),
                                                        BuiltinFunctionParam("y2", IntegerDatatypes),
                                                        BuiltinFunctionParam("color", IntegerDatatypes)), null),
    "vm_gfx_text"      to FunctionSignature(false, listOf(
                                                        BuiltinFunctionParam("x", IntegerDatatypes),
                                                        BuiltinFunctionParam("y", IntegerDatatypes),
                                                        BuiltinFunctionParam("color", IntegerDatatypes),
                                                        BuiltinFunctionParam("text", StringDatatypes)),
                                                        null)
)


fun builtinFunctionReturnType(function: String, args: List<IExpression>, namespace: INameScope, heap: HeapValues): DataType? {

    fun datatypeFromListArg(arglist: IExpression): DataType {
        if(arglist is LiteralValue) {
            if(arglist.type==DataType.ARRAY_UB || arglist.type==DataType.ARRAY_UW || arglist.type==DataType.ARRAY_F) {
                val dt = arglist.arrayvalue!!.map {it.resultingDatatype(namespace, heap)}
                if(dt.any { it!=DataType.UBYTE && it!=DataType.UWORD && it!=DataType.FLOAT}) {
                    throw FatalAstException("fuction $function only accepts arrayspec of numeric values")
                }
                if(dt.any { it==DataType.FLOAT }) return DataType.FLOAT
                if(dt.any { it==DataType.UWORD }) return DataType.UWORD
                return DataType.UBYTE
            }
        }
        if(arglist is IdentifierReference) {
            val dt = arglist.resultingDatatype(namespace, heap)
            return when(dt) {
                DataType.UBYTE, DataType.BYTE, DataType.UWORD, DataType.WORD, DataType.FLOAT,
                DataType.STR, DataType.STR_P, DataType.STR_S, DataType.STR_PS -> dt
                DataType.ARRAY_UB -> DataType.UBYTE
                DataType.ARRAY_B -> DataType.BYTE
                DataType.ARRAY_UW -> DataType.UWORD
                DataType.ARRAY_W -> DataType.WORD
                DataType.ARRAY_F -> DataType.FLOAT
                null -> throw FatalAstException("function requires one argument which is an arrayspec $function")
            }
        }
        throw FatalAstException("function requires one argument which is an arrayspec $function")
    }

    val func = BuiltinFunctions[function]!!
    if(func.returntype!=null)
        return func.returntype
    // function has return values, but the return type depends on the arguments

    return when (function) {
        "max", "min", "abs" -> {
            val dt = datatypeFromListArg(args.single())
            when(dt) {
                DataType.UBYTE, DataType.BYTE, DataType.UWORD, DataType.WORD, DataType.FLOAT -> dt
                DataType.STR, DataType.STR_P, DataType.STR_S, DataType.STR_PS -> DataType.UBYTE
                DataType.ARRAY_UB -> DataType.UBYTE
                DataType.ARRAY_B -> DataType.BYTE
                DataType.ARRAY_UW -> DataType.UWORD
                DataType.ARRAY_W -> DataType.WORD
                DataType.ARRAY_F -> DataType.FLOAT
            }
        }
        "sum" -> {
            val dt=datatypeFromListArg(args.single())
            when(dt) {
                DataType.UBYTE, DataType.UWORD -> DataType.UWORD
                DataType.BYTE, DataType.WORD -> DataType.WORD
                DataType.FLOAT -> DataType.FLOAT
                DataType.ARRAY_UB, DataType.ARRAY_UW -> DataType.UWORD
                DataType.ARRAY_B, DataType.ARRAY_W -> DataType.WORD
                DataType.ARRAY_F -> DataType.FLOAT
                DataType.STR, DataType.STR_P, DataType.STR_S, DataType.STR_PS -> DataType.UWORD
            }
        }
        else -> return null
    }
}


class NotConstArgumentException: AstException("not a const argument to a built-in function")


private fun oneDoubleArg(args: List<IExpression>, position: Position, namespace:INameScope, heap: HeapValues, function: (arg: Double)->Number): LiteralValue {
    if(args.size!=1)
        throw SyntaxError("built-in function requires one floating point argument", position)
    val constval = args[0].constValue(namespace, heap) ?: throw NotConstArgumentException()
    if(constval.type!=DataType.FLOAT)
        throw SyntaxError("built-in function requires one floating point argument", position)

    val float = constval.asNumericValue?.toDouble()!!
    return numericLiteral(function(float), args[0].position)
}

private fun oneDoubleArgOutputWord(args: List<IExpression>, position: Position, namespace:INameScope, heap: HeapValues, function: (arg: Double)->Number): LiteralValue {
    if(args.size!=1)
        throw SyntaxError("built-in function requires one floating point argument", position)
    val constval = args[0].constValue(namespace, heap) ?: throw NotConstArgumentException()
    if(constval.type!=DataType.FLOAT)
        throw SyntaxError("built-in function requires one floating point argument", position)
    return LiteralValue(DataType.WORD, wordvalue=function(constval.asNumericValue!!.toDouble()).toInt(), position=args[0].position)
}

private fun oneIntArgOutputInt(args: List<IExpression>, position: Position, namespace:INameScope, heap: HeapValues, function: (arg: Int)->Number): LiteralValue {
    if(args.size!=1)
        throw SyntaxError("built-in function requires one integer argument", position)
    val constval = args[0].constValue(namespace, heap) ?: throw NotConstArgumentException()
    if(constval.type!=DataType.UBYTE && constval.type!=DataType.UWORD)
        throw SyntaxError("built-in function requires one integer argument", position)

    val integer = constval.asNumericValue?.toInt()!!
    return numericLiteral(function(integer).toInt(), args[0].position)
}

private fun collectionArgOutputNumber(args: List<IExpression>, position: Position,
                                      namespace:INameScope, heap: HeapValues,
                                      function: (arg: Collection<Double>)->Number): LiteralValue {
    if(args.size!=1)
        throw SyntaxError("builtin function requires one non-scalar argument", position)
    val iterable = args[0].constValue(namespace, heap) ?: throw NotConstArgumentException()

    val result = if(iterable.arrayvalue != null) {
        val constants = iterable.arrayvalue.map { it.constValue(namespace, heap)?.asNumericValue }
        if(null in constants)
            throw NotConstArgumentException()
        function(constants.map { it!!.toDouble() }).toDouble()
    } else {
        when(iterable.type) {
            DataType.UBYTE, DataType.UWORD, DataType.FLOAT -> throw SyntaxError("function expects an iterable type", position)
            else -> {
                if(iterable.heapId==null)
                    throw FatalAstException("iterable value should be on the heap")
                val array = heap.get(iterable.heapId).array ?: throw SyntaxError("function expects an iterable type", position)
                function(array.map { it.toDouble() })
            }
        }
    }
    return numericLiteral(result, args[0].position)
}

private fun collectionArgOutputBoolean(args: List<IExpression>, position: Position,
                                       namespace:INameScope, heap: HeapValues,
                                       function: (arg: Collection<Double>)->Boolean): LiteralValue {
    if(args.size!=1)
        throw SyntaxError("builtin function requires one non-scalar argument", position)
    val iterable = args[0].constValue(namespace, heap) ?: throw NotConstArgumentException()

    val result = if(iterable.arrayvalue != null) {
        val constants = iterable.arrayvalue.map { it.constValue(namespace, heap)?.asNumericValue }
        if(null in constants)
            throw NotConstArgumentException()
        function(constants.map { it!!.toDouble() })
    } else {
        val array = heap.get(iterable.heapId!!).array ?: throw SyntaxError("function requires array argument", position)
        function(array.map { it.toDouble() })
    }
    return LiteralValue.fromBoolean(result, position)
}

private fun builtinFlt(args: List<IExpression>, position: Position, namespace:INameScope, heap: HeapValues): LiteralValue {
    // 1 numeric arg, convert to float
    if(args.size!=1)
        throw SyntaxError("flt requires one numeric argument", position)

    val constval = args[0].constValue(namespace, heap) ?: throw NotConstArgumentException()
    val number = constval.asNumericValue ?: throw SyntaxError("flt requires one numeric argument", position)
    return LiteralValue(DataType.FLOAT, floatvalue = number.toDouble(), position = position)
}

private fun builtinFintb(args: List<IExpression>, position: Position, namespace:INameScope, heap: HeapValues): LiteralValue {
    // 1 float arg, convert to byte
    if(args.size!=1)
        throw SyntaxError("fintb requires one floating point argument", position)

    val constval = args[0].constValue(namespace, heap) ?: throw NotConstArgumentException()
    if(constval.type!=DataType.FLOAT)
        throw SyntaxError("fintb requires one floating point argument", position)
    val integer: Short
    val flt = constval.floatvalue!!
    integer = when {
        flt <= -128 -> -128
        flt >= 127 -> 127
        else -> flt.toShort()
    }
    return LiteralValue(DataType.BYTE, bytevalue = integer, position = position)
}

private fun builtinFintw(args: List<IExpression>, position: Position, namespace:INameScope, heap: HeapValues): LiteralValue {
    // 1 float arg, convert to word
    if(args.size!=1)
        throw SyntaxError("fintw requires one floating point argument", position)

    val constval = args[0].constValue(namespace, heap) ?: throw NotConstArgumentException()
    if(constval.type!=DataType.FLOAT)
        throw SyntaxError("fintw requires one floating point argument", position)
    val integer: Int
    val flt = constval.floatvalue!!
    integer = when {
        flt <= -32768 -> -32768
        flt >= 32767 -> 32767
        else -> flt.toInt()
    }
    return LiteralValue(DataType.WORD, wordvalue = integer, position = position)
}

private fun builtinWrd(args: List<IExpression>, position: Position, namespace:INameScope, heap: HeapValues): LiteralValue {
    // 1 byte arg, convert to word
    if(args.size!=1)
        throw SyntaxError("wrd requires one numeric argument", position)

    val constval = args[0].constValue(namespace, heap) ?: throw NotConstArgumentException()
    if(constval.type!=DataType.UBYTE && constval.type!=DataType.BYTE && constval.type!=DataType.UWORD)
        throw SyntaxError("wrd requires one argument of type ubyte, byte or uword", position)
    return LiteralValue(DataType.WORD, wordvalue = constval.asIntegerValue, position = position)
}

private fun builtinUwrd(args: List<IExpression>, position: Position, namespace:INameScope, heap: HeapValues): LiteralValue {
    // 1 arg, convert to uword
    if(args.size!=1)
        throw SyntaxError("uwrd requires one numeric argument", position)

    val constval = args[0].constValue(namespace, heap) ?: throw NotConstArgumentException()
    if(constval.type!=DataType.BYTE && constval.type!=DataType.WORD && constval.type!=DataType.UWORD)
        throw SyntaxError("uwrd requires one argument of type byte, word or uword", position)
    return LiteralValue(DataType.UWORD, wordvalue = constval.asIntegerValue!! and 65535, position = position)
}

private fun builtinB2ub(args: List<IExpression>, position: Position, namespace:INameScope, heap: HeapValues): LiteralValue {
    // 1 byte arg, convert to ubyte
    if(args.size!=1)
        throw SyntaxError("b2ub requires one byte argument", position)

    val constval = args[0].constValue(namespace, heap) ?: throw NotConstArgumentException()
    if(constval.type!=DataType.BYTE && constval.type!=DataType.UBYTE)
        throw SyntaxError("b2ub requires one argument of type byte or ubyte", position)
    return LiteralValue(DataType.UBYTE, bytevalue=(constval.bytevalue!!.toInt() and 255).toShort(), position = position)
}

private fun builtinUb2b(args: List<IExpression>, position: Position, namespace:INameScope, heap: HeapValues): LiteralValue {
    // 1 ubyte arg, convert to byte
    if(args.size!=1)
        throw SyntaxError("ub2b requires one ubyte argument", position)

    val constval = args[0].constValue(namespace, heap) ?: throw NotConstArgumentException()
    if(constval.type!=DataType.UBYTE)
        throw SyntaxError("ub2b requires one argument of type ubyte", position)
    return LiteralValue(DataType.BYTE, bytevalue=(constval.bytevalue!!.toInt() and 255).toShort(), position = position)
}

private fun builtinAbs(args: List<IExpression>, position: Position, namespace:INameScope, heap: HeapValues): LiteralValue {
    // 1 arg, type = float or int, result type= same as argument type
    if(args.size!=1)
        throw SyntaxError("abs requires one numeric argument", position)

    val constval = args[0].constValue(namespace, heap) ?: throw NotConstArgumentException()
    val number = constval.asNumericValue
    return when (number) {
        is Int, is Byte, is Short -> numericLiteral(Math.abs(number.toInt()), args[0].position)
        is Double -> numericLiteral(Math.abs(number.toDouble()), args[0].position)
        else -> throw SyntaxError("abs requires one numeric argument", position)
    }
}

private fun builtinAvg(args: List<IExpression>, position: Position, namespace:INameScope, heap: HeapValues): LiteralValue {
    if(args.size!=1)
        throw SyntaxError("avg requires array argument", position)
    val iterable = args[0].constValue(namespace, heap) ?: throw NotConstArgumentException()

    val result = if(iterable.arrayvalue!=null) {
        val constants = iterable.arrayvalue.map { it.constValue(namespace, heap)?.asNumericValue }
        if (null in constants)
            throw NotConstArgumentException()
        (constants.map { it!!.toDouble() }).average()
    }
    else {
        val array = heap.get(iterable.heapId!!).array ?: throw SyntaxError("avg requires array argument", position)
        array.average()
    }
    return numericLiteral(result, args[0].position)
}

private fun builtinLen(args: List<IExpression>, position: Position, namespace:INameScope, heap: HeapValues): LiteralValue {
    if(args.size!=1)
        throw SyntaxError("len requires one argument", position)
    var argument = args[0].constValue(namespace, heap)
    if(argument==null) {
        if(args[0] !is IdentifierReference)
            throw SyntaxError("len over weird argument ${args[0]}", position)
        val target = (args[0] as IdentifierReference).targetStatement(namespace)
        val argValue = (target as? VarDecl)?.value
        argument = argValue?.constValue(namespace, heap)
                ?: throw NotConstArgumentException()
    }
    return when(argument.type) {
        DataType.ARRAY_UB, DataType.ARRAY_B, DataType.ARRAY_UW, DataType.ARRAY_W -> {
            val arraySize = argument.arrayvalue?.size ?: heap.get(argument.heapId!!).arraysize
            LiteralValue(DataType.UWORD, wordvalue=arraySize, position=args[0].position)
        }
        DataType.ARRAY_F -> {
            val arraySize = argument.arrayvalue?.size ?: heap.get(argument.heapId!!).arraysize
            LiteralValue(DataType.UWORD, wordvalue=arraySize, position=args[0].position)
        }
        DataType.STR, DataType.STR_P, DataType.STR_S, DataType.STR_PS -> {
            val str = argument.strvalue(heap)
            LiteralValue(DataType.UWORD, wordvalue=str.length, position=args[0].position)
        }
        DataType.UBYTE, DataType.BYTE,
        DataType.UWORD, DataType.WORD,
        DataType.FLOAT -> throw SyntaxError("len of weird argument ${args[0]}", position)
    }
}

private fun numericLiteral(value: Number, position: Position): LiteralValue {
    val floatNum=value.toDouble()
    val tweakedValue: Number =
            if(floatNum==Math.floor(floatNum) && (floatNum>=-32768 && floatNum<=65535))
                floatNum.toInt()  // we have an integer disguised as a float.
            else
                floatNum

    return when(tweakedValue) {
        is Int -> LiteralValue.optimalNumeric(value.toInt(), position)
        is Short -> LiteralValue.optimalNumeric(value.toInt(), position)
        is Byte -> LiteralValue(DataType.UBYTE, bytevalue = value.toShort(), position = position)
        is Double -> LiteralValue(DataType.FLOAT, floatvalue = value.toDouble(), position = position)
        is Float -> LiteralValue(DataType.FLOAT, floatvalue = value.toDouble(), position = position)
        else -> throw FatalAstException("invalid number type ${value::class}")
    }
}
