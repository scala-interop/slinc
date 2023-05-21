package fr.hammons.slinc.modules

import java.lang.invoke.MethodHandle
import java.lang.foreign.{FunctionDescriptor as JFunctionDescriptor}
import scala.jdk.OptionConverters.*
import java.lang.foreign.MemorySegment
import fr.hammons.slinc.*
import java.lang.foreign.MemoryLayout
import java.lang.foreign.SymbolLookup

object LinkageModule19 extends LinkageModule:
  import descriptorModule19.*
  override type CSymbol = MemorySegment
  private val lookup = Slinc19.linker.defaultLookup().nn
  private val loaderLookup = SymbolLookup.loaderLookup().nn

  override def defaultLookup(name: String): Option[CSymbol] =
    lookup.lookup(name).nn.toScala

  override def loaderLookup(name: String): Option[CSymbol] =
    loaderLookup.lookup(name).nn.toScala

  override def getDowncall(
      descriptor: CFunctionDescriptor,
      varargs: Seq[Variadic]
  ): MethodHandle =
    val fdGen = (
        argDescriptors: Seq[MemoryLayout],
        varArgDescriptors: Seq[MemoryLayout]
    ) =>
      descriptor.returnDescriptor match
        case None =>
          JFunctionDescriptor
            .ofVoid(
              argDescriptors*
            )
            .nn
            .asVariadic(varArgDescriptors*)
        case Some(value) =>
          JFunctionDescriptor
            .of(
              toDowncallLayout(value),
              argDescriptors*
            )
            .nn
            .asVariadic(varArgDescriptors*)

    val fd = fdGen(
      descriptor.inputDescriptors
        .map(toDowncallLayout),
      varargs.view
        .map(_.use[DescriptorOf](dc ?=> _ => dc.descriptor))
        .map(toMemoryLayout)
        .toList
    )

    Slinc19.linker.downcallHandle(fd).nn
  end getDowncall

  lazy val tempScope: Scope = Scope19.createTempScope
