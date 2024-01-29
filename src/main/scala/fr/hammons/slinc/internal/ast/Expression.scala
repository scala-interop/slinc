package fr.hammons.slinc.internal.ast

import java.lang.invoke.MethodHandle
import fr.hammons.slinc.Struct
import fr.hammons.slinc.internal.IsValidStruct

// MethodHandle strlen = CLinker.getInstance().downcallHandle(
//   CLinker.systemLookup().lookup("strlen").get(),
//   MethodType.methodType(long.class, MemoryAddress.class),
//   FunctionDescriptor.of(CLinker.C_LONG, CLinker.C_POINTER)
// );

// try (var scope = ResourceScope.newConfinedScope()) {
//    var cString = CLinker.toCString("Hello", scope);
//    long len = (long)strlen.invokeExact(cString.address()); // 5
// }

//ast requirements: ToExpr/FromExpr defined for all
//FromExpr/ToExpr generate strings?
//codegen compatible with runtime multistage and compiletime macros

//StoreInPosition(1, LookupMethodHandle(
//  MethodDescription(Seq(ParameterDescription(PointerType)), ReturnDescription(LongDescription))))
//CallMethodHandle(1, Parameter(), scope)
sealed trait Expression[ReturnType]

class StackAlloc[T <: Struct](using IsValidStruct[T]) extends Expression[T]

case class Block[ReturnType](
    startingExpressions: List[Expression[?]],
    lastExpression: Expression[ReturnType]
) extends Expression[ReturnType]

// case class LookupMethodHandle(
//     c: MethodDescription,
//     symbolName: String,
//     libraryRequirement: Dependency
// ) extends Expression[MethodHandle]

// case class CallMethodHandle[ReturnType](
//     mh: MethodHandle,
//     md: MethodDescription,
//     params: Parameter[?]*
// ) extends Expression[ReturnType]
